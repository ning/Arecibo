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

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ning.arecibo.collector.MockTimelineDAO;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.collector.persistent.BackgroundDBChunkWriter;
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.timeline.persistent.FileBackedBuffer;
import com.ning.arecibo.util.timeline.persistent.TimelineDAO;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Map;
import java.util.UUID;

public class TestInMemoryCollectorEventProcessor
{
    private static final UUID HOST_UUID = UUID.randomUUID();
    private static final String EVENT_TYPE = "eventType";
    private static final String SAMPLE_KIND_A = "kindA";
    private static final String SAMPLE_KIND_B = "kindB";
    private static final Map<String, Object> EVENT = ImmutableMap.<String, Object>of(SAMPLE_KIND_A, 12, SAMPLE_KIND_B, 42);
    private static final int NB_EVENTS = 5;
    private static final File basePath = new File(System.getProperty("java.io.tmpdir"), "TestInMemoryCollectorEventProcessor-" + System.currentTimeMillis());

    private final TimelineDAO dao = new MockTimelineDAO();
    private CollectorEventProcessor processor;
    private TimelineEventHandler timelineEventHandler;
    private int eventTypeId = 0;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        Assert.assertTrue(basePath.mkdir());
        System.setProperty("arecibo.collector.timelines.spoolDir", basePath.getAbsolutePath());
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        timelineEventHandler = new TimelineEventHandler(config, dao, new BackgroundDBChunkWriter(dao, config, true), new FileBackedBuffer(config.getSpoolDir(), "TimelineEventHandler", 1024 * 1024, 10));
        processor = new CollectorEventProcessor(ImmutableList.<EventHandler>of(timelineEventHandler), Functions.<Event>identity());

        dao.getOrAddHost(HOST_UUID.toString());
        eventTypeId = dao.getOrAddEventCategory(EVENT_TYPE);
    }

    @Test(groups = "fast")
    public void testInMemoryFilters() throws Exception
    {
        final DateTime startTime = new DateTime(DateTimeZone.UTC);
        for (int i = 0; i < NB_EVENTS; i++) {
            final long eventTs = new DateTime(DateTimeZone.UTC).getMillis();
            processor.processEvent(new MapEvent(eventTs, EVENT_TYPE, HOST_UUID, EVENT));
        }
        final DateTime endTime = new DateTime(DateTimeZone.UTC);

        final Integer hostId = dao.getHostId(HOST_UUID.toString());
        Assert.assertNotNull(hostId);
        final Integer sampleKindAId = dao.getSampleKindId(eventTypeId, SAMPLE_KIND_A);
        Assert.assertNotNull(sampleKindAId);
        final Integer sampleKindBId = dao.getSampleKindId(eventTypeId, SAMPLE_KIND_B);
        Assert.assertNotNull(sampleKindBId);

        // One per host and type
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, null, null).size(), 2);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, startTime, null).size(), 2);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, null, endTime).size(), 2);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, startTime, endTime).size(), 2);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, sampleKindAId, startTime, endTime).size(), 1);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, sampleKindBId, startTime, endTime).size(), 1);
        // Wider ranges should be supported
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, sampleKindBId, startTime.minusSeconds(1), endTime).size(), 1);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, sampleKindBId, startTime, endTime.plusSeconds(1)).size(), 1);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, sampleKindBId, startTime.minusSeconds(1), endTime.plusSeconds(1)).size(), 1);
        // Buggy kind
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, Integer.MAX_VALUE, startTime, endTime).size(), 0);
        // Buggy start date
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, startTime.plusMinutes(1), endTime).size(), 0);
        // Buggy end date
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, startTime, endTime.minusMinutes(1)).size(), 0);
        // Buggy host
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(Integer.MAX_VALUE, startTime, endTime).size(), 0);
    }
}
