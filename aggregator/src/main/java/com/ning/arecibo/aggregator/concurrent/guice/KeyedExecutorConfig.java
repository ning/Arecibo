package com.ning.arecibo.aggregator.concurrent.guice;

import org.skife.config.Config;
import org.skife.config.Default;

public interface KeyedExecutorConfig
{
    @Config("arecibo.events.aggregator.keyedExecutorNumThreads")
    @Default("100")
    int getNumThreads();
}
