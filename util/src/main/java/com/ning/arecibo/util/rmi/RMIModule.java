package com.ning.arecibo.util.rmi;

import java.rmi.registry.Registry;
import org.skife.config.ConfigurationObjectFactory;
import com.google.inject.AbstractModule;

public class RMIModule extends AbstractModule
{
    @Override
	public void configure()
	{
	    RMIRegistryConfig config = new ConfigurationObjectFactory(System.getProperties()).build(RMIRegistryConfig.class);

	    bind(RMIRegistryConfig.class).toInstance(config);
	    bind(Registry.class).toProvider(RMIRegistryProvider.class).asEagerSingleton();
	}
}
