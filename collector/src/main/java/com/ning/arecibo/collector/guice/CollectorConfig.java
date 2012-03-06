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
import org.skife.config.DataAmount;
import org.skife.config.Default;
import org.skife.config.TimeSpan;

public interface CollectorConfig
{
    @Config("arecibo.events.collector.service_name")
    @Default("EventCollectorServer")
    String getCollectorServiceName();

    @Config("arecibo.events.collector.bufferWindow")
    @Default("5s")
    TimeSpan getBufferWindow();

    @Config("arecibo.events.collector.read_only_mode")
    @Default("false")
    boolean isCollectorInReadOnlyMode();

    @Config("arecibo.events.collector.hostUpdateInterval")
    @Default("5m")
    TimeSpan getHostUpdateInterval();

    @Config("arecibo.events.collector.throttlePctFreeThreshold")
    @Default("10")
    int getThrottlePctFreeThreshold();

    @Config("arecibo.events.collector.tablespace")
    @Default("ARECIBO_SYSM_NOLOGGING")
    String getTableSpaceName();

    @Config("arecibo.events.collector.tablespaceStatsUpdateInterval")
    @Default("5m")
    TimeSpan getTablespaceStatsUpdateInterval();

    @Config("arecibo.events.collector.maxTableSpace")
    @Default("1MiB")
    DataAmount getMaxTableSpace();

    @Config("arecibo.events.collector.maxSplitAndSweepInitialDelay")
    @Default("30m")
    TimeSpan getMaxSplitAndSweepInitialDelay();

    @Config("arecibo.events.collector.maxTriageThreads")
    @Default("1")
    int getMaxTriageThreads();

    @Config("arecibo.events.collector.maxBatchInsertThreads")
    @Default("1")
    int getMaxBatchInsertThreads();

    @Config("arecibo.events.collector.minBatchInsertSize")
    @Default("0")
    int getMinBatchInsertSize();

    @Config("arecibo.events.collector.maxBatchInsertSize")
    @Default("1000")
    int getMaxBatchInsertSize();

    @Config("arecibo.events.collector.maxAsyncTriageQueueSize")
    @Default("10")
    int getMaxAsyncTriageQueueSize();

    @Config("arecibo.events.collector.maxAsyncInsertQueueSize")
    @Default("100")
    int getMaxAsyncInsertQueueSize();

    @Config("arecibo.events.collector.maxPendingEvents")
    @Default("50000")
    int getMaxPendingEvents();

    @Config("arecibo.events.collector.maxPendingEventsCheckIntervalMs")
    @Default("10s")
    TimeSpan getMaxPendingEventsCheckInterval();

    @Config("arecibo.events.collector.enableBatchRetryOnIntegrityViolation")
    @Default("false")
    boolean isBatchRetryOnIntegrityViolationEnabled();

    @Config("arecibo.events.collector.enableDuplicateEventLogging")
    @Default("false")
    boolean isDuplicateEventLoggingEnabled();

    @Config("arecibo.events.collector.enablePerTableInserts")
    @Default("false")
    boolean isPerTableInsertsEnabled();

    @Config("arecibo.events.collector.enablePreparedBatchInserts")
    @Default("false")
    boolean isPreparedBatchInsertsEnabled();

    // see also com.ning.arecibo.aggregator.plugin.guice.MonitoringPluginConfig
    @Config("arecibo.events.collector.reductionFactors")
    @Default("1")
    int[] getReductionFactors();

    @Config("arecibo.events.collector.numPartitions")
    @Default("4")
    int[] getNumPartitionsToKeep();

    @Config("arecibo.events.collector.splitIntervals")
    @Default("30m")
    TimeSpan[] getSplitIntervalsInMinutes();

    @Config("arecibo.events.collector.numPartitionsToSplitAheadList")
    @Default("2")
    int[] getNumPartitionsToSplitAhead();

    @Config("arecibo.events.collector.timelines.maxHosts")
    @Default("10000")
    int getMaxHosts();

    @Config("arecibo.events.collector.timelines.length")
    @Default("10m")
    TimeSpan getTimelineLength();

    @Config("arecibo.events.collector.serviceLocatorKlass")
    @Default("com.ning.arecibo.util.service.DummyServiceLocator")
    String getServiceLocatorClass();

    @Config("arecibo.events.collector.extraGuiceModules")
    @Default("")
    String getExtraGuiceModules();

    @Config("arecibo.events.collector.eventSerializersKlass")
    @Default("com.ning.arecibo.event.transport.JavaEventSerializer,com.ning.arecibo.event.transport.JsonEventSerializer,com.ning.arecibo.event.transport.MapEventSerializer")
    String getEventSerializers();

    @Config("arecibo.events.collector.spoolDir")
    @Default("/var/tmp/arecibo")
    String getSpoolDir();

    @Config("arecibo.events.collector.rt.kafka.enabled")
    @Default("false")
    boolean isKafkaEnabled();

    @Config("arecibo.events.collector.rt.kafka.zkConnect")
    @Default("127.0.0.1:2181")
    String getZkConnect();

    @Config("arecibo.events.collector.rt.kafka.zkConnectionTimeout")
    @Default("6s")
    TimeSpan getZkConnectionTimeout();

    @Config("arecibo.events.collector.rt.kafka.groupId")
    @Default("arecibo")
    String getKafkaGroupId();
}
