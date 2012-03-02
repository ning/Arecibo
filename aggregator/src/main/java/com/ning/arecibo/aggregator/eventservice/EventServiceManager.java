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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import com.google.inject.Inject;
import com.mogwee.executors.NamedThreadFactory;
import com.ning.arecibo.event.publisher.AsynchronousSender;
import com.ning.arecibo.event.publisher.EventPublisherConfig;
import com.ning.arecibo.event.publisher.EventSenderType;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.event.transport.EventServiceRESTClient;
import com.ning.arecibo.event.transport.EventServiceUDPClient;
import com.ning.arecibo.event.transport.JavaEventSerializer;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.http.client.AsyncHttpClient;

public class EventServiceManager
{
	private final ServiceLocator serviceLocator;
	private final EventServiceRESTClient restClient;
	private final EventServiceUDPClient udpClient;
	private final AsynchronousSender asyncSender;
	private final ExecutorService enqueueThread = Executors.newSingleThreadExecutor(new NamedThreadFactory(EventServiceManager.class.getSimpleName()+":enqueueThread"));
	private final ExecutorService clusterThread = Executors.newSingleThreadExecutor(new NamedThreadFactory(EventServiceManager.class.getSimpleName()+":clusterThread"));
	private final ConcurrentHashMap<String, ExternalPublisherEventService> externalPublisherEventServices = new ConcurrentHashMap<String, ExternalPublisherEventService>();
    private final AtomicLong externalEventsDelivered = new AtomicLong();
    private final AtomicLong externalEventsFailed = new AtomicLong();

    @Inject
	public EventServiceManager(EventPublisherConfig config,
	                           ServiceLocator serviceLocator,
                               AsyncHttpClient httpClient,
	                           @EventSenderType String senderType) throws IOException
    {
		this.serviceLocator = serviceLocator;
		this.restClient = new EventServiceRESTClient(httpClient, new JavaEventSerializer(), senderType);
		this.udpClient = new EventServiceUDPClient(new JavaEventSerializer(), senderType);
		this.asyncSender = new AsynchronousSender(config.getMaxEventDispatchers(),
		                                          config.getMaxEventBufferSize(),
		                                          config.getMaxDrainDelay());
	}

	public void start()
	{
	}

	public void stop()
	{
		this.asyncSender.shutdown();
	}

	public EventService createEventService(String host, int restPort, int udpPort)
	{
		return new SimpleEventService(host, restPort, udpPort, restClient, udpClient, this);
	}

	public EventService createEventService(String serviceName)
	{
		String key = serviceName ;
		if (!externalPublisherEventServices.containsKey(key)) {
			ExternalPublisherEventService service = new ExternalPublisherEventService(serviceLocator, serviceName, restClient, udpClient, this);
			ExternalPublisherEventService old = externalPublisherEventServices.putIfAbsent(key, service);
			if ( old == null ) {
				service.start();
			}
		}
		return externalPublisherEventServices.get(key);
	}

	public AsynchronousSender getAsyncSender()
	{
		return asyncSender;
	}

	public ExecutorService getEnqueueThread()
	{
		return enqueueThread;
	}

	public ExecutorService getClusterThread()
	{
		return clusterThread;
	}

    public void updateExternalEventsDelivered(long update) {
        externalEventsDelivered.getAndAdd(update);
    }

    public void updateExternalEventsFailed(long update) {
        externalEventsFailed.getAndAdd(update);
    }

    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
    public long getExternalEventsDelivered() {
        return externalEventsDelivered.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER }) 
    public long getExternalEventsFailed() {
        return externalEventsFailed.get();
    }
}
