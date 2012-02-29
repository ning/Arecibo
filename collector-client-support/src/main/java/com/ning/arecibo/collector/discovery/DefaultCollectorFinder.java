package com.ning.arecibo.collector.discovery;

import com.google.inject.Inject;
import com.ning.arecibo.collector.CollectorClientConfig;

public class DefaultCollectorFinder implements CollectorFinder
{
    private final CollectorClientConfig config;

    @Inject
    public DefaultCollectorFinder(final CollectorClientConfig config)
    {
        this.config = config;
    }

    @Override
    public String getCollectorUri()
    {
        return config.getCollectorUri();
    }
}
