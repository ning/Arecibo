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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import com.ning.arecibo.util.Logger;

/**
 * A cron-style job.  In this case cron is simply used to mean that it happens
 * periodically.
 *
 * Provides a programmatic interface to adjust timing and other items on the
 * underlying runnable.
 */
public abstract class CronJob
{
    private final ScheduledExecutorService executor;
    private final Runnable baseJob;
    private final Logger log;
    private final String jobName;

    private final AtomicLong runInterval;
    private final AtomicReference<TimeUnit> intervalUnits;
    private final AtomicBoolean activated;

    private final AtomicLong manualUpdateCount = new AtomicLong(0);

    private volatile Long lastManualSuccessMillis = null;

    public CronJob(final Runnable baseJob,
                   final long runInterval,
                   final TimeUnit intervalUnits,
                   final ScheduledExecutorService executor,
                   final String jobName,
                   final Logger log)
    {
        this.baseJob = baseJob;
        this.runInterval = new AtomicLong(runInterval);
        this.intervalUnits = new AtomicReference<TimeUnit>(intervalUnits);
        this.activated = new AtomicBoolean(false);
        this.executor = executor;
        this.jobName = jobName;
        this.log = log;
    }

    /**
     * Getter for whether this job is active.  If active, it will be scheduled and rescheduled
     *
     * @return whether or not this job is active
     */
    public boolean isActive()
    {
        return activated.get();
    }

    /**
     * Sets whether this job is active.
     *
     * Setting to true from false will schedule the job to get run.
     *
     * Setting to false from true will cancel the job from running.  However, if the job is currently running (as opposed
     * to just scheduled to run), the current run will not be interrupted.
     * 
     * @param activate determines if the job should be active
     */
    public void setActive( boolean activate )
    {
        if ( this.activated.compareAndSet( !activate, activate ) ){
            if (activate) {
                log.info("Job name[%s]: Someone told me to START updating.", jobName);
                scheduleNext(); // Make sure I have a bun in the oven
            }
            else {
                log.info("Job name[%s]: Someone told me to STOP updating.", jobName);
                cancelCurrentRunningJob();
            }
        }
    }

    public long getRunInterval()
    {
        return runInterval.get();
    }

    public void setRunInterval( long runInterval )
    {
        log.info("Job name[%s]: Someone set my update interval to [%s].", jobName, runInterval);
        if ( runInterval >= 0 ) {
            this.runInterval.set(runInterval);
            if (cancelCurrentRunningJob()) {
                reschedule();
            }
        }
    }

    public TimeUnit getIntervalUnits()
    {
        return intervalUnits.get();
    }

    public void setIntervalUnits(TimeUnit newTimeUnit)
    {
        log.info("Someone told me to set my interval units to [%s]", newTimeUnit.toString());
        intervalUnits.set(newTimeUnit);
        if (cancelCurrentRunningJob()) {
            reschedule();
        }
    }

    public Long getLastRunMillis()
    {
        // store references to the values just ot make sure I'm always looking at the same thing.
        // Not entirely necessary but doesn't hurt.

        final Long lastPeriodicSuccessMillis = getLastPeriodicSuccessMillis();
        final Long myLastManualSuccessMillis = lastManualSuccessMillis;

        if (myLastManualSuccessMillis == null) {
            return lastPeriodicSuccessMillis;
        }
        else if ( lastPeriodicSuccessMillis == null) {
            return myLastManualSuccessMillis;
        }
        else {
            return Math.max(myLastManualSuccessMillis, lastPeriodicSuccessMillis);
        }
    }

    public void runOnceNow()
    {
        final long currCount = manualUpdateCount.incrementAndGet();
        log.info("Job name[%s]: Someone asked me to instigate an update, scheduling manual update number [%s] to run on next available thread.",
                 jobName, currCount);
        executor.execute(new Runnable()
        {
            public void run()
            {
                log.info("Job name[%s]: Manual update #%s: START", jobName, currCount);
                try {
                    baseJob.run();
                    lastManualSuccessMillis = System.currentTimeMillis();
                }
                catch (Throwable t) {
                    log.warn(t, "Job name[%s]: Manual invocation #%s caught a throwable.  This is probably not a good thing.  Message was [%s]",
                             getJobName(), currCount, t.getMessage());
                }
                log.info("Job name[%s]: Manual updated #%s: DONE", jobName, currCount);
            }
        });
    }

    public void start()
    {
        setActive(true);
    }

    public void stop()
    {
        setActive(false);
    }

    public abstract boolean isRunning();

    protected abstract void scheduleNext();
    protected abstract void reschedule();

    /**
     * Stops the job.  If the job is currently "running" (as opposed to just scheduled to run), this will block
     * until the current running job is complete.
     *
     * @return true if the job will not run again.  false if the job is currently running
     */
    protected abstract boolean cancelCurrentRunningJob();

    /**
     * Returns the milliseconds since the last time this has run.  Does not include time from manual runs.
     *
     * @return a Long representing the number of milliseconds that have passed since the previous periodic run.  Null if hasn't run.
     */
    protected abstract Long getLastPeriodicSuccessMillis();
    
    protected ScheduledExecutorService getExecutor()
    {
        return executor;
    }

    protected Runnable getBaseJob()
    {
        return baseJob;
    }

    protected Logger getLogger()
    {
        return log;
    }

    protected String getJobName()
    {
        return jobName;
    }
}
