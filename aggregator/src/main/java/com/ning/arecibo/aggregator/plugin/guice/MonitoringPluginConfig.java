package com.ning.arecibo.aggregator.plugin.guice;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.TimeSpan;

public interface MonitoringPluginConfig
{
    @Config("arecibo.event.receiver.serviceName")
    String[] getReceiverServiceNames();

    @Config("arecibo.aggregator.baseLevel.timeWindowSeconds")
    @Default("150s")
    TimeSpan getBaseLevelTimeWindow();

    @Config("arecibo.aggregator.baseLevel.batchIntervalSeconds")
    @Default("60s")
    TimeSpan getBaseLevelBatchInterval();

    // see also com.ning.arecibo.collector.guice.CollectorConfig
    @Config("arecibo.events.collector.reductionFactors")
    @Default("1")
    int[] getReductionFactors();
}
