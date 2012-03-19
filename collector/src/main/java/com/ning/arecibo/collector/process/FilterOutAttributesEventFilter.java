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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.MonitoringEvent;
import com.ning.arecibo.eventlogger.Event;

import javax.annotation.Nullable;
import java.util.Map;

public class FilterOutAttributesEventFilter implements Function<Event, Event>
{
    private final Iterable<String> attributesToIgnore;

    @VisibleForTesting
    public FilterOutAttributesEventFilter(final Iterable<String> attributesToIgnore)
    {
        this.attributesToIgnore = attributesToIgnore;
    }

    @Inject
    public FilterOutAttributesEventFilter(final CollectorConfig config)
    {
        this(Splitter.on(",").split(config.getAttributesToIgnore()));
    }

    @Override
    public Event apply(@Nullable final Event event)
    {
        if (event == null) {
            return null;
        }

        final Map<String, Object> samplesMap;
        if (event instanceof MapEvent) {
            samplesMap = ((MapEvent) event).getMap();
        }
        else if (event instanceof MonitoringEvent) {
            samplesMap = ((MonitoringEvent) event).getMap();
        }
        else {
            return event;
        }

        for (final String attributeToIgnore : attributesToIgnore) {
            if (samplesMap.containsKey(attributeToIgnore)) {
                samplesMap.remove(attributeToIgnore);
            }
        }

        // Empty event?
        if (samplesMap.keySet().size() == 0) {
            return null;
        }
        else {
            return event;
        }
    }
}
