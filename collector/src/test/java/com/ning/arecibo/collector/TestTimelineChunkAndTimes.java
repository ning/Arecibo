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
import com.ning.arecibo.util.timeline.TimelineCoder;

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
    private static final int HOST_ID = 1242;
    private static final int SAMPLE_KIND_ID = 12;
    private static final int SAMPLE_TIMELINE_ID = 30;

    @Test(groups = "fast")
    public void testToString() throws Exception
    {
        final int sampleCount = 3;

        final DateTime startTime = new DateTime("2012-01-16T21:23:58.316Z", DateTimeZone.UTC);
        final List<DateTime> dateTimes = new ArrayList<DateTime>();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DataOutputStream stream = new DataOutputStream(out);

        for (int i = 0; i < sampleCount; i++) {
            SampleCoder.encodeSample(stream, new ScalarSample<Long>(SampleOpcode.LONG, 12345L + i));
            dateTimes.add(startTime.plusSeconds(1 + i));
        }

        final DateTime endTime = dateTimes.get(dateTimes.size() - 1);
        final byte[] times = TimelineCoder.compressDateTimes(dateTimes);
        final TimelineChunk timelineChunk = new TimelineChunk(SAMPLE_TIMELINE_ID, HOST_ID, SAMPLE_KIND_ID, startTime, endTime, times, out.toByteArray(), sampleCount);
        // Test CSV filtering
        Assert.assertEquals(timelineChunk.getSamplesAsCSV(), "1326749039,12345,1326749040,12346,1326749041,12347");
        Assert.assertEquals(timelineChunk.getSamplesAsCSV(null, null), "1326749039,12345,1326749040,12346,1326749041,12347");
        Assert.assertEquals(timelineChunk.getSamplesAsCSV(startTime, null), "1326749039,12345,1326749040,12346,1326749041,12347");
        Assert.assertEquals(timelineChunk.getSamplesAsCSV(null, startTime.plusSeconds(sampleCount)), "1326749039,12345,1326749040,12346,1326749041,12347");
        Assert.assertEquals(timelineChunk.getSamplesAsCSV(startTime.plusSeconds(1), startTime.plusSeconds(sampleCount)), "1326749039,12345,1326749040,12346,1326749041,12347");
        Assert.assertEquals(timelineChunk.getSamplesAsCSV(startTime.plusSeconds(2), startTime.plusSeconds(sampleCount)), "1326749040,12346,1326749041,12347");
        Assert.assertEquals(timelineChunk.getSamplesAsCSV(startTime.plusSeconds(3), startTime.plusSeconds(sampleCount)), "1326749041,12347");
        Assert.assertEquals(timelineChunk.getSamplesAsCSV(startTime.plusSeconds(4), startTime.plusSeconds(sampleCount)), "");
        // Buggy start date
        Assert.assertEquals(timelineChunk.getSamplesAsCSV(startTime.plusSeconds(10), startTime.plusSeconds(sampleCount)), "");
        // Buggy end date
        Assert.assertEquals(timelineChunk.getSamplesAsCSV(startTime, startTime.minusSeconds(1)), "");
    }
}
