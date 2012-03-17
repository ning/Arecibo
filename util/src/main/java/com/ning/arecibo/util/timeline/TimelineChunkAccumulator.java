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

import com.ning.arecibo.util.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This class represents a sequence of values for a single attribute,
 * e.g., "TP99 Response Time", for one host and one specific time range,
 * as the object is being accumulated.  It is not used to represent
 * past timeline sequences; they are held in TimelineChunk objects.
 * <p/>
 * It accumulates samples in a byte array object. Readers can call
 * getEncodedSamples() at any time to get the latest data.
 */
public class TimelineChunkAccumulator
{
    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();
    private static final int DEFAULT_CHUNK_BYTE_ARRAY_SIZE = 300;
    private final int hostId;
    private final int sampleKindId;
    private ByteArrayOutputStream byteStream;
    /**
     * Any operation that writes to the outputStream must synchronize on it!
     */
    private DataOutputStream outputStream;

    private int sampleCount;
    private SampleBase lastSample;

    public TimelineChunkAccumulator(final int hostId, final int sampleKindId)
    {
        this.hostId = hostId;
        this.sampleKindId = sampleKindId;
        reset();
    }

    private TimelineChunkAccumulator(final int hostId, final int sampleKindId, final ByteArrayOutputStream stream, final SampleBase lastSample, final int sampleCount) throws IOException
    {
        this.hostId = hostId;
        this.sampleKindId = sampleKindId;
        this.byteStream = stream;
        byteStream.write(byteStream.toByteArray());
        outputStream = new DataOutputStream(byteStream);
        this.lastSample = lastSample;
        this.sampleCount = sampleCount;
    }

    public TimelineChunkAccumulator deepCopy() throws IOException
    {
        return new TimelineChunkAccumulator(hostId, sampleKindId, byteStream, lastSample, sampleCount);
    }

    public synchronized void addPlaceholder(final byte repeatCount)
    {
        if (repeatCount > 0) {
            addLastSample();
            lastSample = new RepeatSample<Void>(repeatCount, new NullSample());
            sampleCount += repeatCount;
        }
    }

    /**
     * This method grabs the current encoded form, and resets the accumulator
     */
    public synchronized TimelineChunk extractTimelineChunkAndReset(final int timelineTimesId)
    {
        // Extract the chunk
        final byte[] bytes = getEncodedSamples().getEncodedBytes();
        final TimelineChunk chunk = new TimelineChunk(0, hostId, sampleKindId, timelineTimesId, bytes, sampleCount);

        // Reset this current accumulator
        reset();

        return chunk;
    }

    @SuppressWarnings("unchecked")
    public synchronized void addSample(final ScalarSample sample)
    {
        if (lastSample == null) {
            lastSample = sample;
        }
        else {
            final SampleOpcode lastOpcode = lastSample.getOpcode();
            final SampleOpcode sampleOpcode = sample.getOpcode();
            if (lastOpcode == SampleOpcode.REPEAT) {
                final RepeatSample repeatSample = (RepeatSample)lastSample;
                final ScalarSample sampleRepeated = repeatSample.getSampleRepeated();
                if (sampleRepeated.getOpcode() == sampleOpcode &&
                    (sampleOpcode.getNoArgs() || sampleRepeated.getSampleValue().equals(sample.getSampleValue())) &&
                    repeatSample.getRepeatCount() < RepeatSample.MAX_REPEAT_COUNT) {
                    // We can just increment the count in the repeat instance
                    repeatSample.incrementRepeatCount();
                }
                else {
                    // A non-matching repeat - - just add it
                    addLastSample();
                    lastSample = sample;
                }
            }
            else {
                final ScalarSample lastScalarSample = (ScalarSample)lastSample;
                if (sampleOpcode == lastOpcode &&
                    (sampleOpcode.getNoArgs() || sample.getSampleValue().equals(lastScalarSample.getSampleValue()))) {
                    // Replace lastSample with repeat group
                    lastSample = new RepeatSample((byte) 2, lastScalarSample);
                }
                else {
                    addLastSample();
                    lastSample = sample;
                }
            }
        }
        // In all cases, we got 1 more sample
        sampleCount++;
    }

    public int getHostId()
    {
        return hostId;
    }

    public int getSampleKindId()
    {
        return sampleKindId;
    }

    public synchronized int getSampleCount()
    {
        return sampleCount;
    }

    private void reset()
    {
        byteStream = new ByteArrayOutputStream(DEFAULT_CHUNK_BYTE_ARRAY_SIZE);
        outputStream = new DataOutputStream(byteStream);
        lastSample = null;
        sampleCount = 0;
    }

    /**
     * The log scanner can safely call this method, and know that the byte
     * array will always end in a complete sample
     *
     * @return an instance containing the bytes and the counts of samples
     */
    private synchronized EncodedBytesAndSampleCount getEncodedSamples()
    {
        if (lastSample != null) {
            SampleCoder.encodeSample(outputStream, lastSample);
        }
        try {
            outputStream.flush();
            return new EncodedBytesAndSampleCount(byteStream.toByteArray(), sampleCount);
        }
        catch (IOException e) {
            log.error(e, "In getEncodedSamples, IOException flushing outputStream");
            // Do no harm - - this at least won't corrupt the encoding
            return new EncodedBytesAndSampleCount(new byte[0], 0);
        }
    }

    private synchronized void addLastSample()
    {
        if (lastSample != null) {
            SampleCoder.encodeSample(outputStream, lastSample);
            lastSample = null;
        }
    }
}
