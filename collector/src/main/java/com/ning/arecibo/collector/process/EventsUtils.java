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

import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.MonitoringEvent;
import com.ning.arecibo.eventlogger.Event;

import java.util.Map;

public class EventsUtils
{
    public static String getHostNameFromEvent(final Event event)
    {
        String host = event.getSourceUUID().toString();
        if (event instanceof MonitoringEvent) {
            host = ((MonitoringEvent) event).getHostName();
        }
        else if (event instanceof MapEvent) {
            final Object hostName = ((MapEvent) event).getMap().get("hostName");
            if (hostName != null) {
                host = hostName.toString();
            }
        }

        return host;
    }

    public static Map<String, Object> getSamplesFromEvent(final Event event)
    {
        if (event instanceof MapEvent) {
            return ((MapEvent) event).getMap();
        }
        else if (event instanceof MonitoringEvent) {
            return ((MonitoringEvent) event).getMap();
        }
        else {
            return null;
        }
    }
}
