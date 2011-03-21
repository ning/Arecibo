package com.ning.arecibo.collector.config;

import org.skife.config.Config;

public class CollectorConfig
{
    @Config(value = "arecibo.events.collector.reductionFactorList")
    public String getReductionFactorList()
    {
        return "1";
    }

    @Config(value = "arecibo.events.collector.numPartitionsList")
    public String getNumPartitionsToKeepList()
    {
        return "4";
    }

    @Config(value = "arecibo.events.collector.splitIntervalInMinutesList")
    public String getSplitIntervalInMinutesList()
    {
        return "30";
    }

    @Config(value = "arecibo.events.collector.numPartitionsToSplitAheadList")
    public String getNumPartitionsToSplitAheadList()
    {
        return "2";
    }

    @Config(value = "arecibo.rmi.port")
    public int getRmiPort()
    {
        return 48000;
    }

    @Config(value = "arecibo.events.collector.bufferWindowInSeconds")
    public int getBufferWindowInSeconds()
    {
        return 5;
    }

    @Config(value = "arecibo.events.collector.read_only_mode")
    public boolean isCollectorReadOnlyMode()
    {
        return false;
    }

    @Config(value = "arecibo.events.collector.hostUpdateInterval")
    public long getHostUpdateInterval()
    {
        return 300000;
    }

    @Config(value = "arecibo.events.collector.throttlePctFreeThreshold")
    public int getThrottlePctFreeThreshold()
    {
        return 10;
    }

    @Config(value = "arecibo.events.collector.service_name")
    public String getCollectorServiceName()
    {
        return "EventCollectorServer";
    }

    @Config(value = "arecibo.events.collector.tablespaceStatsUpdateIntervalMinutes")
    public int getTablespaceStatsUpdateIntervalMinutes()
    {
        return 5;
    }

    @Config(value = "arecibo.events.collector.maxTableSpaceMB")
    public int getMaxTableSpaceMB()
    {
        return 1;
    }

    @Config(value = "arecibo.events.collector.maxSplitAndSweepInitialDelayMinutes")
    public int getMaxSplitAndSweepInitialDelayMinutes()
    {
        return 30;
    }

    @Config(value = "arecibo.events.collector.maxTriageThreads")
    public int getMaxTriageThreads()
    {
        return 1;
    }

    @Config(value = "arecibo.events.collector.maxBatchInsertThreads")
    public int getMaxBatchInsertThreads()
    {
        return 1;
    }

    @Config(value = "arecibo.events.collector.minBatchInsertSize")
    public int getMinBatchInsertSize()
    {
        return 0;
    }

    @Config(value = "arecibo.events.collector.maxBatchInsertSize")
    public int getMaxBatchInsertSize()
    {
        return 1000;
    }

    @Config(value = "arecibo.events.collector.maxAsynchTriageQueueSize")
    public int getMaxAsynchTriageQueueSize()
    {
        return 10;
    }

    @Config(value = "arecibo.events.collector.maxAsynchInsertQueueSize")
    public int getMaxAsynchInsertQueueSize()
    {
        return 100;
    }

    @Config(value = "arecibo.events.collector.maxPendingEvents")
    public int getMaxPendingEvents()
    {
        return 50000;
    }

    @Config(value = "arecibo.events.collector.maxPendingEventsCheckIntervalMs")
    public long getMaxPendingEventsCheckIntervalMs()
    {
        return 10000L;
    }

    @Config(value = "arecibo.events.collector.enableBatchRetryOnIntegrityViolation")
    public boolean getEnableBatchRetryOnIntegrityViolation()
    {
        return false;
    }

    @Config(value = "arecibo.events.collector.enableDuplicateEventLogging")
    public boolean getEnableDuplicateEventLogging()
    {
        return false;
    }

    @Config(value = "arecibo.events.collector.enablePerTableInserts")
    public boolean getEnablePerTableInserts()
    {
        return false;
    }

    @Config(value = "arecibo.events.collector.enablePreparedBatchInserts")
    public boolean getEnablePreparedBatchInserts()
    {
        return false;
    }

    @Config(value = "arecibo.events.collector.db.type")
    public String getDBType()
    {
        // config-magic doesn't support enums :'(
        return "MYSQL";
    }

    @Config(value = "arecibo.events.collector.db.url")
    public String getJdbcUrl()
    {
        return "jdbc:mysql://localhost/arecibo";
    }

    @Config(value = "arecibo.events.collector.db.user")
    public String getDBUsername()
    {
        return "arecibo";
    }

    @Config(value = "arecibo.events.collector.db.password")
    public String getDBPassword()
    {
        return "arecibo";
    }

    @Config(value = "arecibo.events.collector.tablespace")
    public String getTableSpaceName()
    {
        // This is the schema for MySQL
        return "arecibo";
    }

    @Config(value = "arecibo.events.collector.db.minIdleConnections")
    public int getMinConnectionsPerPartition()
    {
        return 1;
    }

    @Config(value = "arecibo.events.collector.db.maxActiveConnections")
    public int getMaxConnectionsPerPartition()
    {
        return 50;
    }
}
