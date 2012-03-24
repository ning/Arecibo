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
    private static final String EVENT_CATEGORY = "JVM";
    private static final String SAMPLE_KIND = "JVM_Heap_Used";
    private static final int SAMPLE_KIND_ID = 12;
    private static final int SAMPLE_TIMELINE_ID = 30;
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

        final TimelineTimes timelineTimes = new TimelineTimes(TIMELINE_TIMES_ID, HOST_ID, EVENT_CATEGORY, startTime, times.get(times.size() - 1), times);
        final TimelineChunk timelineChunk = new TimelineChunk(SAMPLE_TIMELINE_ID, HOST_ID, SAMPLE_KIND_ID, TIMELINE_TIMES_ID, startTime, out.toByteArray(), sampleCount);

        final TimelineChunkAndTimes timelineChunkAndTimes = new TimelineChunkAndTimes(HOST_NAME, SAMPLE_KIND, timelineChunk, timelineTimes);
        Assert.assertEquals(timelineChunkAndTimes.toString(),
            "{\"sampleKind\":\"JVM_Heap_Used\",\"samples\":\"1326749039,12345,1326749040,12346,1326749041,12347\"}");

        // Test CSV filtering
        Assert.assertEquals(timelineChunkAndTimes.getSamplesAsCSV(), "1326749039,12345,1326749040,12346,1326749041,12347");
        Assert.assertEquals(timelineChunkAndTimes.getSamplesAsCSV(null, null), "1326749039,12345,1326749040,12346,1326749041,12347");
        Assert.assertEquals(timelineChunkAndTimes.getSamplesAsCSV(startTime, null), "1326749039,12345,1326749040,12346,1326749041,12347");
        Assert.assertEquals(timelineChunkAndTimes.getSamplesAsCSV(null, startTime.plusSeconds(sampleCount)), "1326749039,12345,1326749040,12346,1326749041,12347");
        Assert.assertEquals(timelineChunkAndTimes.getSamplesAsCSV(startTime.plusSeconds(1), startTime.plusSeconds(sampleCount)), "1326749039,12345,1326749040,12346,1326749041,12347");
        Assert.assertEquals(timelineChunkAndTimes.getSamplesAsCSV(startTime.plusSeconds(2), startTime.plusSeconds(sampleCount)), "1326749040,12346,1326749041,12347");
        Assert.assertEquals(timelineChunkAndTimes.getSamplesAsCSV(startTime.plusSeconds(3), startTime.plusSeconds(sampleCount)), "1326749041,12347");
        Assert.assertEquals(timelineChunkAndTimes.getSamplesAsCSV(startTime.plusSeconds(4), startTime.plusSeconds(sampleCount)), "");
        // Buggy start date
        Assert.assertEquals(timelineChunkAndTimes.getSamplesAsCSV(startTime.plusSeconds(10), startTime.plusSeconds(sampleCount)), "");
        // Buggy end date
        Assert.assertEquals(timelineChunkAndTimes.getSamplesAsCSV(startTime, startTime.minusSeconds(1)), "");
    }
}
