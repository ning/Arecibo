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

package com.ning.arecibo.collector.process;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ning.arecibo.collector.MockTimelineDAO;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineDAO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class TestCollectorEventProcessor
{
    private static final UUID HOST_UUID = UUID.randomUUID();
    private static final String KIND_A = "kindA";
    private static final String KIND_B = "kindB";
    private static final Map<String, Object> EVENT = ImmutableMap.<String, Object>of(KIND_A, 12, KIND_B, 42);
    private static final int NB_EVENTS = 5;

    private final TimelineDAO dao = new MockTimelineDAO();
    private CollectorEventProcessor processor;
    private TimelineEventHandler timelineEventHandler;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        System.setProperty("arecibo.collector.timelines.length", "2s");
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        timelineEventHandler = new TimelineEventHandler(config, dao);
        processor = new CollectorEventProcessor(ImmutableList.<EventHandler>of(timelineEventHandler));
    }

    @Test(groups = "slow")
    public void testCache() throws Exception
    {
        // Check initial state
        Assert.assertEquals(processor.getEventsReceived(), 0);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelines(), 0);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_A, null, null).size(), 0);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_B, null, null).size(), 0);
        Assert.assertEquals(dao.getHosts().size(), 0);
        Assert.assertEquals(dao.getSampleKinds().size(), 0);

        String csvSamplesKindA = "";
        String csvSamplesKindB = "";
        for (int i = 0; i < NB_EVENTS; i++) {
            final long eventTs = new DateTime(DateTimeZone.UTC).getMillis();
            processor.processEvent(new MapEvent(eventTs, "NOT_USED", HOST_UUID, EVENT));

            // Build expected CSV output
            if (i > 0) {
                csvSamplesKindA += ",";
                csvSamplesKindB += ",";
            }
            csvSamplesKindA = String.format("%s%d,%d", csvSamplesKindA, eventTs / 1000, 12);
            csvSamplesKindB = String.format("%s%d,%d", csvSamplesKindB, eventTs / 1000, 42);

            checkProcessorState(csvSamplesKindA, csvSamplesKindB, i + 1);
        }

        // Check the state before the flush to the db
        Assert.assertEquals(processor.getEventsReceived(), NB_EVENTS);
        // One per host
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelines(), 1);
        // One per host and type
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_A, null, null).size(), 1);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_B, null, null).size(), 1);
        Assert.assertEquals(dao.getHosts().size(), 1);
        Assert.assertEquals(dao.getSampleKinds().size(), 2);
        Assert.assertTrue(dao.getSampleKinds().values().contains(KIND_A));
        Assert.assertTrue(dao.getSampleKinds().values().contains(KIND_B));

        Thread.sleep(2 * 1000 + 100);

        // Check the state after the flush to the db
        Assert.assertEquals(processor.getEventsReceived(), NB_EVENTS);
        // Should have been flushed
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelines(), 0);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_A, null, null).size(), 0);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_B, null, null).size(), 0);
    }

    private void checkProcessorState(final String csvSamplesKindA, final String csvSamplesKindB, final int eventSent) throws IOException, ExecutionException
    {
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelines(), 1);
        Assert.assertEquals(processor.getEventsReceived(), eventSent);

        // One per host and per type (two types here: kindA and kindB)
        final Collection<? extends TimelineChunkAndTimes> inMemoryTimelineChunkAndTimesA = timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_A, null, null);
        Assert.assertEquals(inMemoryTimelineChunkAndTimesA.size(), 1);
        Assert.assertEquals(inMemoryTimelineChunkAndTimesA.iterator().next().getSamplesAsCSV(), csvSamplesKindA);

        final Collection<? extends TimelineChunkAndTimes> inMemoryTimelineChunkAndTimesB = timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_B, null, null);
        Assert.assertEquals(inMemoryTimelineChunkAndTimesB.size(), 1);
        Assert.assertEquals(inMemoryTimelineChunkAndTimesB.iterator().next().getSamplesAsCSV(), csvSamplesKindB);
    }
}
