package com.ning.arecibo.util.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class RMIRegistryProvider implements Provider<Registry>
{
	private final RMIRegistryConfig config;

	@Inject
	public RMIRegistryProvider(RMIRegistryConfig config)
	{
		this.config = config;
	}

	public Registry get()
	{
		try {
			return LocateRegistry.createRegistry(config.getPort());
		}
		catch (RemoteException e) {
			return null;
		}
	}
}
