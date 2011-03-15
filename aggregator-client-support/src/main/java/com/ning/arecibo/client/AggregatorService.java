package com.ning.arecibo.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.ning.arecibo.event.publisher.EventServiceName;
import com.ning.arecibo.event.transport.EventService;

import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.arecibo.util.service.ServiceNotAvailableException;

public class AggregatorService
{
    private final Selector selector;
	private final ServiceLocator serviceLocator;

	@Inject
	public AggregatorService(ServiceLocator serviceLocator,
                             final @EventServiceName String eventServiceName)
	{
		this.serviceLocator = serviceLocator;
		this.selector = new Selector() {
			public boolean match(ServiceDescriptor sd)
			{
				return sd.getName().equals(eventServiceName);
			}
		};
		this.serviceLocator.startReadOnly();
	}

	public RemoteAggregatorService getAggregatorService() throws ServiceNotAvailableException
	{
		ServiceDescriptor sd = serviceLocator.selectServiceAtRandom(selector);
		return getRemoteObject(sd, new LookupCallback<RemoteAggregatorService>()
		{
			public RemoteAggregatorService lookup(Registry registry) throws NotBoundException, RemoteException
			{
				return (RemoteAggregatorService) registry.lookup(RemoteAggregatorService.class.getSimpleName());
			}
		});
	}

	public static RemoteAggregatorService getAggregatorService(String host, int port) throws ServiceNotAvailableException
	{
		Map<String,String> prop = new HashMap<String,String>();
		prop.put(EventService.RMI_PORT, String.valueOf(port));
		prop.put(EventService.HOST, host);
		ServiceDescriptor sd = new ServiceDescriptor("", prop);
		return getRemoteObject(sd, new LookupCallback<RemoteAggregatorService>()
		{
			public RemoteAggregatorService lookup(Registry registry) throws NotBoundException, RemoteException
			{
				return (RemoteAggregatorService) registry.lookup(RemoteAggregatorService.class.getSimpleName());
			}
		});
	}


	public static <T> T getRemoteObject(ServiceDescriptor sd, LookupCallback<T> callback) throws ServiceNotAvailableException
	{
		try {
			int port = Integer.parseInt(sd.getProperties().get(EventService.RMI_PORT));
			String host = sd.getProperties().get(EventService.HOST);
			Registry registry = LocateRegistry.getRegistry(host, port);
			return callback.lookup(registry);
		}
		catch (Exception e) {
			throw new ServiceNotAvailableException();
		}
	}

	public static interface LookupCallback<T>
	{
		public T lookup(Registry registry) throws NotBoundException, RemoteException;
	}

}
