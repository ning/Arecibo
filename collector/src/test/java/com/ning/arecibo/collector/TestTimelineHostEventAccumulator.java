package com.ning.arecibo.collector;

import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.SampleOpcode;
import com.ning.arecibo.util.timeline.ScalarSample;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.TimelineHostEventAccumulator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.IDBI;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestTimelineHostEventAccumulator
{
    private static final int HOST_ID = 1;

    private static final MockTimelineDAO dao = new MockTimelineDAO();

    @Test(groups = "fast")
    public void testSimpleAggregate()
    {
        final TimelineHostEventAccumulator accumulator = new TimelineHostEventAccumulator(dao, HOST_ID);

        // Send a first type of data
        final DateTime startTime = new DateTime(DateTimeZone.UTC);
        final int sampleCount = 5;
        final int sampleKindId = 1;
        sendData(accumulator, startTime, sampleCount, sampleKindId);
        Assert.assertEquals(accumulator.getStartTime(), startTime);
        Assert.assertEquals(accumulator.getEndTime(), startTime.plusSeconds(sampleCount - 1));
        Assert.assertEquals(accumulator.getHostId(), HOST_ID);
        Assert.assertEquals(accumulator.getTimelines().size(), 1);
        Assert.assertEquals(accumulator.getTimelines().get(sampleKindId).getSampleCount(), sampleCount);
        Assert.assertEquals(accumulator.getTimelines().get(sampleKindId).getSampleKindId(), sampleKindId);

        // Send now a second type
        final DateTime secondStartTime = startTime.plusSeconds(sampleCount + 1);
        final int secondSampleCount = 15;
        final int secondSampleKindId = 2;
        sendData(accumulator, secondStartTime, secondSampleCount, secondSampleKindId);
        // We keep the start time of the accumulator
        Assert.assertEquals(accumulator.getStartTime(), startTime);
        Assert.assertEquals(accumulator.getEndTime(), secondStartTime.plusSeconds(secondSampleCount - 1));
        Assert.assertEquals(accumulator.getHostId(), HOST_ID);
        Assert.assertEquals(accumulator.getTimelines().size(), 2);
        // We advance all timelines in parallel
        Assert.assertEquals(accumulator.getTimelines().get(sampleKindId).getSampleCount(), sampleCount + secondSampleCount);
        Assert.assertEquals(accumulator.getTimelines().get(sampleKindId).getSampleKindId(), sampleKindId);
        Assert.assertEquals(accumulator.getTimelines().get(secondSampleKindId).getSampleCount(), sampleCount + secondSampleCount);
        Assert.assertEquals(accumulator.getTimelines().get(secondSampleKindId).getSampleKindId(), secondSampleKindId);
    }

    private void sendData(final TimelineHostEventAccumulator accumulator, final DateTime startTime, final int sampleCount, final int sampleKindId)
    {
        final Map<Integer, ScalarSample> samples = new HashMap<Integer, ScalarSample>();

        for (int i = 0; i < sampleCount; i++) {
            samples.put(sampleKindId, new ScalarSample<Long>(SampleOpcode.LONG, i + 1242L));
            final HostSamplesForTimestamp hostSamplesForTimestamp = new HostSamplesForTimestamp(HOST_ID, "JVM", startTime.plusSeconds(i), samples);
            accumulator.addHostSamples(hostSamplesForTimestamp);
        }
    }
}
