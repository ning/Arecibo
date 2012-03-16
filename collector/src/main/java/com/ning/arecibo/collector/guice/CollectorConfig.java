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

package com.ning.arecibo.collector.guice;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.Description;
import org.skife.config.TimeSpan;

public interface CollectorConfig
{
    @Config("arecibo.collector.serviceLocatorKlass")
    @Description("ServiceLocator implementation for announcements and discovery")
    @Default("com.ning.arecibo.util.service.DummyServiceLocator")
    String getServiceLocatorClass();

    @Config("arecibo.collector.serviceName")
    @Description("Arecibo service name, used for announcements and discovery")
    @Default("AreciboCollectorService")
    String getCollectorServiceName();

    @Config("arecibo.collector.extraGuiceModules")
    @Description("Extra Guice modules to be installed")
    @Default("")
    String getExtraGuiceModules();

    @Config("arecibo.collector.eventSerializersKlass")
    @Description("Serializers classes to use to deserialize incoming events")
    @Default("com.ning.arecibo.event.transport.JavaEventSerializer,com.ning.arecibo.event.transport.JsonEventSerializer,com.ning.arecibo.event.transport.MapEventSerializer")
    String getEventSerializers();

    @Config("arecibo.collector.timelines.maxHosts")
    @Description("Max number of different hosts to keep in memory at the same time")
    @Default("10000")
    int getMaxHosts();

    @Config("arecibo.collector.timelines.maxSampleKinds")
    @Description("Max number of different sample kinds to keep in memory at the same time")
    @Default("10000")
    int getMaxSampleKinds();

    @Config("arecibo.collector.timelines.length")
    @Description("How long to buffer data in memory before flushing it to the database")
    @Default("10m")
    TimeSpan getTimelineLength();

    @Config("arecibo.collector.timelines.aggregationInterval")
    @Description("How often to check to see if there are timelines ready to be aggregated")
    @Default("1h")
    TimeSpan getAggregationInterval();

    @Config("arecibo.collector.timelines.deleteAggregatedChunks")
    @Description("If true, blast the old TimelineTimes and TimelineChunk rows; if false, leave them in peace, since they won't be accessed")
    @Default("false")
    boolean getDeleteAggregatedChunks();

    @Config("arecibo.collector.timelines.spoolDir")
    @Description("Spool directory for in-memory data")
    @Default("/var/tmp/arecibo")
    String getSpoolDir();

    @Config("arecibo.collector.timelines.chunksToAggregate")
    @Description("The number of unaggregated sequential TimelineTimes chunks we must find to perform aggregation")
    @Default("12")
    int getChunksToAggregate();

    @Config("arecibo.collector.rt.kafka.enabled")
    @Description("Whether Kafka is enabled")
    @Default("false")
    boolean isKafkaEnabled();

    @Config("arecibo.collector.rt.kafka.zkConnect")
    @Description("zkConnect string for Kafka")
    @Default("127.0.0.1:2181")
    String getZkConnect();

    @Config("arecibo.collector.rt.kafka.zkConnectionTimeout")
    @Description("Zookeeper timeout")
    @Default("6s")
    TimeSpan getZkConnectionTimeout();

    @Config("arecibo.collector.rt.kafka.groupId")
    @Description("Kafka groupId")
    @Default("arecibo")
    String getKafkaGroupId();
}
