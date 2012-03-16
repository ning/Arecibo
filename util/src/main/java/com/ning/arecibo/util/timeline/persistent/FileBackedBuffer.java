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
import com.google.common.annotations.VisibleForTesting;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.smile.SmileGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Backing buffer for a single TimelineHostEventAccumulator that spools to disk
 */
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

    private final String basePath;
    private final String prefix;
    private final AtomicLong samplesforTimestampWritten = new AtomicLong();
    private final Object recyclingMonitor = new Object();

    private final StreamyBytesMemBuffer inputBuffer;
    private StreamyBytesPersistentOutputStream out = null;
    private SmileGenerator smileGenerator;

    public FileBackedBuffer(final String basePath, final String prefix) throws IOException
    {
        this.basePath = basePath;
        this.prefix = prefix;

        // This configuration creates ~60M files, this to stay below 64M default limit
        // that JVM has for direct buffers. You can bump this via -XX:MaxDirectMemorySize
        final MemBuffersForBytes bufs = new MemBuffersForBytes(4 * 1024 * 1024, 1, 15);
        inputBuffer = bufs.createStreamyBuffer(8, 15);

        recycle();
    }

    public boolean append(final HostSamplesForTimestamp hostSamplesForTimestamp)
    {
        try {
            synchronized (recyclingMonitor) {
                smileObjectMapper.writeValue(smileGenerator, hostSamplesForTimestamp);
                samplesforTimestampWritten.incrementAndGet();
                return true;
            }
        }
        catch (IOException e) {
            log.warn("Unable to backup samples", e);
            return false;
        }
    }

    /**
     * Discard in-memory and on-disk data
     */
    public void discard()
    {
        try {
            recycle();
        }
        catch (IOException e) {
            log.warn("Exception discarding buffer", e);
        }
    }

    private void recycle() throws IOException
    {
        synchronized (recyclingMonitor) {
            if (out != null && !out.isEmpty()) {
                out.close();
            }

            out = new StreamyBytesPersistentOutputStream(basePath, prefix, inputBuffer);
            smileGenerator = smileFactory.createJsonGenerator(out, JsonEncoding.UTF8);
            // Drop the Smile header
            smileGenerator.flush();
            out.reset();

            samplesforTimestampWritten.set(0);
        }
    }

    @MonitorableManaged(description = "Return the approximate size of bytes on disk for samples not yet in the database", monitored = true, monitoringType = {MonitoringType.VALUE})
    public long getBytesOnDisk()
    {
        return out.getBytesOnDisk();
    }

    @MonitorableManaged(description = "Return the approximate size of bytes in memory for samples not yet in the database", monitored = true, monitoringType = {MonitoringType.VALUE})
    public long getBytesInMemory()
    {
        return out.getBytesInMemory();
    }

    @MonitorableManaged(description = "Return the approximate size of bytes available in memory (before spilling over to disk) for sampels not yet in the database", monitored = true, monitoringType = {MonitoringType.VALUE})
    public long getInMemoryAvailableSpace()
    {
        return out.getInMemoryAvailableSpace();
    }

    @VisibleForTesting
    public long getFilesCreated()
    {
        return out.getCreatedFiles().size();
    }
}
