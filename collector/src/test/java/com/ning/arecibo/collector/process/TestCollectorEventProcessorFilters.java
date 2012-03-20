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
import com.ning.arecibo.collector.MockEventHandler;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.UUID;

public class TestCollectorEventProcessorFilters
{
    private static final UUID HOST_UUID = UUID.randomUUID();
    private static final String EVENT_TYPE = "eventType";
    private static final String KIND_A = "kindA";
    private static final String KIND_B = "kindB";
    private static final Map<String, Object> EVENT = ImmutableMap.<String, Object>of(KIND_A, 12, KIND_B, 42);
    private static final int NB_EVENTS = 5;

    @Test(groups = "fast")
    public void testIdentityFilter() throws Exception
    {
        final MockEventHandler eventHandler = new MockEventHandler();
        final CollectorEventProcessor processor = new CollectorEventProcessor(ImmutableList.<EventHandler>of(eventHandler), Functions.<Event>identity());

        for (int i = 0; i < NB_EVENTS; i++) {
            final long eventTs = new DateTime(DateTimeZone.UTC).getMillis();
            processor.processEvent(new MapEvent(eventTs, EVENT_TYPE, HOST_UUID, EVENT));
        }

        Assert.assertEquals(processor.getEventsReceived(), NB_EVENTS);
        Assert.assertEquals(processor.getEventsFiltered(), 0);
        Assert.assertEquals(eventHandler.getEventsHandled().size(), NB_EVENTS);
        for (int i = 0; i < NB_EVENTS; i++) {
            Assert.assertTrue(((MapEvent) eventHandler.getEventsHandled().get(i)).getKeys().contains(KIND_A));
            Assert.assertTrue(((MapEvent) eventHandler.getEventsHandled().get(i)).getKeys().contains(KIND_B));
        }
    }

    @Test(groups = "fast")
    public void testFilterOutOneAttributeEventFilter() throws Exception
    {
        final MockEventHandler eventHandler = new MockEventHandler();
        final CollectorEventProcessor processor = new CollectorEventProcessor(ImmutableList.<EventHandler>of(eventHandler),
            new FilterOutAttributesEventFilter(ImmutableList.<String>of(KIND_A)));

        for (int i = 0; i < NB_EVENTS; i++) {
            final long eventTs = new DateTime(DateTimeZone.UTC).getMillis();
            processor.processEvent(new MapEvent(eventTs, EVENT_TYPE, HOST_UUID, EVENT));
        }

        Assert.assertEquals(processor.getEventsReceived(), NB_EVENTS);
        // Only a single attribute from the events was filtered out, no event got actually dropped
        Assert.assertEquals(processor.getEventsFiltered(), 0);
        Assert.assertEquals(eventHandler.getEventsHandled().size(), NB_EVENTS);
        for (int i = 0; i < NB_EVENTS; i++) {
            Assert.assertFalse(((MapEvent) eventHandler.getEventsHandled().get(i)).getKeys().contains(KIND_A));
            Assert.assertTrue(((MapEvent) eventHandler.getEventsHandled().get(i)).getKeys().contains(KIND_B));
        }
    }

    @Test(groups = "fast")
    public void testFilterOutAllAttributesEventFilter() throws Exception
    {
        final MockEventHandler eventHandler = new MockEventHandler();
        final CollectorEventProcessor processor = new CollectorEventProcessor(ImmutableList.<EventHandler>of(eventHandler),
            new FilterOutAttributesEventFilter(ImmutableList.<String>of(KIND_A, KIND_B)));

        for (int i = 0; i < NB_EVENTS; i++) {
            final long eventTs = new DateTime(DateTimeZone.UTC).getMillis();
            processor.processEvent(new MapEvent(eventTs, EVENT_TYPE, HOST_UUID, EVENT));
        }

        Assert.assertEquals(processor.getEventsReceived(), NB_EVENTS);
        // All attributes from the events were filtered out, events were dropped
        Assert.assertEquals(processor.getEventsFiltered(), NB_EVENTS);
        Assert.assertEquals(eventHandler.getEventsHandled().size(), 0);
    }
}
