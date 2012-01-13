package com.ning.arecibo.aggregator.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.mogwee.executors.NamedThreadFactory;
import org.weakref.jmx.Managed;
import com.google.inject.Inject;
import com.ning.arecibo.aggregator.guice.AggregatorConfig;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;

public class AsynchronousUpdateWorker
{
	private static final Logger log = Logger.getLogger(AsynchronousUpdateWorker.class);

	final ThreadPoolExecutor executor;
    final ArrayBlockingQueue<Runnable> asyncDispatchQueue;
    final AtomicLong discardedTasksDueToOverflow = new AtomicLong(0L);

    @Inject
	public AsynchronousUpdateWorker(AggregatorConfig config)
	{
        this.asyncDispatchQueue = new ArrayBlockingQueue<Runnable>(config.getAsyncUpdateWorkerBufferSize());

		executor = new ThreadPoolExecutor(config.getAsyncUpdateWorkerNumThreads(),
		                                  config.getAsyncUpdateWorkerNumThreads(),
		                                  60L, TimeUnit.SECONDS,
		                                  this.asyncDispatchQueue,
		                                  new NamedThreadFactory(getClass().getSimpleName()),
		                                  new RejectedExecutionHandler() {
                                              @Override
                                              public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                                                  log.warn("AsynchronousUpdateWorker queue full, discarding task");
                                                  discardedTasksDueToOverflow.getAndIncrement();
                                              }
                                          });
	}

    public void executeLater(final Runnable run)
	{
		executor.execute(new Runnable()
		{
			public void run()
			{
				try {
					run.run();
				}
				catch (Exception e) {
                    /*
                    ** quiet down logging in this case
                    ** log.error(e);
                    */
					log.warn(e.getMessage());
				}
			}
		});
	}

	@MonitorableManaged(monitored = true)
	public int getQueueSize()
	{
		return executor.getQueue().size();
	}

	@MonitorableManaged(monitored = true)
	public int getActiveCount()
	{
		return executor.getActiveCount();
	}

	@Managed
	public int getPoolSize()
	{
		return executor.getPoolSize();
	}

	@Managed
	public long getTaskCount()
	{
		return executor.getTaskCount();
	}

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
    public long getCompletedTaskCount()
    {
        return executor.getCompletedTaskCount();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
	public long getDiscardedTasksDueToOverflow()
	{
		return discardedTasksDueToOverflow.get();
	}

	@Managed
	public int getMaximumPoolSize()
	{
		return executor.getMaximumPoolSize();
	}

	@Managed
	public void setMaximumPoolSize(int maximumPoolSize)
	{
		executor.setMaximumPoolSize(maximumPoolSize);
	}

	@Managed
	public int getCorePoolSize()
	{
		return executor.getCorePoolSize();
	}

	@Managed
	public void setCorePoolSize(int corePoolSize)
	{
		executor.setCorePoolSize(corePoolSize);
	}
}
