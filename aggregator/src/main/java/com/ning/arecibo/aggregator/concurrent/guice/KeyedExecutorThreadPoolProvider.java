package com.ning.arecibo.aggregator.concurrent.guice;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class KeyedExecutorThreadPoolProvider implements Provider<ExecutorService>
{
    private final KeyedExecutorConfig config;

    @Inject
    public KeyedExecutorThreadPoolProvider(KeyedExecutorConfig config)
    {
        this.config = config;
    }

    public ExecutorService get()
	{
		return Executors.newFixedThreadPool(config.getNumThreads());
	}
}