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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.ning.arecibo.collector.persistent.BackgroundDBChunkWriter;
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.collector.process.CollectorEventProcessor;
import com.ning.arecibo.collector.process.EventHandler;
import com.ning.arecibo.dao.MysqlTestingHelper;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.publisher.EventSenderType;
import com.ning.arecibo.event.publisher.RESTEventService;
import com.ning.arecibo.event.transport.EventSerializer;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.event.transport.EventServiceRESTClient;
import com.ning.arecibo.event.transport.JsonEventSerializer;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.timeline.CategoryIdAndSampleKind;
import com.ning.arecibo.util.timeline.chunks.TimelineChunk;
import com.ning.arecibo.util.timeline.persistent.TimelineDAO;
import com.ning.arecibo.util.timeline.times.TimelineCursor;
import com.ning.arecibo.util.timeline.times.TimelineCursorImpl;
import com.ning.http.client.AsyncHttpClient;

@Guice(moduleFactory = TestModulesFactory.class)
public class TestEventCollectorServer
{
    private static final String EVENT_TYPE = "JVM";
    private static final String MIN_HEAPUSED_KIND = "min_heapUsed";
    private static final String MAX_HEAPUSED_KIND = "max_heapUsed";

    @Inject
    MysqlTestingHelper helper;

    @Inject
    EventCollectorServer server;

    @Inject
    CollectorEventProcessor processor;

    @Inject
    List<EventHandler> eventHandlers;

    TimelineEventHandler timelineEventHandler = null;

    @Inject
    TimelineDAO timelineDAO;

    @Inject
    BackgroundDBChunkWriter backgroundWriter;

    @Inject
    Injector injector;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        final String ddl = IOUtils.toString(TestEventCollectorServer.class.getResourceAsStream("/collector.sql"));

        helper.startMysql();
        helper.initDb(ddl);

