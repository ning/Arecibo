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

import com.google.inject.Inject;
import com.ning.arecibo.event.BatchedEvent;
import com.ning.arecibo.event.receiver.EventProcessor;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class CollectorEventProcessor implements EventProcessor
{
    private static final Logger log = Logger.getLogger(CollectorEventProcessor.class);

    private final AtomicLong eventsReceived = new AtomicLong(0L);
    private final List<EventHandler> eventHandlers;

    @Inject
    public CollectorEventProcessor(final List<EventHandler> eventHandlers) throws IOException
    {
        this.eventHandlers = eventHandlers;
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

        // Dispatch all events
        for (final Event event : events) {
            for (final EventHandler handler : eventHandlers) {
                try {
                    handler.handle(event);
                }
                catch (RuntimeException ruEx) {
                    log.warn(ruEx);
                }
            }
        }
    }

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventsReceived()
    {
        return eventsReceived.get();
    }
}
