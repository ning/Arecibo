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

package com.ning.arecibo.util.timeline.chunks;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ning.arecibo.util.timeline.samples.SampleCoderImpl;
import com.ning.arecibo.util.timeline.samples.SampleCoder;
import com.ning.arecibo.util.timeline.samples.SampleOpcode;
import com.ning.arecibo.util.timeline.samples.SampleProcessor;
import com.ning.arecibo.util.timeline.samples.ScalarSample;
import com.ning.arecibo.util.timeline.times.TimelineCursor;
import com.ning.arecibo.util.timeline.times.TimelineCoder;
import com.ning.arecibo.util.timeline.times.TimelineCoderImpl;

public class TestTimelineChunkAccumulator
{
    private static final TimelineCoder timelineCoder = new TimelineCoderImpl();
    private static final SampleCoder sampleCoder = new SampleCoderImpl();

    @SuppressWarnings("unchecked")
    @Test(groups="fast")
    public void testBasicAccumulator() throws Exception {
        final int hostId = 123;
        final int sampleKindId = 456;
        final TimelineChunkAccumulator accum = new TimelineChunkAccumulator(hostId, sampleKindId, sampleCoder);
        final List<DateTime> dateTimes = new ArrayList<DateTime>();
        final DateTime startTime = new DateTime();
        final DateTime endTime = startTime.plus(1000);

        accum.addSample(new ScalarSample(SampleOpcode.INT, new Integer(25)));
        int timesCounter = 0;
        dateTimes.add(startTime.plusSeconds(30 * timesCounter++));
        for (int i=0; i<5; i++) {
            accum.addSample(new ScalarSample(SampleOpcode.INT, new Integer(10)));
            dateTimes.add(startTime.plusSeconds(30 * timesCounter++));
        }
        accum.addSample(new ScalarSample(SampleOpcode.DOUBLE, new Double(100.0)));
        dateTimes.add(startTime.plusSeconds(30 * timesCounter++));
        accum.addSample(new ScalarSample(SampleOpcode.DOUBLE, new Double(100.0)));
        dateTimes.add(startTime.plusSeconds(30 * timesCounter++));

        accum.addSample(new ScalarSample(SampleOpcode.STRING, new String("Hiya!")));
        dateTimes.add(startTime.plusSeconds(30 * timesCounter++));

        final byte[] compressedTimes = timelineCoder.compressDateTimes(dateTimes);
        final TimelineChunk chunk = accum.extractTimelineChunkAndReset(startTime, endTime, compressedTimes);
        Assert.assertEquals(chunk.getSampleCount(), 9);
        // Now play them back
        sampleCoder.scan(chunk.getSamples(), compressedTimes, dateTimes.size(), new SampleProcessor() {
            private int sampleNumber = 0;

            @Override
            public void processSamples(TimelineCursor timeCursor, int sampleCount, SampleOpcode opcode, Object value) {
                if (sampleNumber == 0) {
                    Assert.assertEquals(opcode, SampleOpcode.INT);
                    Assert.assertEquals(value, new Integer(25));
                }
                else if (sampleNumber >= 1 && sampleNumber < 6) {
                    Assert.assertEquals(opcode, SampleOpcode.INT);
                    Assert.assertEquals(value, new Integer(10));
                }
                else if (sampleNumber >= 6 && sampleNumber < 8) {
                    Assert.assertEquals(opcode, SampleOpcode.DOUBLE);
                    Assert.assertEquals(value, new Double(100.0));
                }
                else if (sampleNumber == 8) {
                    Assert.assertEquals(opcode, SampleOpcode.STRING);
                    Assert.assertEquals(value, new String("Hiya!"));
                }
                else {
                    Assert.assertTrue(false);
                }
                sampleNumber += sampleCount;
            }
        });
        final TimelineChunkDecoded chunkDecoded = new TimelineChunkDecoded(chunk, sampleCoder);
        System.out.printf("%s\n", chunkDecoded.toString());
    }


