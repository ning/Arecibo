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

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.ning.arecibo.collector.CollectorClient;
import com.yammer.metrics.core.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectorClientHealthCheck extends HealthCheck
{
    private static final String NAME = CollectorClientHealthCheck.class.getSimpleName();

    private final Logger log = LoggerFactory.getLogger(CollectorClientHealthCheck.class);

    private final CollectorClient client;

    @Inject
    public CollectorClientHealthCheck(final CollectorClient client)
    {
        super(NAME);
        this.client = client;
    }

    @Override
    public HealthCheck.Result check() throws Exception
    {
        try {
            if (ImmutableList.<String>copyOf(client.getHosts()).size() == 0) {
                log.warn("{} check failed", NAME);
                return Result.unhealthy("CollectorClient is working but no host was found");
            }
        }
        catch (Exception e) {
            log.warn("{} check failed", NAME);
            return Result.unhealthy(e);
        }

        log.info("{} check succeeded", NAME);
        return HealthCheck.Result.healthy();
    }
}