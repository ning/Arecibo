package com.ning.arecibo.aggregator.guice;

import org.skife.config.Config;
import org.skife.config.Default;

public interface AggregatorConfig
{
    @Config("arecibo.events.aggregator.asyncUpdateWorkerBufferSize")
    @Default("1000")
    int getAsyncUpdateWorkerBufferSize();

    @Config("arecibo.events.aggregator.asynchUpdateWorkerNumThreads")
    @Default("25")
    int getAsyncUpdateWorkerNumThreads();
}
