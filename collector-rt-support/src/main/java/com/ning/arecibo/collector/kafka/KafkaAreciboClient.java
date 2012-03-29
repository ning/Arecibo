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

package com.ning.arecibo.collector.kafka;

import com.ning.arecibo.collector.RealtimeClient;
import com.ning.arecibo.collector.RealtimeClientConfig;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.mogwee.executors.Executors;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaMessageStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaAreciboClient implements RealtimeClient<Message>
{
    private static final Logger log = LoggerFactory.getLogger(KafkaAreciboClient.class);
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JodaModule());

    private final Map<String, ExecutorService> executorServicePerTopic = new ConcurrentHashMap<String, ExecutorService>();
    private final Map<String, ConsumerConnector> kafkaConnectorPerTopic = new ConcurrentHashMap<String, ConsumerConnector>();

    private final AtomicLong messagesReceived = new AtomicLong(0L);
    private final AtomicLong validMessagesReceived = new AtomicLong(0L);
    private final AtomicLong unParseableMessagesReceived = new AtomicLong(0L);
    private final AtomicLong corruptedMessagesReceived = new AtomicLong(0L);

    private final RealtimeClientConfig config;

    public KafkaAreciboClient(final RealtimeClientConfig config)
    {
        this.config = config;
    }

    @Override
    public void listenToStream(final String topic, final Function<Event, Void> onValidMessage, final Function<Message, Void> onCorruptedMessage)
    {
        final ExecutorService executor = Executors.newFixedThreadPool(config.getNbThreads(), "Arecibo-Kafka-Listener");
        executorServicePerTopic.put(topic, executor);

        for (final KafkaMessageStream<Message> stream : createKafkaConsumers(topic)) {
            executor.submit(new Runnable()
            {
                public void run()
                {
                    for (final Message message : stream) {
                        messagesReceived.incrementAndGet();

                        if (message.isValid()) {
                            validMessagesReceived.incrementAndGet();

                            try {
                                final ByteBuffer payload = message.payload();
                                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                                out.write(payload.array(), payload.arrayOffset(), payload.limit());
                                out.close();

                                final byte[] bytes = out.toByteArray();
                                final Event event = convertByteBufferPayloadToEvent(bytes);
                                onValidMessage.apply(event);
                            }
                            catch (IOException e) {
                                unParseableMessagesReceived.incrementAndGet();
                                log.warn("Exception parsing Message from Kafka", e);
                            }
                        }
                        else {
                            corruptedMessagesReceived.incrementAndGet();
                            onCorruptedMessage.apply(message);
                        }
                    }
                }
            });
        }
    }

    @Override
    public synchronized void stopListening(final String topic)
    {
        final ExecutorService service = executorServicePerTopic.remove(topic);
        if (service == null) {
            return;
        }
        else {
            service.shutdownNow();
        }

        final ConsumerConnector connector = kafkaConnectorPerTopic.remove(topic);
        if (connector != null) {
            connector.shutdown();
        }
    }

    @VisibleForTesting
    static Event convertByteBufferPayloadToEvent(final byte[] bytes) throws IOException
    {
        // We convert only to MapEvent for now
        // TODO - should be handle separately MonitoringEvents?
        return mapper.readValue(bytes, MapEvent.class);
    }

    private List<KafkaMessageStream<Message>> createKafkaConsumers(final String topic)
    {
        final Properties props = new Properties();
        props.put("zk.connect", config.getZkConnect());
        props.put("zk.connectiontimeout.ms", config.getZkConnectionTimeout());
        props.put("groupid", config.getKafkaGroupId());

        final ConsumerConfig consumerConfig = new ConsumerConfig(props);
        final ConsumerConnector consumerConnector = Consumer.createJavaConsumerConnector(consumerConfig);

        // TODO - should we share the connectors across topics?
        kafkaConnectorPerTopic.put(topic, consumerConnector);

        final Map<String, List<KafkaMessageStream<Message>>> topicMessageStreams = consumerConnector.createMessageStreams(ImmutableMap.of(topic, config.getNbThreads()));
        return topicMessageStreams.get(topic);
    }

    @MonitorableManaged(description = "Number of messages received", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getMessagesReceived()
    {
        return messagesReceived.get();
    }

    @MonitorableManaged(description = "Number of valid messages received", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getValidMessagesReceived()
    {
        return validMessagesReceived.get();
    }

    @MonitorableManaged(description = "Number of messages received that couldn't be parsed into an Event ", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getUnParseableMessagesReceived()
    {
        return unParseableMessagesReceived.get();
    }

    @MonitorableManaged(description = "Number of corrupted (invalid crc) messages received", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getCorruptedMessagesReceived()
    {
        return corruptedMessagesReceived.get();
    }
}
