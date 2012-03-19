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

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.ning.arecibo.collector.guice.EventFilter;
import com.ning.arecibo.event.BatchedEvent;
import com.ning.arecibo.event.receiver.EventProcessor;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class CollectorEventProcessor implements EventProcessor
{
    private static final Logger log = LoggerFactory.getLogger(CollectorEventProcessor.class);

    private final AtomicLong eventsReceived = new AtomicLong(0L);
    private final AtomicLong eventsFiltered = new AtomicLong(0L);
    private final List<EventHandler> eventHandlers;
    private final Function<Event, Event> filter;

    @Inject
    public CollectorEventProcessor(final List<EventHandler> eventHandlers, @EventFilter final Function<Event, Event> filter) throws IOException
    {
        this.eventHandlers = eventHandlers;
        this.filter = filter;
        log.info("Event processor filter: {}", filter.getClass().toString());
    }

    public void processEvent(final Event evt)
    {
        final List<Event> events = new ArrayList<Event>();
        if (evt instanceof BatchedEvent) {
            events.addAll(((BatchedEvent) evt).getEvents());
        }
        else {
            events.add(evt);
        }

        // Update stats
        eventsReceived.getAndAdd(events.size());

        // Filter events
        final Collection<Event> filteredEvents = Lists.newArrayList(
            Iterables.filter(
                Iterables.transform(events, filter),
                Predicates.<Event>notNull()
            )
        );
        eventsFiltered.addAndGet(events.size() - filteredEvents.size());

        // Dispatch all events
        for (final Event event : filteredEvents) {
            for (final EventHandler handler : eventHandlers) {
                try {
                    handler.handle(event);
                }
                catch (RuntimeException ruEx) {
                    log.warn("Exception handling event", ruEx);
                }
            }
        }
    }

    @MonitorableManaged(description = "Number of events received", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventsReceived()
    {
        return eventsReceived.get();
    }

    @MonitorableManaged(description = "Number of events dropped by filters", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventsFiltered()
    {
        return eventsFiltered.get();
    }
}
