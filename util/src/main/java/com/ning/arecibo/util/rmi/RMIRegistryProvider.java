package com.ning.arecibo.util.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class RMIRegistryProvider implements Provider<Registry>
{
	private final int port;

	@Inject
	public RMIRegistryProvider(@Named("RMIRegistryPort") int port)
	{
		this.port = port;
	}

	public Registry get()
	{
		try {
			return LocateRegistry.createRegistry(port);
		}
		catch (RemoteException e) {
			return null;
		}
	}
}
