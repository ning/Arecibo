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

package com.ning.arecibo.collector.rt.kafka;

import com.google.inject.Inject;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.collector.process.EventHandler;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import kafka.javaapi.producer.Producer;
import kafka.javaapi.producer.ProducerData;
import kafka.producer.ProducerConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaEventHandler implements EventHandler
{
    private static final Logger log = Logger.getLogger(KafkaEventHandler.class);

    private final AtomicLong eventsSent = new AtomicLong(0L);
    private final AtomicLong eventsDiscarded = new AtomicLong(0L);

    final ObjectMapper mapper = new ObjectMapper();
    final Producer<String, String> producer;

    @Inject
    public KafkaEventHandler(final CollectorConfig config)
    {
        final Properties props = new Properties();
        props.put("zk.connect", config.getZkConnect());
        props.put("zk.connectiontimeout.ms", String.valueOf(config.getZkConnectionTimeout().getMillis()));
        props.put("groupid", config.getKafkaGroupId());
        props.put("serializer.class", "kafka.serializer.StringEncoder");

        final ProducerConfig producerConfig = new ProducerConfig(props);
        producer = new Producer<String, String>(producerConfig);
    }

    @Override
    public void handle(final Event event)
    {
        try {
            final ProducerData<String, String> data = new ProducerData<String, String>(event.getEventType(), eventToMessage(event));
            producer.send(data);
            eventsSent.incrementAndGet();
        }
        catch (IOException e) {
            log.warn(e);
            eventsDiscarded.getAndIncrement();
        }
    }

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventsSent()
    {
        return eventsSent.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventsDiscarded()
    {
        return eventsDiscarded.get();
    }

    private String eventToMessage(final Event event) throws IOException
    {
        return mapper.writeValueAsString(event);
    }
}
