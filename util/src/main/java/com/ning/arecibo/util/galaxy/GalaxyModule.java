package com.ning.arecibo.util.galaxy;

import com.google.inject.AbstractModule;
import org.skife.config.ConfigurationObjectFactory;

public class GalaxyModule extends AbstractModule
{
    @Override
    public void configure()
    {
        final GalaxyConfig config = new ConfigurationObjectFactory(System.getProperties()).build(GalaxyConfig.class);
        bind(GalaxyConfig.class).toInstance(config);
    }
}
