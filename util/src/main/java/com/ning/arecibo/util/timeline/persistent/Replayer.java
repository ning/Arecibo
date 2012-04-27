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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.smile.SmileParser;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class Replayer
{
    private static final Logger log = LoggerFactory.getLogger(Replayer.class);
    private static final SmileFactory smileFactory = new SmileFactory();
    private static final ObjectMapper smileMapper = new ObjectMapper(smileFactory);

    static {
        smileFactory.configure(SmileParser.Feature.REQUIRE_HEADER, false);
        smileFactory.setCodec(smileMapper);
    }

    @VisibleForTesting
    public static final Ordering<File> FILE_ORDERING = new Ordering<File>()
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

    // This method is only used by test code
    public List<HostSamplesForTimestamp> readAll()
    {
        final List<HostSamplesForTimestamp> samples = new ArrayList<HostSamplesForTimestamp>();

        readAll(true, null, new Function<HostSamplesForTimestamp, Void>()
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

    public int readAll(final boolean deleteFiles, final @Nullable DateTime minStartTime, final Function<HostSamplesForTimestamp, Void> fn)
    {
        final Collection<File> files = FileUtils.listFiles(new File(path), new String[]{"bin"}, false);
        int filesSkipped = 0;
        for (final File file : FILE_ORDERING.sortedCopy(files)) {
            try {
                // Skip files whose last modification date is is earlier than the first start time.
                if (minStartTime != null && file.lastModified() < minStartTime.getMillis()) {
                    filesSkipped++;
                    continue;
                }
                read(file, fn);

                if (deleteFiles) {
                    if (!file.delete()) {
                        log.warn("Unable to delete file: {}", file.getAbsolutePath());
                    }
                }
            }
            catch (IOException e) {
                log.warn("Exception replaying file: {}", file.getAbsolutePath(), e);
            }
        }
        return filesSkipped;
    }

    @VisibleForTesting
    public void read(final File file, final Function<HostSamplesForTimestamp, Void> fn) throws IOException
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


    public void purgeOldFiles(final DateTime purgeIfOlderDate)
    {
        final Collection<File> files = FileUtils.listFiles(new File(path), new String[]{"bin"}, false);

        for (final File file : files) {
            if (FileUtils.isFileOlder(file, new Date(purgeIfOlderDate.getMillis()))) {

                if (!file.delete()) {
                    log.warn("Unable to delete file: {}", file.getAbsolutePath());
                }
            }
        }
    }
}
