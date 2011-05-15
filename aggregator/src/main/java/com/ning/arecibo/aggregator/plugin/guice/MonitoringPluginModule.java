package com.ning.arecibo.aggregator.plugin.guice;

import org.skife.config.ConfigurationObjectFactory;
import com.google.inject.AbstractModule;

public class MonitoringPluginModule extends AbstractModule
{
    @Override
    public void configure()
    {
        MonitoringPluginConfig config = new ConfigurationObjectFactory(System.getProperties()).build(MonitoringPluginConfig.class);

        bind(MonitoringPluginConfig.class).toInstance(config);
    }
}
