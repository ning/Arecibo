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
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ning.arecibo.collector.MockTimelineDAO;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.collector.persistent.BackgroundDBChunkWriter;
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.timeline.CategoryIdAndSampleKind;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.persistent.FileBackedBuffer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class TestCollectorEventProcessor
{
    private static final UUID HOST_UUID = UUID.randomUUID();
    private static final String EVENT_TYPE = "eventType";
    private static final String SAMPLE_KIND_A = "kindA";
    private static final String SAMPLE_KIND_B = "kindB";
    private static final Map<String, Object> EVENT = ImmutableMap.<String, Object>of(SAMPLE_KIND_A, 12, SAMPLE_KIND_B, 42);
    private static final int NB_EVENTS = 5;
    private static final File basePath = new File(System.getProperty("java.io.tmpdir"), "TestCollectorEventProcessor-" + System.currentTimeMillis());

    private final TimelineDAO dao = new MockTimelineDAO();
    private BackgroundDBChunkWriter backgroundWriter;
    private CollectorEventProcessor processor;
    private TimelineEventHandler timelineEventHandler;
    private int eventCategoryId = 0;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        System.setProperty("arecibo.collector.timelines.length", "2s");
        Assert.assertTrue(basePath.mkdir());
        System.setProperty("arecibo.collector.timelines.spoolDir", basePath.getAbsolutePath());
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        backgroundWriter = new BackgroundDBChunkWriter(dao, config, config.getPerformForegroundWrites());
        timelineEventHandler = new TimelineEventHandler(config, dao, backgroundWriter, new FileBackedBuffer(config.getSpoolDir(), "TimelineEventHandler", 1024 * 1024, 10));
        processor = new CollectorEventProcessor(ImmutableList.<EventHandler>of(timelineEventHandler), Functions.<Event>identity());
        eventCategoryId = dao.getOrAddEventCategory(EVENT_TYPE);
    }

    @Test(groups = "slow")
    public void testCache() throws Exception
    {
        // Check initial state
        Assert.assertEquals(processor.getEventsReceived(), 0);
        Assert.assertEquals(timelineEventHandler.getHostEventAccumulatorCount(), 0);
        Assert.assertEquals(dao.getHosts().size(), 0);
        Assert.assertEquals(dao.getSampleKinds().size(), 0);

        final Integer hostId = dao.getOrAddHost(HOST_UUID.toString());
        Assert.assertEquals(dao.getHosts().size(), 1);

        String csvSamplesKindA = "";
        String csvSamplesKindB = "";
        for (int i = 0; i < NB_EVENTS; i++) {
            final long eventTs = new DateTime(DateTimeZone.UTC).getMillis();
            processor.processEvent(new MapEvent(eventTs, EVENT_TYPE, HOST_UUID, EVENT));

            final Integer sampleKindAId = dao.getSampleKindId(eventCategoryId, SAMPLE_KIND_A);
            Assert.assertNotNull(sampleKindAId);
            final Integer sampleKindBId = dao.getSampleKindId(eventCategoryId, SAMPLE_KIND_B);
            Assert.assertNotNull(sampleKindBId);

            // Build expected CSV output
            if (i > 0) {
                csvSamplesKindA += ",";
                csvSamplesKindB += ",";
            }
            csvSamplesKindA = String.format("%s%d,%d", csvSamplesKindA, eventTs / 1000, 12);
            csvSamplesKindB = String.format("%s%d,%d", csvSamplesKindB, eventTs / 1000, 42);

            checkProcessorState(hostId, sampleKindAId, sampleKindBId, csvSamplesKindA, csvSamplesKindB, i + 1);
        }

        final Integer sampleKindAId = dao.getSampleKindId(eventCategoryId, SAMPLE_KIND_A);
        Assert.assertNotNull(sampleKindAId);
        final Integer sampleKindBId = dao.getSampleKindId(eventCategoryId, SAMPLE_KIND_B);
        Assert.assertNotNull(sampleKindBId);

        // Check the state before the flush to the db
        Assert.assertEquals(processor.getEventsReceived(), NB_EVENTS);
        // One per host
        Assert.assertEquals(timelineEventHandler.getHostEventAccumulatorCount(), 1);
        // One per host and type
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, sampleKindAId, null, null).size(), 1);
        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, sampleKindBId, null, null).size(), 1);
        Assert.assertEquals(dao.getHosts().size(), 1);
        final BiMap<Integer, CategoryIdAndSampleKind> categoriesAndSampleKinds = dao.getSampleKinds();
        Assert.assertEquals(categoriesAndSampleKinds.size(), 2);
        final List<String> sampleKinds = CategoryIdAndSampleKind.extractSampleKinds(categoriesAndSampleKinds.values());
        Assert.assertTrue(sampleKinds.contains(SAMPLE_KIND_A));
        Assert.assertTrue(sampleKinds.contains(SAMPLE_KIND_B));

        Thread.sleep(2 * 1000 + 100);

        // Check the state after the flush to the db
        Assert.assertEquals(processor.getEventsReceived(), NB_EVENTS);

        // These tests are bogus, because the new system doesn't use cache flushing - - accumulators stay around forever.

//        // Should have been flushed
//        Assert.assertEquals(timelineEventHandler.getHostEventAccumulatorCount(), 0);
//        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, sampleKindAId, null, null).size(), 0);
//        Assert.assertEquals(timelineEventHandler.getInMemoryTimelineChunks(hostId, sampleKindBId, null, null).size(), 0);
    }

    private void checkProcessorState(final Integer hostId, final Integer sampleKindAId, final Integer sampleKindBId,
                                     final String csvSamplesKindA, final String csvSamplesKindB, final int eventSent) throws IOException, ExecutionException
    {
        Assert.assertEquals(timelineEventHandler.getHostEventAccumulatorCount(), 1);
        Assert.assertEquals(processor.getEventsReceived(), eventSent);

        // One per host and per type (two types here: kindA and kindB)
        final Collection<? extends TimelineChunk> inMemoryTimelineChunkA = timelineEventHandler.getInMemoryTimelineChunks(hostId, sampleKindAId, null, null);
        Assert.assertEquals(inMemoryTimelineChunkA.size(), 1);
        Assert.assertEquals(inMemoryTimelineChunkA.iterator().next().getSamplesAsCSV(), csvSamplesKindA);

        final Collection<? extends TimelineChunk> inMemoryTimelineChunkB = timelineEventHandler.getInMemoryTimelineChunks(hostId, sampleKindBId, null, null);
        Assert.assertEquals(inMemoryTimelineChunkB.size(), 1);
        Assert.assertEquals(inMemoryTimelineChunkB.iterator().next().getSamplesAsCSV(), csvSamplesKindB);
    }
}
