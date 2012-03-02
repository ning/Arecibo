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

package com.ning.arecibo.event.publisher;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.inject.Inject;
import com.mogwee.executors.FailsafeScheduledExecutor;
import com.mogwee.executors.NamedThreadFactory;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.event.transport.EventServiceRESTClient;
import com.ning.arecibo.event.transport.JavaEventSerializer;
import com.ning.arecibo.util.service.RandomSelector;
import com.ning.arecibo.util.service.RandomServiceChooser;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.http.client.AsyncHttpClient;

public class RandomEventServiceChooser implements EventServiceChooser
{
    private final EventPublisherConfig config;
	private final ServiceLocator serviceLocator;
	private final EventServiceRESTClient restClient;
	private final Selector selector;
	private final RandomServiceChooser chooser;
	private final AtomicInteger serviceUseCount = new AtomicInteger(0);
	private final ExecutorService executor = new FailsafeScheduledExecutor(10, "EventServiceChooser");
	private volatile EventService eventService;

	@Inject
	public RandomEventServiceChooser(EventPublisherConfig config,
	                                 ServiceLocator serviceLocator,
	                                 @RandomSelector Selector selector,
	                                 RandomServiceChooser chooser,
	                                 @EventSenderType String senderType,
	                                 AsyncHttpClient httpClient)
	{
	    this.config = config;
		this.serviceLocator = serviceLocator;
		this.selector = selector;
		this.chooser = chooser;
		this.serviceLocator.startReadOnly();
		this.restClient = new EventServiceRESTClient(httpClient, new JavaEventSerializer(), senderType);
	}

	@Override
	public void start()
	{
		serviceLocator.registerListener(selector, executor, this);
	}

	@Override
	public void stop()
	{
		executor.shutdown();
	}

	@Override
	public EventService choose(UUID uuid) throws IOException
	{
		if (eventService == null || serviceUseCount.getAndIncrement() > config.getRandomSelectorMaxUse()) {
			ServiceDescriptor sd = chooser.getResponsibleService(uuid.toString());

			if (sd == null) {
				throw new IOException("No hdfs-collector server available!");
			}

			eventService = new RESTEventService(this, sd, restClient);
		}

		return eventService;
	}

	@Override
	public void invalidate(UUID uuid)
	{
		serviceUseCount.set(config.getRandomSelectorMaxUse());
	}

	@Override
	public void onAdd(ServiceDescriptor sd)
	{
	}

	@Override
	public void onRemove(ServiceDescriptor sd)
	{
	}
}
