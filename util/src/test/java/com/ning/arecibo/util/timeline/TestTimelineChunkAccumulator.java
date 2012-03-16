package com.ning.arecibo.util.timeline;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestTimelineChunkAccumulator {

    @SuppressWarnings("unchecked")
    @Test(groups="fast")
    public void testBasicAccumulator() throws Exception {
        final int hostId = 123;
        final int sampleKindId = 456;
        final int timelineTimesId = 789;
        final TimelineChunkAccumulator accum = new TimelineChunkAccumulator(hostId, sampleKindId);

        accum.addSample(new ScalarSample(SampleOpcode.INT, new Integer(25)));
        for (int i=0; i<5; i++) {
            accum.addSample(new ScalarSample(SampleOpcode.INT, new Integer(10)));
        }
        accum.addSample(new ScalarSample(SampleOpcode.DOUBLE, new Double(100.0)));
        accum.addSample(new ScalarSample(SampleOpcode.DOUBLE, new Double(100.0)));

        accum.addSample(new ScalarSample(SampleOpcode.STRING, new String("Hiya!")));

        final TimelineChunk chunk = accum.extractTimelineChunkAndReset(timelineTimesId);
        Assert.assertEquals(chunk.getSampleCount(), 9);
        final TimelineTimes times = makeTimelineTimesBytes(TimelineTimes.unixSeconds(new DateTime()), 30, timelineTimesId, 9, hostId);
        Assert.assertEquals(times.getSampleCount(), 9);
        // Now play them back
        SampleCoder.scan(chunk.getSamples(), times, new SampleProcessor() {

            @Override
            public void processSamples(TimelineTimes timestamps, int sampleNumber, int sampleCount, SampleOpcode opcode, Object value) {
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
            }
        });
    }

    private TimelineTimes makeTimelineTimesBytes(final int initialUnixTime, final int secondsBetweenSamples, final int timelineTimesId, final int sampleCount, final int hostId) {
        final byte[] times = new byte[4 * sampleCount];
        final ByteBuffer byteBuffer = ByteBuffer.wrap(times);
        final IntBuffer intBuffer = byteBuffer.asIntBuffer();
        for (int i=0; i<sampleCount; i++) {
            intBuffer.put(initialUnixTime + i * secondsBetweenSamples);
        }
        return new TimelineTimes(timelineTimesId,
                                 hostId,
                                 TimelineTimes.dateTimeFromUnixSeconds(initialUnixTime),
                                 TimelineTimes.dateTimeFromUnixSeconds(initialUnixTime + secondsBetweenSamples * sampleCount),
                                 times,
                                 sampleCount);
    }
}
