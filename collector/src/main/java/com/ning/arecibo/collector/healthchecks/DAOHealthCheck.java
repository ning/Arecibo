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

package com.ning.arecibo.collector.healthchecks;

import com.google.inject.Inject;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.yammer.metrics.core.HealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DAOHealthCheck extends HealthCheck
{
    private static final String NAME = DAOHealthCheck.class.getSimpleName();

    private final Logger log = LoggerFactory.getLogger(DAOHealthCheck.class);

    private final TimelineDAO dao;

    @Inject
    public DAOHealthCheck(final TimelineDAO dao)
    {
        super(NAME);
        this.dao = dao;
    }

    @Override
    public Result check() throws Exception
    {
        try {
            dao.test();
        }
        catch (Exception e) {
            log.warn("{} check failed", NAME);
            return Result.unhealthy(e);
        }

        log.info("{} check succeeded", NAME);
        return Result.healthy();
    }
}
