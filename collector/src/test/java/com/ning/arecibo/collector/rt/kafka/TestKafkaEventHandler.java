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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mogwee.executors.Executors;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.collector.process.CollectorEventProcessor;
import com.ning.arecibo.collector.process.EventHandler;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaMessageStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.Message;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class TestKafkaEventHandler
{
    private static final ObjectMapper mapper = new ObjectMapper();
    // To avoid collisions when running the tests multiple times
    private static final String TOPIC = UUID.randomUUID().toString();
    private static final String ZK_CONNECT = "127.0.0.1:2181";

    private final List<Map<String, String>> messagesReceived = new ArrayList<Map<String, String>>();

    private CollectorEventProcessor processor;
    private KafkaEventHandler kafkaEventHandler;
    private KafkaMessageStream<Message> stream;
    private Event event;
    private ExecutorService consumerService;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        System.setProperty("arecibo.collector.rt.kafka.zkConnect", ZK_CONNECT);
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        kafkaEventHandler = new KafkaEventHandler(config);
        processor = new CollectorEventProcessor(ImmutableList.<EventHandler>of(kafkaEventHandler));

        stream = createKafkaConsumer();

        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("min_heapUsed", Double.valueOf("1.515698888E9"));
        data.put("max_heapUsed", Double.valueOf("1.835511784E9"));

        event = new MapEvent(new DateTime(DateTimeZone.UTC).getMillis(), TOPIC, UUID.randomUUID(), data);
        consumerService = Executors.newFixedThreadPool(1, "KafaConsumer");
        consumerService.submit(new KafkaHandlerRunnable());
    }

    private final class KafkaHandlerRunnable implements Runnable
    {
        @Override
        public void run()
        {
            for (final Message message : stream) {
                if (message.isValid()) {
                    try {
                        final ByteBuffer payload = message.payload();
                        final ByteArrayOutputStream out = new ByteArrayOutputStream();
                        out.write(payload.array(), payload.arrayOffset(), payload.limit());
                        out.close();

                        final Map<String, String> readMessage = mapper.readValue(out.toByteArray(), new TypeReference<HashMap<String, String>>()
                        {
                        });
                        messagesReceived.add(readMessage);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    @Test(groups = "slow,integration")
    public void testHandle() throws Exception
    {
        Assert.assertEquals(processor.getEventsReceived(), 0);
        Assert.assertEquals(kafkaEventHandler.getEventsSent(), 0);
        Assert.assertEquals(kafkaEventHandler.getEventsDiscarded(), 0);
        Assert.assertEquals(messagesReceived.size(), 0);

        processor.processEvent(event);

        Assert.assertEquals(processor.getEventsReceived(), 1);
        Assert.assertEquals(kafkaEventHandler.getEventsSent(), 1);
        Assert.assertEquals(kafkaEventHandler.getEventsDiscarded(), 0);

        // Wait a bit, it takes some time...
        Thread.sleep(10000);
        consumerService.shutdown();

        Assert.assertEquals(messagesReceived.size(), 1);

        final Map<String, String> map = messagesReceived.get(0);
        Assert.assertEquals(map.get("sourceUUID"), event.getSourceUUID().toString());
        Assert.assertEquals(map.get("eventType"), event.getEventType());
        Assert.assertEquals(Long.valueOf(map.get("timestamp")), (Long) event.getTimestamp());
        Assert.assertEquals(Double.valueOf(map.get("min_heapUsed")), ((MapEvent) event).getMap().get("min_heapUsed"));
        Assert.assertEquals(Double.valueOf(map.get("max_heapUsed")), ((MapEvent) event).getMap().get("max_heapUsed"));
    }

    private KafkaMessageStream<Message> createKafkaConsumer()
    {
        final Properties props = new Properties();
        props.put("zk.connect", ZK_CONNECT);
        props.put("zk.connectiontimeout.ms", "1000000");
        props.put("groupid", "arecibo");

        final ConsumerConfig consumerConfig = new ConsumerConfig(props);
        final ConsumerConnector consumerConnector = Consumer.createJavaConsumerConnector(consumerConfig);

        final Map<String, List<KafkaMessageStream<Message>>> topicMessageStreams = consumerConnector.createMessageStreams(ImmutableMap.of(TOPIC, 1));
        final List<KafkaMessageStream<Message>> streams = topicMessageStreams.get(TOPIC);
        return streams.get(0);
    }
}
