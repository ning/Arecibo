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

import com.google.common.collect.BiMap;
import com.ning.arecibo.collector.guice.CollectorRESTEventReceiverModule;
import com.ning.arecibo.collector.process.CollectorEventProcessor;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.publisher.EventSenderType;
import com.ning.arecibo.event.publisher.RESTEventService;
import com.ning.arecibo.event.receiver.UDPEventReceiverModule;
import com.ning.arecibo.event.transport.EventSerializer;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.event.transport.EventServiceRESTClient;
import com.ning.arecibo.event.transport.JsonEventSerializer;
import com.ning.arecibo.util.EmbeddedJettyJerseyModule;
import com.ning.arecibo.util.lifecycle.LifecycleModule;
import com.ning.arecibo.util.rmi.RMIModule;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.http.client.AsyncHttpClient;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

@Guice(modules = {
    LifecycleModule.class,
    EmbeddedJettyJerseyModule.class,
    UDPEventReceiverModule.class,
    RMIModule.class,
    CollectorTestModule.class,
    CollectorRESTEventReceiverModule.class
})
public class TestEventCollectorServer
{
    @Inject
    MysqlTestingHelper helper;

    @Inject
    EventCollectorServer server;

    @Inject
    CollectorEventProcessor processor;

    @Inject
    TimelineDAO timelineDAO;

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
                    server.run();
                }
                catch (Exception e) {
                    Assert.fail();
                }
            }
        });

        while (!server.isRunning()) {
            Thread.sleep(1000);
        }
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
        Assert.assertEquals(processor.getEventsDiscarded(), 0);

        final UUID hostId = UUID.randomUUID();
        final DateTime startTime = new DateTime(DateTimeZone.UTC);
        DateTime endTime = startTime;
        final int sampleCount = 10;
        for (int i = 0; i < sampleCount; i++) {
            endTime = startTime.plusMinutes(i);
            final MapEvent event = createEvent(hostId, endTime.getMillis());
            service.sendREST(event);

            Assert.assertEquals(processor.getEventsReceived(), 1 + i);
            Assert.assertEquals(processor.getEventsDiscarded(), 0);

            // Make sure we don't create dups
            final BiMap<Integer, String> hosts = timelineDAO.getHosts();
            Assert.assertEquals(hosts.values().size(), 1);
            Assert.assertEquals(hosts.values().toArray()[0], hostId.toString());

            // Make sure we saw all sample kinds
            final BiMap<Integer, String> sampleKinds = timelineDAO.getSampleKinds();
            Assert.assertEquals(sampleKinds.values().size(), event.getKeys().size());
            Assert.assertTrue(sampleKinds.values().containsAll(event.getKeys()));
        }

        processor.forceCommit();
        // Might take a while
        Thread.sleep(100);

        final List<TimelineChunkAndTimes> chunkAndTimes = timelineDAO.getSamplesByHostName(hostId.toString(), startTime, endTime);
        // 1 host x 2 sample kinds
        Assert.assertEquals(chunkAndTimes.size(), 2);
        // Only one
        Assert.assertEquals(chunkAndTimes.get(0).getHostName(), hostId.toString());
        Assert.assertEquals(chunkAndTimes.get(1).getHostName(), hostId.toString());
        // Two types
        Assert.assertEquals(chunkAndTimes.get(0).getSampleKind(), "min_heapUsed");
        Assert.assertEquals(chunkAndTimes.get(1).getSampleKind(), "max_heapUsed");

        // Only one
        Assert.assertEquals(chunkAndTimes.get(0).getTimelineChunk().getHostId(), 1);
        Assert.assertEquals(chunkAndTimes.get(1).getTimelineChunk().getHostId(), 1);
        // Two types
        Assert.assertEquals(chunkAndTimes.get(0).getTimelineChunk().getSampleKindId(), 1);
        Assert.assertEquals(chunkAndTimes.get(1).getTimelineChunk().getSampleKindId(), 2);
        // Only one
        Assert.assertEquals(chunkAndTimes.get(0).getTimelineChunk().getTimelineTimesId(), 1);
        Assert.assertEquals(chunkAndTimes.get(1).getTimelineChunk().getTimelineTimesId(), 1);
        // Number of events sent
        Assert.assertEquals(chunkAndTimes.get(0).getTimelineChunk().getSampleCount(), sampleCount);
        Assert.assertEquals(chunkAndTimes.get(1).getTimelineChunk().getSampleCount(), sampleCount);

        // Only one
        Assert.assertEquals(chunkAndTimes.get(0).getTimelineTimes().getHostId(), 1);
        Assert.assertEquals(chunkAndTimes.get(1).getTimelineTimes().getHostId(), 1);
        // When we started sending events (we store seconds granularity)
        Assert.assertEquals(chunkAndTimes.get(0).getTimelineTimes().getStartTime().getMillis() / 1000, startTime.getMillis() / 1000);
        Assert.assertEquals(chunkAndTimes.get(1).getTimelineTimes().getStartTime().getMillis() / 1000, startTime.getMillis() / 1000);
        // When we finished sending events (we store seconds granularity)
        Assert.assertEquals(chunkAndTimes.get(0).getTimelineTimes().getEndTime().getMillis() / 1000, endTime.getMillis() / 1000);
        Assert.assertEquals(chunkAndTimes.get(1).getTimelineTimes().getEndTime().getMillis() / 1000, endTime.getMillis() / 1000);
        // Each event was sent at a separate time
        Assert.assertEquals(chunkAndTimes.get(0).getTimelineTimes().getSampleCount(), sampleCount);
        Assert.assertEquals(chunkAndTimes.get(1).getTimelineTimes().getSampleCount(), sampleCount);
        // Check all the timelines events
        for (int i = 0; i < sampleCount; i++) {
            Assert.assertEquals(chunkAndTimes.get(0).getTimelineTimes().getSampleTimestamp(i).getMillis() / 1000, startTime.plusMinutes(i).getMillis() / 1000);
            Assert.assertEquals(chunkAndTimes.get(1).getTimelineTimes().getSampleTimestamp(i).getMillis() / 1000, startTime.plusMinutes(i).getMillis() / 1000);
        }
    }

    private RESTEventService createService(final EventSerializer serializer)
    {
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put(EventService.HOST, "0.0.0.0");
        properties.put(EventService.JETTY_PORT, "8088");
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

        return new MapEvent(ts, "myType", hostId, data);
    }
}
