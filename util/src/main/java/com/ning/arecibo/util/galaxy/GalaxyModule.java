package com.ning.arecibo.util.galaxy;

import org.skife.config.ConfigurationObjectFactory;
import com.google.inject.AbstractModule;

public class GalaxyModule extends AbstractModule
{
    @Override
	public void configure()
	{
        GalaxyConfig config = new ConfigurationObjectFactory(System.getProperties()).build(GalaxyConfig.class);

        bind(GalaxyConfig.class).toInstance(config);
	}
}
