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

package com.ning.arecibo.util.timeline;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.joda.time.DateTime;

import com.ning.arecibo.util.Logger;

/**
 * This class represents a sequence of values for a single attribute,
 * e.g., "TP99 Response Time", for one host and one specific time range,
 * as the object is being accumulated.  It is not used to represent
 * past timeline sequences; they are held in TimelineChunk objects.
 * <p/>
 * It accumulates samples in a byte array object. Readers can call
 * getEncodedSamples() at any time to get the latest data.
 */
public class TimelineChunkAccumulator extends SampleCoder
{
    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();
    private final int hostId;
    private final int sampleKindId;

    public TimelineChunkAccumulator(final int hostId, final int sampleKindId)
    {
        super();
        this.hostId = hostId;
        this.sampleKindId = sampleKindId;
    }

    private TimelineChunkAccumulator(final int hostId, final int sampleKindId, final byte[] bytes, final SampleBase lastSample, final int sampleCount) throws IOException
    {
        super(bytes, lastSample, sampleCount);
        this.hostId = hostId;
        this.sampleKindId = sampleKindId;
    }

    public TimelineChunkAccumulator deepCopy() throws IOException
    {
        return new TimelineChunkAccumulator(hostId, sampleKindId, getByteStream().toByteArray(), getLastSample(), getSampleCount());
    }

    /**
     * This method grabs the current encoded form, and resets the accumulator
     */
    public synchronized TimelineChunk extractTimelineChunkAndReset(final DateTime startTime, final DateTime endTime, final byte[] timeBytes)
    {
        // Extract the chunk
        final byte[] sampleBytes = getEncodedSamples().getEncodedBytes();
        log.debug("Creating TimelineChunk for sampleKindId %d, sampleCount %d", sampleKindId, getSampleCount());
        final TimelineChunk chunk = new TimelineChunk(0, hostId, sampleKindId, startTime, endTime, timeBytes, sampleBytes, getSampleCount());

        // Reset this current accumulator
        reset();

        return chunk;
    }

    public int getHostId()
    {
        return hostId;
    }

    public int getSampleKindId()
    {
        return sampleKindId;
    }
}
