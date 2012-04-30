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
import org.skife.config.DefaultNull;
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

    @Config("arecibo.collector.eventFilterKlass")
    @Description("Class to use to filter events")
    @DefaultNull
    String getEventFilterClass();

    @Config("arecibo.collector.attributesToIgnore")
    @Description("When using the provided FilterOutAttributesEventFilter class, list of attributes to filter out on the way in")
    @Default("")
    String getAttributesToIgnore();

    @Config("arecibo.collector.timelines.length")
    @Description("How long to buffer data in memory before flushing it to the database")
    @Default("60m")
    TimeSpan getTimelineLength();

    // This is used to predict the number of samples between two times.  It might be
    // better to store this information on a per event category basis.
    @Config("arecibo.collector.timelines.pollingInterval")
    @Description("How long to between attribute polling.  This constant should be replaced by a flexible mechanism")
    @Default("30s")
    TimeSpan getPollingInterval();

    @Config("arecibo.collector.timelines.performForegroundWrites")
    @Description("If true, perform database writes in the foreground; if false, in the background")
    @Default("false")
    boolean getPerformForegroundWrites();

    @Config("arecibo.collector.timelines.backgroundWriteBatchSize")
    @Description("The number of TimelineChunks that must accumulate before we perform background writes, unless the max delay has been exceeded")
    @Default("1000")
    int getBackgroundWriteBatchSize();

    @Config("arecibo.collector.timelines.backgroundWriteCheckInterval")
    @Description("The time interval between checks to see if we should perform background writes")
    @Default("1s")
    TimeSpan getBackgroundWriteCheckInterval();

    @Config("arecibo.collector.timelines.backgroundWriteMaxDelay")
    @Description("The maximum timespan after a pending chunks are added before we perform background writes")
    @Default("1m")
    TimeSpan getBackgroundWriteMaxDelay();

    @Config("arecibo.collector.timelines.timelineAggregationEnabled")
    @Description("If true, periodically perform timeline aggregation; if false, don't aggregate")
    @Default("true")
    boolean getTimelineAggregationEnabled();

    @Config("arecibo.collector.timelines.maxAggregationLevel")
    @Description("Max aggregation level")
    @Default("5")
    int getMaxAggregationLevel();

    @Config("arecibo.collector.timelines.chunksToAggregate")
    @Description("A string with a comma-separated set of integers, one for each aggregation level, giving the number of sequential TimelineChunks with that aggregation level we must find to perform aggregation")
    // These values translate to 4 hours, 16 hours, 2.7 days, 10.7 days, 42.7 days,
    @Default("4,4,4,4,4")
    String getChunksToAggregate();

    @Config("arecibo.collector.timelines.aggregationInterval")
    @Description("How often to check to see if there are timelines ready to be aggregated")
    @Default("2h")
    TimeSpan getAggregationInterval();

    @Config("arecibo.collector.timelines.aggregationBatchSize")
    @Description("The number of chunks to fetch in each batch processed")
    @Default("4000")
    int getAggregationBatchSize();

    @Config("arecibo.collector.timelines.aggregationSleepBetweenBatches")
    @Description("How long to sleep between aggregation batches")
    @Default("50ms")
    TimeSpan getAggregationSleepBetweenBatches();

    @Config("arecibo.collector.timelines.maxChunkIdsToInvalidateOrDelete")
    @Description("If the number of queued chunkIds to invalidate or delete is greater than or equal to this count, perform aggregated timeline writes and delete or invalidate the chunks aggregated")
    @Default("1000")
    int getMaxChunkIdsToInvalidateOrDelete();

    @Config("arecibo.collector.timelines.deleteAggregatedChunks")
    @Description("If true, blast the old TimelineChunk rows; if false, leave them in peace, since they won't be accessed")
    @Default("true")
    boolean getDeleteAggregatedChunks();

    @Config("arecibo.collector.timelines.shutdownSaveMode")
    @Description("What to save on shut down; either all timelines (save_all_timelines) or just the accumulator start times (save_start_times)")
    @Default("save_all_timelines")
    String getShutdownSaveMode();

    @Config("arecibo.collector.timelines.segmentsSize")
    @Description("Direct memory segments size in bytes to allocate when buffering incoming events")
    @Default("1048576")
    int getSegmentsSize();

    @Config("arecibo.collector.timelines.maxNbSegments")
    @Description("Max number of direct memory segments to allocate. This times the number of segments indicates the max amount of data buffered before storing a copy to disk")
    @Default("10")
    int getMaxNbSegments();

    @Config("arecibo.collector.timelines.spoolDir")
    @Description("Spool directory for in-memory data")
    @Default("/var/tmp/arecibo")
    String getSpoolDir();

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

    @Config("arecibo.collector.runLoadGenerator")
    @Description("If true, in additional to processing any incoming events, start the load generator to create generated events")
    @Default("false")
    boolean getRunLoadGenerator();
}
