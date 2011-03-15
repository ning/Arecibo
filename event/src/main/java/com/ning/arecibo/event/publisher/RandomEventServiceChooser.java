package com.ning.arecibo.event.publisher;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.inject.Inject;
import com.ning.arecibo.event.publisher.cluster.RandomSelectorMaxUse;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.event.transport.EventServiceRESTClient;
import com.ning.arecibo.event.transport.JavaEventSerializer;
import com.ning.arecibo.util.FailsafeScheduledExecutor;
import com.ning.arecibo.util.NamedThreadFactory;
import com.ning.arecibo.util.service.RandomSelector;
import com.ning.arecibo.util.service.RandomServiceChooser;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.http.client.AsyncHttpClient;

public class RandomEventServiceChooser implements EventServiceChooser
{
	private final ServiceLocator serviceLocator;
	private final EventServiceRESTClient restClient;
	private final Selector selector;
	private final RandomServiceChooser chooser;
	private final AtomicInteger serviceUseCount = new AtomicInteger(0);
	private final int maxServiceUseCount;
	private final ExecutorService executor = new FailsafeScheduledExecutor(10, new NamedThreadFactory("EventServiceChooser"));
	private volatile EventService eventService;

	@Inject
	public RandomEventServiceChooser(
		ServiceLocator serviceLocator,
		@RandomSelector Selector selector,
		RandomServiceChooser chooser,
		@EventSenderType String senderType,
		@RandomSelectorMaxUse int maxServiceUseCount,
		AsyncHttpClient httpClient
	)
	{
		this.serviceLocator = serviceLocator;
		this.selector = selector;
		this.chooser = chooser;
		this.maxServiceUseCount = maxServiceUseCount;
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
		if (eventService == null || serviceUseCount.getAndIncrement() > maxServiceUseCount) {
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
		serviceUseCount.set(maxServiceUseCount);
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
