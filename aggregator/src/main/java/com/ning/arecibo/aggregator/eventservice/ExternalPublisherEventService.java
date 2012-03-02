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

package com.ning.arecibo.aggregator.eventservice;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import com.ning.arecibo.event.transport.EventServiceRESTClient;
import com.ning.arecibo.event.transport.EventServiceUDPClient;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceListener;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.arecibo.util.service.ServiceSelector;

public class ExternalPublisherEventService extends AbstractEventService implements ServiceListener
{
	private static final Logger log = Logger.getLogger(ExternalPublisherEventService.class);
	private final ServiceLocator serviceLocator;
	private final String serviceName;
	private final EventServiceManager manager;
	private final AtomicInteger clusterUp = new AtomicInteger(0);
	private final Selector serviceSelector;

	protected ExternalPublisherEventService(ServiceLocator serviceLocator,
	                                        String serviceName,
	                                        EventServiceRESTClient restClient,
	                                        EventServiceUDPClient udpClient,
	                                        EventServiceManager manager)
	{
		super(restClient, udpClient, manager);
		this.serviceLocator = serviceLocator;
		this.serviceName = serviceName;
		this.manager = manager;
		this.serviceSelector = new ServiceSelector(serviceName);
	}

    void start()
	{
		log.info("registering listener for named cluster (%s)", serviceName);
		serviceLocator.registerListener(serviceSelector, manager.getClusterThread(), this);
	}

	void stop()
	{
		serviceLocator.unregisterListener(this);		
	}


	protected InetSocketAddress getRESTSocketAddress() throws IOException
	{
		return getInetSocketAddress(JETTY_PORT);
	}

	protected InetSocketAddress getUDPSocketAddress() throws IOException
	{
		return getInetSocketAddress(UDP_PORT);
	}

	private InetSocketAddress getInetSocketAddress(String name) throws IOException
	{
		try {
			ServiceDescriptor sd = serviceLocator.selectServiceAtRandom(serviceSelector);
			int port = Integer.parseInt(sd.getProperties().get(name));
			String host = sd.getProperties().get(HOST);
			return new InetSocketAddress(host, port);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public void sendREST(Event event) throws IOException
	{
		if (clusterUp.get() > 0) {
			super.sendREST(event);
		}
		else {
			log.debug("named cluster (%s) unavailable, discarding event", serviceName);
		}
	}

	public void sendUDP(Event event) throws IOException
	{
		if (clusterUp.get() > 0) {
			super.sendUDP(event);
		}
		else {
			log.debug("named cluster (%s) unavailable, discarding event", serviceName);
		}
	}

	public void onRemove(ServiceDescriptor serviceDescriptor)
	{
		int i = clusterUp.decrementAndGet();
		if ( i == 0 ) {
			log.info("named cluster (%s) is down", serviceName);
		}
	}

	public void onAdd(ServiceDescriptor serviceDescriptor)
	{
		int i = clusterUp.getAndIncrement();
		if ( i == 0 ) {
			log.info("named cluster (%s) is up", serviceName);
		}
	}
}
