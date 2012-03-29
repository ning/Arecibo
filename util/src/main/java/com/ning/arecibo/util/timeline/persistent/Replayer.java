/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.util.timeline.persistent;

import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileParser;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Replayer
{
    private static final Logger log = LoggerFactory.getLogger(Replayer.class);
    private static final SmileFactory smileFactory = new SmileFactory();
    private static final ObjectMapper smileMapper = new ObjectMapper(smileFactory).registerModule(new JodaModule());

    static {
        smileFactory.configure(SmileParser.Feature.REQUIRE_HEADER, false);
        smileFactory.setCodec(smileMapper);
    }

    @VisibleForTesting
    static final Ordering<File> FILE_ORDERING = new Ordering<File>()
    {
        @Override
        public int compare(@Nullable final File left, @Nullable final File right)
        {
            if (left == null || right == null) {
                throw new NullPointerException();
            }

            // Order by the nano time
            return left.getAbsolutePath().compareTo(right.getAbsolutePath());
        }
    };

    private final String path;

    public Replayer(final String path)
    {
        this.path = path;
    }

    public List<HostSamplesForTimestamp> readAll()
    {
        final List<HostSamplesForTimestamp> samples = new ArrayList<HostSamplesForTimestamp>();

        readAll(new Function<HostSamplesForTimestamp, Void>()
        {
            @Override
            public Void apply(@Nullable final HostSamplesForTimestamp input)
            {
                if (input != null) {
                    samples.add(input);
                }
                return null;
            }
        });

        return samples;
    }

    public void readAll(final Function<HostSamplesForTimestamp, Void> fn)
    {
        final Collection<File> files = FileUtils.listFiles(new File(path), new String[]{"bin"}, false);

        for (final File file : FILE_ORDERING.sortedCopy(files)) {
            try {
                read(file, fn);

                if (!file.delete()) {
                    log.warn("Unable to delete file: {}", file.getAbsolutePath());
                }
            }
            catch (IOException e) {
                log.warn("Exception replaying file: {}", file.getAbsolutePath(), e);
            }
        }
    }

    @VisibleForTesting
    void read(final File file, final Function<HostSamplesForTimestamp, Void> fn) throws IOException
    {
        final JsonParser smileParser = smileFactory.createJsonParser(file);
        if (smileParser.nextToken() != JsonToken.START_ARRAY) {
            return;
        }

        while (smileParser.nextToken() != JsonToken.END_ARRAY) {
            final HostSamplesForTimestamp hostSamplesForTimestamp = smileParser.readValueAs(HostSamplesForTimestamp.class);
            fn.apply(hostSamplesForTimestamp);
        }

        smileParser.close();
    }
}