package com.ning.arecibo.util.rmi;

import java.rmi.registry.Registry;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;

public class RMIModule implements Module
{
	public void configure(Binder binder)
	{
		binder.bind(Registry.class).toProvider(RMIRegistryProvider.class).asEagerSingleton();
		binder.bindConstant().annotatedWith(Names.named("RMIRegistryPort")).to(System.getProperty("arecibo.rmi.port", "auto"));
	}
}
