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

package com.ning.arecibo.dashboard.healthchecks;

import com.google.inject.Inject;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;
import com.yammer.metrics.core.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GalaxyStatusManagerHealthCheck extends HealthCheck
{
    private static final String NAME = GalaxyStatusManagerHealthCheck.class.getSimpleName();

    private final Logger log = LoggerFactory.getLogger(GalaxyStatusManagerHealthCheck.class);

    private final GalaxyStatusManager statusManager;

    @Inject
    public GalaxyStatusManagerHealthCheck(final GalaxyStatusManager statusManager)
    {
        super(NAME);
        this.statusManager = statusManager;
    }

    @Override
    public Result check() throws Exception
    {
        if (!statusManager.isRunning()) {
            log.warn("{} check failed", NAME);
            return Result.unhealthy("GalaxyStatusManager is not running");
        }

        if (statusManager.getCoreStatusMap().size() == 0) {
            log.warn("{} check failed", NAME);
            return Result.unhealthy("GalaxyStatusManager is running but no core was found");
        }

        log.info("{} check succeeded", NAME);
        return Result.healthy();
    }
}

