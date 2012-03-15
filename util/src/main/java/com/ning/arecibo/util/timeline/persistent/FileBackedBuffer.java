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

import com.fasterxml.util.membuf.MemBuffersForBytes;
import com.fasterxml.util.membuf.StreamyBytesMemBuffer;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.smile.SmileGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class FileBackedBuffer
{
    private static final Logger log = LoggerFactory.getLogger(FileBackedBuffer.class);

    private static final SmileFactory smileFactory = new SmileFactory();
    private static final ObjectMapper smileObjectMapper = new ObjectMapper(smileFactory);

    static {
        // Disable all magic for now as we don't write the Smile header (we share the same smileGenerator
        // across multiple backend files)
        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, false);
        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, false);
    }

    private final StreamyBytesPersistentOutputStream out;
    private final SmileGenerator smileGenerator;
    private final AtomicLong samplesforTimestampWritten = new AtomicLong();

    public FileBackedBuffer(final String basePath, final String prefix) throws IOException
    {
        // Create up to 7 segments of 70kB, which creates ~500kB files.
        // With -XX:MaxDirectMemorySize=1024m, this allows one to manage up to 2k hosts
        final MemBuffersForBytes bufs = new MemBuffersForBytes(70 * 1024, 1, 8);
        final StreamyBytesMemBuffer inputBuffer = bufs.createStreamyBuffer(4, 8);
        out = new StreamyBytesPersistentOutputStream(basePath, prefix, inputBuffer);
        smileGenerator = smileFactory.createJsonGenerator(out, JsonEncoding.UTF8);
        // Drop the Smile header
        smileGenerator.flush();
        out.reset();
    }

    public boolean append(final HostSamplesForTimestamp hostSamplesForTimestamp)
    {
        try {
            smileObjectMapper.writeValue(smileGenerator, hostSamplesForTimestamp);
            samplesforTimestampWritten.incrementAndGet();
            return true;
        }
        catch (IOException e) {
            log.warn("Unable to backup samples", e);
            return false;
        }
    }

    public void discard()
    {
        for (final String path : out.getCreatedFiles()) {
            log.info("Discarding file: {}", path);
            if (!new File(path).delete()) {
                log.warn("Unable to discard file: {}", path);
            }
        }

        samplesforTimestampWritten.set(0);
    }

    public long getFilesCreated()
    {
        return out.getCreatedFiles().size();
    }

    public long getSamplesForTimestampWritten()
    {
        return samplesforTimestampWritten.get();
    }
}