        Executors.newFixedThreadPool(1).submit(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    server.start();
                }
                catch (Exception e) {
                    Assert.fail();
                }
            }
        });

        while (!server.isRunning()) {
            Thread.sleep(1000);
        }

        Assert.assertEquals(eventHandlers.size(), 1);
        timelineEventHandler = (TimelineEventHandler) eventHandlers.get(0);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception
    {
        helper.stopMysql();
        server.stop();
    }

    @Test(groups = "slow")
    public void testJsonClientIntegration() throws Exception
    {
        final RESTEventService service = createService(new JsonEventSerializer());

        Assert.assertEquals(processor.getEventsReceived(), 0);
        Assert.assertEquals(timelineEventHandler.getEventsDiscarded(), 0);

        final UUID hostUUID = UUID.randomUUID();
        final String hostName = hostUUID.toString();
        final int eventTypeId = timelineDAO.getOrAddEventCategory(EVENT_TYPE);
        final Integer hostId = timelineDAO.getOrAddHost(hostName);
        Assert.assertNotNull(hostId);

        final Integer minHeapUserKindId = timelineDAO.getOrAddSampleKind(hostId, eventTypeId, MIN_HEAPUSED_KIND);
        Assert.assertNotNull(minHeapUserKindId);
        final Integer maxHeapUserKindId = timelineDAO.getOrAddSampleKind(hostId, eventTypeId, MAX_HEAPUSED_KIND);
        Assert.assertNotNull(maxHeapUserKindId);

        final long now = System.currentTimeMillis();
        final DateTime startTime = new DateTime(now - (now % 1000), DateTimeZone.UTC);
        DateTime endTime = startTime;
        final int sampleCount = 10;
        for (int i = 0; i < sampleCount; i++) {
            endTime = startTime.plusSeconds(i * 10);
            final MapEvent event = createEvent(hostUUID, endTime.getMillis());
            service.sendREST(event);

            Assert.assertEquals(processor.getEventsReceived(), 1 + i);
            Assert.assertEquals(timelineEventHandler.getEventsDiscarded(), 0);
            Assert.assertEquals(timelineEventHandler.getAccumulators().size(), 1);

            // Make sure we don't create dups
            final BiMap<Integer, String> hosts = timelineDAO.getHosts();
            Assert.assertEquals(hosts.values().size(), 1);
            Assert.assertEquals(hosts.values().toArray()[0], hostName);

            // Make sure we saw all sample kinds
            final BiMap<Integer, CategoryIdAndSampleKind> categoryIdsAndSampleKinds = timelineDAO.getSampleKinds();
            Assert.assertEquals(categoryIdsAndSampleKinds.values().size(), event.getKeys().size());
            final List<String> sampleKinds = CategoryIdAndSampleKind.extractSampleKinds(categoryIdsAndSampleKinds.values());
            Assert.assertTrue(sampleKinds.contains(MIN_HEAPUSED_KIND));
            Assert.assertTrue(sampleKinds.contains(MAX_HEAPUSED_KIND));
        }

        timelineEventHandler.forceCommit();
        backgroundWriter.initiateShutdown();

        final AccumulatorConsumer consumer = new AccumulatorConsumer();
        timelineDAO.getSamplesByHostIdsAndSampleKindIds(ImmutableList.<Integer>of(hostId),
            ImmutableList.<Integer>of(minHeapUserKindId, maxHeapUserKindId), startTime, endTime, consumer);
        final List<TimelineChunk> chunks = consumer.getAccumulator();
        // 1 host x 2 sample kinds
        Assert.assertEquals(chunks.size(), 2);
        // Only one
        Assert.assertEquals((Integer)chunks.get(0).getHostId(), hostId);
        Assert.assertEquals((Integer)chunks.get(1).getHostId(), hostId);
        // Two types
        Assert.assertTrue((chunks.get(0).getSampleKindId() == minHeapUserKindId) || chunks.get(0).getSampleKindId() == maxHeapUserKindId);
        Assert.assertTrue((chunks.get(1).getSampleKindId() == minHeapUserKindId) || chunks.get(1).getSampleKindId() == maxHeapUserKindId);

        // Only one
        Assert.assertEquals(chunks.get(0).getHostId(), (int) hostId);
        Assert.assertEquals(chunks.get(1).getHostId(), (int) hostId);
        // Two types
        Assert.assertTrue(chunks.get(0).getSampleKindId() == minHeapUserKindId || chunks.get(0).getSampleKindId() == maxHeapUserKindId);
        Assert.assertTrue(chunks.get(1).getSampleKindId() == minHeapUserKindId || chunks.get(1).getSampleKindId() == maxHeapUserKindId);

        // Number of events sent
        Assert.assertEquals(chunks.get(0).getSampleCount(), sampleCount);
        Assert.assertEquals(chunks.get(1).getSampleCount(), sampleCount);

        // Only one
        Assert.assertEquals(chunks.get(0).getHostId(), (int) hostId);
        Assert.assertEquals(chunks.get(1).getHostId(), (int) hostId);
        // When we started sending events (we store seconds granularity)
        Assert.assertEquals(chunks.get(0).getStartTime().getMillis() / 1000, startTime.getMillis() / 1000);
        Assert.assertEquals(chunks.get(1).getStartTime().getMillis() / 1000, startTime.getMillis() / 1000);
        // When we finished sending events (we store seconds granularity)
        Assert.assertEquals(chunks.get(0).getEndTime().getMillis() / 1000, endTime.getMillis() / 1000);
        Assert.assertEquals(chunks.get(1).getEndTime().getMillis() / 1000, endTime.getMillis() / 1000);
        // Each event was sent at a separate time
        Assert.assertEquals(chunks.get(0).getSampleCount(), sampleCount);
        Assert.assertEquals(chunks.get(1).getSampleCount(), sampleCount);
        // Check all the timelines events
        final TimelineCursor timeCursor0 = new TimelineCursorImpl(chunks.get(0).getTimes(), chunks.get(0).getSampleCount());
        final TimelineCursor timeCursor1 = new TimelineCursorImpl(chunks.get(1).getTimes(), chunks.get(1).getSampleCount());
        for (int i = 0; i < sampleCount; i++) {
            Assert.assertEquals(timeCursor0.getNextTime(), startTime.plusSeconds(i * 10));
            Assert.assertEquals(timeCursor1.getNextTime(), startTime.plusSeconds(i * 10));
        }
    }

    private RESTEventService createService(final EventSerializer serializer)
    {
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put(EventService.HOST, String.valueOf(injector.getInstance(Key.get(String.class, Names.named(TestModulesFactory.TEST_JETTY_HOST)))));
        properties.put(EventService.JETTY_PORT, String.valueOf(injector.getInstance(Key.get(Integer.class, Names.named(TestModulesFactory.TEST_JETTY_PORT)))));
        final ServiceDescriptor localServiceDescriptor = new ServiceDescriptor("testing", properties);

        final AsyncHttpClient client = new AsyncHttpClient();
        final EventServiceRESTClient restClient = new EventServiceRESTClient(client, serializer, EventSenderType.CLIENT);

        return new RESTEventService(new MockEventServiceChooser(), localServiceDescriptor, restClient);
    }

    private MapEvent createEvent(final UUID hostId, final long ts)
    {
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("min_heapUsed", Double.valueOf("1.515698888E9"));
        data.put("max_heapUsed", Double.valueOf("1.835511784E9"));

        return new MapEvent(ts, EVENT_TYPE, hostId, data);
    }
}
