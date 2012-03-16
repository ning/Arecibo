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
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.persistent.FileBackedBuffer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.UUID;

public class TestInMemoryCollectorEventProcessor
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
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        timelineEventHandler = new TimelineEventHandler(config, dao, new FileBackedBuffer(config.getSpoolDir(), "TimelineEventHandler"));
        processor = new CollectorEventProcessor(ImmutableList.<EventHandler>of(timelineEventHandler));
    }

    @Test(groups = "fast")
    public void testInMemoryFilters() throws Exception
    {
        final DateTime startTime = new DateTime(DateTimeZone.UTC);
        for (int i = 0; i < NB_EVENTS; i++) {
            final long eventTs = new DateTime(DateTimeZone.UTC).getMillis();
            processor.processEvent(new MapEvent(eventTs, "NOT_USED", HOST_UUID, EVENT));
        }
        final DateTime endTime = new DateTime(DateTimeZone.UTC);

        // One per host and type
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), null, null).size(), 2);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), startTime, null).size(), 2);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), null, endTime).size(), 2);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), startTime, endTime).size(), 2);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_A, startTime, endTime).size(), 1);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_B, startTime, endTime).size(), 1);
        // Wider ranges should be supported
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_B, startTime.minusSeconds(1), endTime).size(), 1);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_B, startTime, endTime.plusSeconds(1)).size(), 1);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), KIND_B, startTime.minusSeconds(1), endTime.plusSeconds(1)).size(), 1);
        // Buggy kind
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), "kindC", startTime, endTime).size(), 0);
        // Buggy start date
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), startTime.plusMinutes(1), endTime).size(), 0);
        // Buggy end date
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(HOST_UUID.toString(), startTime, endTime.minusMinutes(1)).size(), 0);
        // Buggy host
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunkAndTimes(UUID.randomUUID().toString(), startTime, endTime).size(), 0);
    }
}