    @Test(groups = "fast")
    public void testByteRepeater() throws Exception
    {
        final int hostId = 123;
        final int sampleKindId = 456;
        final DateTime startTime = new DateTime();
        final List<DateTime> dateTimes = new ArrayList<DateTime>();
        final int byteRepeaterCount = 255;
        final TimelineChunkAccumulator accum = new TimelineChunkAccumulator(hostId, sampleKindId, sampleCoder);
        for (int i=0; i<byteRepeaterCount; i++) {
            dateTimes.add(startTime.plusSeconds(i * 5));
            accum.addSample(sampleCoder.compressSample(new ScalarSample<Double>(SampleOpcode.DOUBLE, 2.0)));
        }
        final DateTime endTime = startTime.plusSeconds(5 * byteRepeaterCount);
        byte[] compressedTimes = timelineCoder.compressDateTimes(dateTimes);
        final TimelineChunk chunk = accum.extractTimelineChunkAndReset(startTime, endTime, compressedTimes);
        final byte[] samples = chunk.getSamples();
        // Should be 0xFF 0xFF 0x12 0x02
        Assert.assertEquals(samples.length, 4);
        Assert.assertEquals(((int)samples[0]) & 0xff, SampleOpcode.REPEAT_BYTE.getOpcodeIndex());
        Assert.assertEquals(((int)samples[1]) & 0xff, byteRepeaterCount);
        Assert.assertEquals(((int)samples[2]) & 0xff, SampleOpcode.BYTE_FOR_DOUBLE.getOpcodeIndex());
        Assert.assertEquals(((int)samples[3]) & 0xff, 0x02);
        Assert.assertEquals(chunk.getSampleCount(), byteRepeaterCount);
        sampleCoder.scan(chunk.getSamples(), compressedTimes, dateTimes.size(), new SampleProcessor() {

            @Override
            public void processSamples(TimelineCursor timeCursor, int sampleCount, SampleOpcode opcode, Object value) {
                Assert.assertEquals(sampleCount, byteRepeaterCount);
                Assert.assertEquals(value, 2.0);
            }
        });
    }

    @Test(groups = "fast")
    public void testShortRepeater() throws Exception
    {
        final int hostId = 123;
        final int sampleKindId = 456;
        final DateTime startTime = new DateTime();
        final List<DateTime> dateTimes = new ArrayList<DateTime>();
        final int shortRepeaterCount = 256;
        final TimelineChunkAccumulator accum = new TimelineChunkAccumulator(hostId, sampleKindId, sampleCoder);
        for (int i=0; i<shortRepeaterCount; i++) {
            dateTimes.add(startTime.plusSeconds(i * 5));
            accum.addSample(sampleCoder.compressSample(new ScalarSample<Double>(SampleOpcode.DOUBLE, 2.0)));
        }
        final DateTime endTime = startTime.plusSeconds(5 * shortRepeaterCount);
        final byte[] compressedTimes = timelineCoder.compressDateTimes(dateTimes);
        final TimelineChunk chunk = accum.extractTimelineChunkAndReset(startTime, endTime, compressedTimes);
        final byte[] samples = chunk.getSamples();
        Assert.assertEquals(samples.length, 5);
        Assert.assertEquals(((int)samples[0]) & 0xff, SampleOpcode.REPEAT_SHORT.getOpcodeIndex());
        final int count = ((samples[1] & 0xff) << 8) | (samples[2] & 0xff);
        Assert.assertEquals(count, shortRepeaterCount);
        Assert.assertEquals(((int)samples[3]) & 0xff, SampleOpcode.BYTE_FOR_DOUBLE.getOpcodeIndex());
        Assert.assertEquals(((int)samples[4]) & 0xff, 0x02);
        Assert.assertEquals(chunk.getSampleCount(), shortRepeaterCount);

        sampleCoder.scan(chunk.getSamples(), compressedTimes, dateTimes.size(), new SampleProcessor() {

            @Override
            public void processSamples(TimelineCursor timeCursor, int sampleCount, SampleOpcode opcode, Object value) {
                Assert.assertEquals(sampleCount, shortRepeaterCount);
                Assert.assertEquals(value, 2.0);
            }
        });
    }
}
