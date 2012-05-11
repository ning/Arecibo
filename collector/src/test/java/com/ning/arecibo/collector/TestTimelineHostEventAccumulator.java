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

import com.ning.arecibo.collector.persistent.TimelineHostEventAccumulator;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.samples.SampleOpcode;
import com.ning.arecibo.util.timeline.samples.ScalarSample;
import com.ning.arecibo.util.timeline.times.TimelineCoder;
import com.ning.arecibo.util.timeline.times.TimelineCoderImpl;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TestTimelineHostEventAccumulator
{
    private static final int HOST_ID = 1;
    private static final int EVENT_CATEGORY_ID = 123;

    private static final MockTimelineDAO dao = new MockTimelineDAO();
    private static final TimelineCoder timelineCoder = new TimelineCoderImpl();

    @Test(groups = "fast")
    public void testSimpleAggregate() throws IOException
    {
        final DateTime startTime = new DateTime(DateTimeZone.UTC);
        final TimelineHostEventAccumulator accumulator = new TimelineHostEventAccumulator(dao, timelineCoder, HOST_ID, EVENT_CATEGORY_ID, startTime);

        // Send a first type of data
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
