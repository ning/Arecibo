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

package com.ning.arecibo.util.cron;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.ning.arecibo.util.Logger;

public class FixedDelayCronJob extends CronJob
{
    private static final boolean INTERRUPT = true;
    private static final boolean DO_NOT_INTERRUPT = false;

    private final Logger log;
    private final AtomicBoolean nextIsScheduled;
    private final AtomicBoolean running;
    private final CronPeon peon;

    private volatile ScheduledFuture<?> currentRunningJob;
    private volatile Long lastPeriodicSuccessMillis = null;

    public FixedDelayCronJob(final Runnable baseJob,
                             final long runInterval,
                             final TimeUnit intervalUnits,
                             final ScheduledExecutorService executor,
                             final String objectName,
                             final Logger log)
    {
        super(baseJob, runInterval, intervalUnits, executor, objectName, log);

        this.nextIsScheduled = new AtomicBoolean(false);
        this.running = new AtomicBoolean(false);
        this.peon = new CronPeon();
        this.log = getLogger();
    }

    @Override
    public boolean isRunning()
    {
        return running.get();
    }

    @Override
    public Long getLastPeriodicSuccessMillis()
    {
        return lastPeriodicSuccessMillis;
    }

    @Override
    protected void scheduleNext()
    {
        synchronized (nextIsScheduled) {
            if (!nextIsScheduled.get() ) {  // Make sure we don't already have one scheduled
                currentRunningJob = getExecutor().schedule(peon, getRunInterval(), getIntervalUnits());

                log.debug("Job name[%s] Scheduling next update for [%s %s] from now.", getJobName(), getRunInterval(), getIntervalUnits().toString());
                nextIsScheduled.set(true);
            }
        }
    }

    @Override
    protected void reschedule()
    {
        synchronized (nextIsScheduled) {
            nextIsScheduled.set(false);
            if ( isActive() ) {
                scheduleNext();
            }
        }
    }

    @Override
    protected boolean cancelCurrentRunningJob()
    {
        synchronized (nextIsScheduled) {
            if (currentRunningJob != null) {
                nextIsScheduled.set(false);
                return this.currentRunningJob.cancel(DO_NOT_INTERRUPT);
            }
            return false;
        }
    }

    private class CronPeon implements Runnable
    {
        public void run()
        {
            log.debug("CronPeon[%s] starting up.", getJobName());
            try {
                if (isActive()) {
                    running.set(true);
                    getBaseJob().run();
                    lastPeriodicSuccessMillis = System.currentTimeMillis();
                }
                log.debug("CronPeon[%s] done with update", getJobName());
            }
            catch (Throwable t) {
                log.warn(t, "CronPeon[%s] caught a throwable.  This is probably not a good thing.  However, I will reschedule.  Message was [%s]",
                         getJobName(), t.getMessage());
            }
            finally {
                running.set(false);
                reschedule();
            }
        }
    }
}