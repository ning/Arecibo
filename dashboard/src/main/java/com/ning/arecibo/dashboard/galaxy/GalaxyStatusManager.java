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

package com.ning.arecibo.dashboard.galaxy;

import com.google.inject.Inject;
import com.ning.arecibo.dashboard.guice.DashboardConfig;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.galaxy.GalaxyCorePicker;
import com.ning.arecibo.util.galaxy.GalaxyCoreStatus;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GalaxyStatusManager implements Runnable
{
    private static final Logger log = Logger.getLogger(GalaxyStatusManager.class);

    private final DashboardConfig config;
    private final GalaxyCorePicker corePicker;
    private final ConcurrentHashMap<String, GalaxyCoreStatus> coreStatusMap;

    private ScheduledThreadPoolExecutor executor;

    @Inject
    public GalaxyStatusManager(final DashboardConfig config, final GalaxyCorePicker corePicker)
    {
        this.config = config;
        this.corePicker = corePicker;
        this.coreStatusMap = new ConcurrentHashMap<String, GalaxyCoreStatus>();
    }

    public synchronized void start()
    {
        // one thread should be fine
        this.executor = new ScheduledThreadPoolExecutor(1);

        // start the config updater
        this.executor.scheduleWithFixedDelay(this, 0, config.getGalaxyUpdateInterval().getMillis(), TimeUnit.MILLISECONDS);
    }

    public synchronized void stop()
    {
        if (this.executor != null) {
            this.executor.shutdown();
            this.executor = null;
        }
    }

    public String getGlobalZone(final String hostName)
    {
        final GalaxyCoreStatus status = coreStatusMap.get(hostName);

        if (status == null) {
            return null;
        }

        return status.getGlobalZoneHostName();
    }

    public String getCoreType(final String hostName)
    {
        final GalaxyCoreStatus status = coreStatusMap.get(hostName);

        if (status == null) {
            return null;
        }

        return status.getCoreType();
    }

    public String getConfigPath(final String hostName)
    {
        final GalaxyCoreStatus status = coreStatusMap.get(hostName);

        if (status == null) {
            return null;
        }

        return status.getConfigPath();
    }

    public String getConfigSubPath(final String hostName)
    {
        final String configPath = getConfigPath(hostName);

        if (configPath == null) {
            return null;
        }

        // look for the 4th component in the path (actually 5th, the leading '/' results in empty string)
        final String[] parts = configPath.split("/");
        if (parts.length < 5) {
            return null;
        }
        else {
            return parts[4];
        }
    }

    public void run()
    {

        // update the current list of cores, in concurrently safe way
        // don't want to block access in anyway, it's ok if the coreStatusMap
        // temporarily contains stale hosts

        try {
            log.info("Updating the status list of available cores from galaxy");
            final List<GalaxyCoreStatus> statii = corePicker.getCores();

            if (statii == null) {
                log.info("Retrieved no available cores from galaxy");
                coreStatusMap.clear();
            }
            else {
                log.info("Retrieved list of " + statii.size() + " available cores from galaxy");

                // build a keyed list of cores
                final Set<String> newKeys = new HashSet<String>();
                for (final GalaxyCoreStatus status : statii) {
                    newKeys.add(status.getZoneHostName());
                }

                // get list of prev keys
                final Set<String> prevKeys = coreStatusMap.keySet();
                final Iterator<String> prevKeyIter = prevKeys.iterator();

                // throw out any that are no longer apparently active
                while (prevKeyIter.hasNext()) {
                    final String prevKey = prevKeyIter.next();
                    if (!newKeys.contains(prevKey)) {
                        prevKeyIter.remove();
                    }
                }

                // now add all the new core statii in
                for (final GalaxyCoreStatus status : statii) {
                    coreStatusMap.put(status.getZoneHostName(), status);
                }
            }
        }
        catch (IOException ioEx) {
            log.warn(ioEx, "Got IOException retrieving galaxy core data");
        }
        catch (RuntimeException ruEx) {
            log.warn(ruEx, "Got RuntimeException retrieving galaxy core data");
        }
    }
}
