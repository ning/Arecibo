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

package com.ning.arecibo.alert.manage;

import com.google.inject.Inject;
import com.mogwee.executors.NamedThreadFactory;
import com.ning.arecibo.alert.guice.AlertServiceConfig;
import com.ning.arecibo.alert.manage.AlertEventProcessor.AlertEventRunnableHandler;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import org.weakref.jmx.Managed;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AsynchronousEventHandler
{
    private static final Logger log = Logger.getLogger(AsynchronousEventHandler.class);

    private final AtomicLong eventsDiscarded = new AtomicLong(0L);

    private final ThreadPoolExecutor executor;
    private final ArrayBlockingQueue<Runnable> asynchEventQueue;

    @Inject
    public AsynchronousEventHandler(final AlertServiceConfig alertServiceConfig)
    {
        this.asynchEventQueue = new ArrayBlockingQueue<Runnable>(alertServiceConfig.getEventHandlerBufferSize());
        this.executor = new ThreadPoolExecutor(0, alertServiceConfig.getEventHandlerNumThreads(),
            60L, TimeUnit.SECONDS,
            this.asynchEventQueue,
            new NamedThreadFactory(getClass().getSimpleName()),
            new RejectedExecutionHandler()
            {

                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
                {
                    Event removedEvent = ((AlertEventRunnableHandler) r).getEvent();
                    log.warn("AsynchronousEventHandler queue full (%d), discarding event %s",
                        alertServiceConfig.getEventHandlerBufferSize(),
                        removedEvent.toString());
                    eventsDiscarded.getAndIncrement();
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
                    log.error(e);
                }
            }
        });
    }

    @MonitorableManaged(monitored = true)
    public int getQueueSize()
    {
        return executor.getQueue().size();
    }

    @Managed
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

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.RATE, MonitoringType.VALUE})
    public long getCompletedTaskCount()
    {
        return executor.getCompletedTaskCount();
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

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.RATE, MonitoringType.VALUE})
    public long getEventsDiscarded()
    {
        return eventsDiscarded.get();
    }
}
