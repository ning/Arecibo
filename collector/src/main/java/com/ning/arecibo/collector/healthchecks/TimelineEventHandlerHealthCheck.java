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

package com.ning.arecibo.collector.healthchecks;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.event.MapEvent;
import com.yammer.metrics.core.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

@Singleton
public class TimelineEventHandlerHealthCheck extends HealthCheck
{
    private static final String EVENT_TYPE = "AreciboCollectorHealthCheck";
    private static final UUID EVENT_UUID = UUID.randomUUID();

    private final Logger log = LoggerFactory.getLogger(TimelineEventHandlerHealthCheck.class);

    private final TimelineEventHandler processor;

    @Inject
    public TimelineEventHandlerHealthCheck(final TimelineEventHandler eventHandler)
    {
        this.processor = eventHandler;
    }

    @Override
    public String name()
    {
        return TimelineEventHandlerHealthCheck.class.getSimpleName();
    }

    @Override
    public Result check() throws Exception
    {
        try {
            final Map<String, Object> payload = ImmutableMap.<String, Object>of(
                "short", Short.MAX_VALUE,
                "int", Integer.MAX_VALUE,
                "long", Long.MAX_VALUE,
                "float", Float.MAX_VALUE,
                "double", Double.MAX_VALUE
            );
            processor.handle(new MapEvent(System.currentTimeMillis(), EVENT_TYPE, EVENT_UUID, payload));
        }
        catch (Exception e) {
            log.warn("{} check failed", name());
            return Result.unhealthy(e);
        }

        log.info("{} check succeeded", name());
        return Result.healthy();
    }
}
