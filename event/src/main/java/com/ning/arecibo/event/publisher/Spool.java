package com.ning.arecibo.event.publisher;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.skife.config.TimeSpan;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.Pair;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceListener;
import com.ning.arecibo.util.service.ServiceLocator;

public abstract class Spool implements ServiceListener
{
	private static final Logger log = Logger.getLogger(Spool.class);

	private final File spoolDir;
	private final File retryDir;
	private BDBQueue<EventItem> spoolQueue;
	private BDBQueue<EventItem> retryQueue;
	private final ExecutorService globalExecutor;
	private final TimeSpan spoolExpiration;
	private final String clusterName;
	private final ServiceLocator serviceLocator;
	private final AtomicInteger serviceCount = new AtomicInteger(0);
	private final AtomicBoolean isRunning = new AtomicBoolean(false), hasCluster = new AtomicBoolean(false);
	private static final int MAX_RETRIES = 25;
	private final LinkedBlockingQueue<EventItem> enqueue = new LinkedBlockingQueue<EventItem>();
	private KeyedDispatcher keyedExecutor;

	public int getSpoolQueueSize()
	{
		return spoolQueue.getNumQueued();
	}

	public int getRetryQueueSize()
	{
		return retryQueue.getNumQueued();
	}

	public Spool(String clusterName,
	             File spoolRoot,
	             ServiceLocator serviceLocator,
	             Selector selector,
	             ExecutorService globalExecutor,
	             TimeSpan spoolExpiration)
	{
		this.clusterName = clusterName;
		this.serviceLocator = serviceLocator;
		this.globalExecutor = globalExecutor;
		this.keyedExecutor = new KeyedDispatcher();
		this.spoolExpiration = spoolExpiration;

		spoolDir = new File(spoolRoot, "event");
		retryDir = new File(spoolRoot, "retry");

		this.spoolQueue = new BDBQueue<EventItem>(spoolDir, "event", 1000);
		this.retryQueue = new BDBQueue<EventItem>(retryDir, "retry", 1000);

		this.serviceLocator.registerListener(selector, Executors.newSingleThreadExecutor(), this);
	}

	public void start() throws IOException
	{
		this.isRunning.set(true);
		log.info("retryQueue size = %d", retryQueue.getNumQueued());
		log.info("spoolQueue size = %d", spoolQueue.getNumQueued());
	}

	public void stop(long l, TimeUnit timeUnit) throws IOException, InterruptedException
	{
		this.retryQueue.stop();
		this.spoolQueue.stop();
		this.isRunning.set(false);
	}


	public void onRemove(ServiceDescriptor sd)
	{
		log.info("%s removed", sd);
		serviceCount.decrementAndGet();
		if (serviceCount.get() == 0) {
			log.info("%s : cluster %s is offline", this, clusterName);
			hasCluster.set(false);
		}
	}

	public void onAdd(ServiceDescriptor sd)
	{
		log.info("%s added", sd);
		serviceCount.incrementAndGet();
		if (serviceCount.get() == 1) {
			log.info("%s : cluster %s is online", this, clusterName);
			hasCluster.set(true);
			keyedExecutor.execute(new SpoolRunnable());
			keyedExecutor.execute(new RetryRunnable());
		}
	}

	public boolean hasCluster()
	{
		return hasCluster.get();
	}

	public void retry(final Event event)
	{
		enqueueRetry(new EventItem(event).retry());
	}

