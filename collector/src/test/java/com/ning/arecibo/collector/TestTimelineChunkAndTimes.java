package com.ning.arecibo.collector;

import com.ning.arecibo.util.timeline.SampleCoder;
import com.ning.arecibo.util.timeline.SampleOpcode;
import com.ning.arecibo.util.timeline.ScalarSample;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineTimes;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class TestTimelineChunkAndTimes
{
    private static final String HOST_NAME = "arecibo.ning.com";
    private static final int HOST_ID = 1242;
    private static final String SAMPLE_KIND = "JVM_Heap_Used";
    private static final int SAMPLE_KIND_ID = 12;
    private static final int TIMELINE_TIMES_ID = 11;

    @Test(groups = "fast")
    public void testToString() throws Exception
    {
        final int sampleCount = 3;

        final DateTime startTime = new DateTime("2012-01-16T21:23:58.316Z", DateTimeZone.UTC);
        final List<DateTime> times = new ArrayList<DateTime>();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DataOutputStream stream = new DataOutputStream(out);

        for (int i = 0; i < sampleCount; i++) {
            SampleCoder.encodeSample(stream, new ScalarSample<Long>(SampleOpcode.LONG, 12345L + i));
            times.add(startTime.plusSeconds(1 + i));
        }

        final TimelineTimes timelineTimes = new TimelineTimes(TIMELINE_TIMES_ID, HOST_ID, startTime, times.get(times.size() - 1), times);
        final TimelineChunk timelineChunk = new TimelineChunk(HOST_ID, SAMPLE_KIND_ID, TIMELINE_TIMES_ID, out.toByteArray(), sampleCount);

        final TimelineChunkAndTimes timelineChunkAndTimes = new TimelineChunkAndTimes(HOST_NAME, SAMPLE_KIND, timelineChunk, timelineTimes);

        Assert.assertEquals(timelineChunkAndTimes.toString(),
            "{\"sampleKind\":\"JVM_Heap_Used\",\"samples\":\"2012-01-16T21:23:59.316Z,12345,2012-01-16T21:24:00.316Z,12346,2012-01-16T21:24:01.316Z,12347\"}");
    }
}
