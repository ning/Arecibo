/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import com.google.inject.Inject;
import com.ning.arecibo.event.publisher.EventPublisherConfig;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.arecibo.util.service.ServiceNotAvailableException;
import com.ning.arecibo.util.service.ServiceSelector;

public class AggregatorService
{
    private final Selector selector;
	private final ServiceLocator serviceLocator;

	@Inject
	public AggregatorService(ServiceLocator serviceLocator,
                             EventPublisherConfig eventPublisherConfig)
	{
		this.serviceLocator = serviceLocator;
		this.selector = new ServiceSelector(eventPublisherConfig.getEventServiceName());
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