	public void spool(final Event event)
	{
		try {
			enqueue.put(new EventItem(event));
			keyedExecutor.execute(new EnqueueRunnable());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void enqueueRetry(final EventItem item)
	{
		try {
			enqueue.put(item.retry());
			keyedExecutor.execute(new EnqueueRunnable());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}


	private class RetryRunnable implements Runnable
	{
		public void run()
		{
			log.info("%s start", this);
			while (hasCluster.get() && retryQueue.getNumQueued() > 0) {
				log.debug("%s dequeuing from retryQueue", Thread.currentThread().getName());
				try {
					Pair<Integer, EventItem> p = retryQueue.get();
					p.getSecond().touch();
					log.debug("%s processing %s", Thread.currentThread().getName(), p.getSecond());
					retryQueue.remove(p.getFirst());

					Event evt = p.getSecond().getEvent();
					if (p.getSecond().getCount() < MAX_RETRIES) {
						try {
							sendViaREST(evt);
						}
						catch (IOException e) {
							log.error(e, "retry failed, retry count = %d", p.getSecond().getCount());
							enqueueRetry(p.getSecond());
						}
					}
					else {
						log.error("max retried reached, dumping event %s", evt);
					}

				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			log.info("%s end", this);			
		}
	}

	abstract protected void sendViaREST(Event evt) throws IOException;


	private class SpoolRunnable implements Runnable
	{
		public void run()
		{
			log.info("%s start", this);
			while (hasCluster.get() && spoolQueue.getNumQueued() > 0) {
				log.debug("%s dequeuing from spoolQueue", Thread.currentThread().getName());
				try {
					Pair<Integer, EventItem> p = spoolQueue.get();
					p.getSecond().touch();
					log.debug("%s processing %s", Thread.currentThread().getName(), p.getSecond());
					spoolQueue.remove(p.getFirst());

					Event evt = p.getSecond().getEvent();
					if (p.getSecond().getTimestamp() + spoolExpiration.getMillis() > System.currentTimeMillis()) {
						try {
							sendViaREST(evt);
						}
						catch (IOException e) {
							log.error(e, "retry failed, retry count = %d", p.getSecond().getCount());
							enqueueRetry(p.getSecond());
						}
					}
					else {
						log.error("event expired, %s", evt);
					}

				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			log.info("%s end", this);
		}
	}


	private static class EventItem
	{
		public Event event;
		public long touchedOn;
		public int count = 0;
		private boolean spool = true;

		public EventItem(Event event)
		{
			this.event = event;
			touch();
		}

		public EventItem retry()
		{
			spool = false;
			return this;
		}

		public void touch()
		{
			this.touchedOn = System.currentTimeMillis();
			count++;
		}

		public int getCount()
		{
			return count;
		}

		public Event getEvent()
		{
			return event;
		}

		public long getTimestamp()
		{
			return touchedOn;
		}

		public boolean isSpool()
		{
			return spool;
		}
	}

	private class EnqueueRunnable implements Runnable
	{
		public void run()
		{
			int pollCountDown = 10;
			while (!enqueue.isEmpty() || pollCountDown > 0) {
				try {
					EventItem item = enqueue.poll(15, TimeUnit.SECONDS);
					if (item != null) {
						pollCountDown = 10;
						if (item.isSpool()) {
							spoolQueue.put(item);
							if ( hasCluster.get() ) {
								keyedExecutor.execute(new SpoolRunnable());
							}
						}
						else {
							retryQueue.put(item);
							if ( hasCluster.get() ) {
								keyedExecutor.execute(new RetryRunnable());								
							}
						}
					}
					else {
						pollCountDown--;
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private class KeyedDispatcher
	{
		ArrayBlockingQueue<Runnable> spool = new ArrayBlockingQueue<Runnable>(1);
		ArrayBlockingQueue<Runnable> enqueue = new ArrayBlockingQueue<Runnable>(1);
		ArrayBlockingQueue<Runnable> retry = new ArrayBlockingQueue<Runnable>(1);
		AtomicBoolean spoolRunning = new AtomicBoolean(false);
		AtomicBoolean enqueueRunning = new AtomicBoolean(false);
		AtomicBoolean retryRunning = new AtomicBoolean(false);

		public void execute(final Runnable r)
		{
			if (r instanceof SpoolRunnable) {
				offer(spool, spoolRunning, r);
			}
			else if (r instanceof RetryRunnable) {
				offer(retry, retryRunning, r);
			}
			else if (r instanceof EnqueueRunnable) {
				offer(enqueue, enqueueRunning, r);
			}
		}

		private void offer(final ArrayBlockingQueue<Runnable> q, final AtomicBoolean b, Runnable r)
		{
			synchronized(b) {
				if (q.offer(r) && !b.get()) {
					globalExecutor.execute(new Runnable()
					{
						public void run()
						{
							synchronized(b) {
								if ( !b.get() ) {
									b.set(true);
								}
								else {
									return;
								}
							}
							try {
								while (!q.isEmpty()) {
									Runnable theRunnable = q.take();
									if ( theRunnable != null ) {
										log.info("running %s", theRunnable);
										theRunnable.run();
									}
								}
							}
							catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
							synchronized(b) {
								b.set(false);
							}
						}
					});
				}
			}
		}
	}
}
