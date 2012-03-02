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

import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.weakref.jmx.MBeanExporter;
import org.weakref.jmx.Managed;
import com.google.inject.Inject;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;

/**
 * This class encapsulates a cron-style executor.
 *
 * Use this to schedule cron-style jobs with full run-time control.  That includes the ability to
 *   1) Adjust the time period while the process is running
 *   2) Stop the job and keep the process running
 *   3) Start the job at any time
 *   4) Run the job at will
 */
public class JMXCronTaskMaster
{
    private final ScheduledExecutorService executorService;
    private final MBeanExporter exporter;

    @Inject
    public JMXCronTaskMaster(@JMXCronScheduler ScheduledExecutorService executorService,
                             MBeanExporter exporter)
    {
        this.executorService = executorService;
        this.exporter = exporter;
    }

    public CronJob createFixedDelayCronJob(long runInterval, TimeUnit intervalUnits, String jobName, Runnable theJob)
    {
        return createFixedDelayCronJob(runInterval, intervalUnits, jobName, theJob, Logger.getCallersLoggerViaExpensiveMagic());
    }

    public CronJob createFixedDelayCronJob(long runInterval, TimeUnit intervalUnits, String jobName, Runnable theJob, Logger log)
    {
        return new FixedDelayCronJob(theJob, runInterval, intervalUnits, executorService, jobName, log);
    }

    public CronJob createFixedRateCronJob(long runInterval, TimeUnit intervalUnits, String jobName, Runnable theJob)
    {
        return createFixedRateCronJob(runInterval, intervalUnits, jobName, theJob, Logger.getCallersLoggerViaExpensiveMagic());
    }

    public CronJob createFixedRateCronJob(long runInterval, TimeUnit intervalUnits, String jobName, Runnable theJob, Logger log)
    {
        return new FixedDelayCronJob(theJob, runInterval, intervalUnits, executorService, jobName, log);
    }

    /**
     * Registers a cron job, but does not start it.  Call the start() method on the returned object in order to start the cron job.
     *
     * @param runInterval default run interval of the cron job, this value is adjustable via JMX
     * @param intervalUnits default run interval time units of the cron job, this value is adjustable via JMX
     * @param jmxObjectName ObjectName-conformant string that will be used to export the JMX instrumentation
     * @param theJob the Runnable that this job represents
     * @return the JMXCronMiddleManagement object that represents the is responsible for the individual cron job.
     */
    public CronJob exportNewFixedDelayCronJob(long runInterval, TimeUnit intervalUnits, String jmxObjectName, Runnable theJob)
    {
        return exportCronJob(createFixedDelayCronJob(runInterval, intervalUnits, jmxObjectName, theJob, Logger.getCallersLoggerViaExpensiveMagic()),
                             jmxObjectName);
    }

    public CronJob exportNewFixedRateCronJob(long runInterval, TimeUnit intervalUnits, String jmxObjectName, Runnable theJob)
    {
        return exportCronJob(createFixedRateCronJob(runInterval, intervalUnits, jmxObjectName, theJob, Logger.getCallersLoggerViaExpensiveMagic()),
                             jmxObjectName);
    }

    public CronJob exportCronJob(CronJob job, String jmxObjectName)
    {
        exporter.export(jmxObjectName, new JMXExportableCronJobWrapper(job));
        return job;
    }

    public static class JMXExportableCronJobWrapper
    {
        private static final Logger log = Logger.getLoggerViaExpensiveMagic();

        private final CronJob job;

        public JMXExportableCronJobWrapper(CronJob job)
        {
            this.job = job;
        }

        @Managed
        public boolean isActive()
        {
            return job.isActive();
        }

        @Managed
        public void setActive(final boolean activate)
        {
            job.setActive(activate);
        }

        @Managed
        public boolean isRunning()
        {
            return job.isRunning();
        }

        @Managed
        public long getRunInterval()
        {
            return job.getRunInterval();
        }

        @Managed
        public void setRunInterval(final long runInterval)
        {
            job.setRunInterval(runInterval);
        }

        @Managed
        public String getIntervalUnits()
        {
            return job.getIntervalUnits().toString();
        }

        @Managed
        public void setIntervalUnits(final String newTimeUnit)
        {
            if (newTimeUnit != null) {
                try {
                    job.setIntervalUnits(TimeUnit.valueOf(newTimeUnit.trim().toUpperCase()));
                }
                catch (Throwable t) {
                    log.info("Job name[%s]: Exception caught while trying to set new UpdateIntervalUnits[%s], keeping current value of [%s]",
                             job.getJobName(), newTimeUnit, job.getIntervalUnits().toString());
                }
            }

        }

        @MonitorableManaged(monitored = true)
        public Long getMillisSinceLastRun()
        {
            final Long runMillis = job.getLastRunMillis();

            if (runMillis == null) {
                return null;
            }
            else {
                return System.currentTimeMillis() - runMillis;
            }
        }

        @Managed
        public String getLastRunTimestamp()
        {
            final Long runMillis = job.getLastRunMillis();

            if (runMillis == null) {
                return null;
            }

            return new Date(runMillis).toString();
        }

        @Managed
        public String[] whatArePossibleIntervalUnits()
        {
            TimeUnit[] timeUnits = TimeUnit.values();
            String[] retVal = new String[timeUnits.length];
            for( int i = 0; i < timeUnits.length; ++i ) {
                retVal[i] = timeUnits[i].toString();
            }
            return retVal;
        }

        @Managed
        public void runOnceNow()
        {
            job.runOnceNow();
        }
    }

}
