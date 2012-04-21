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

package com.ning.arecibo.collector.guice;

import com.google.inject.Inject;
import com.ning.arecibo.util.timeline.CachingTimelineDAO;
import com.ning.arecibo.util.timeline.DefaultTimelineDAO;
import com.ning.arecibo.util.timeline.TimelineDAO;
import org.skife.jdbi.v2.DBI;
import org.weakref.jmx.MBeanExporter;
import org.weakref.jmx.ObjectNames;

import javax.inject.Provider;
import javax.management.MBeanServer;

public class CachingDefaultTimelineDAOProvider implements Provider<TimelineDAO>
{
    private final CollectorConfig config;
    private final DBI dbi;
    private final MBeanServer mBeanServer;

    @Inject
    public CachingDefaultTimelineDAOProvider(final CollectorConfig config, final DBI dbi, final MBeanServer mBeanServer)
    {
        this.config = config;
        this.dbi = dbi;
        this.mBeanServer = mBeanServer;
    }

    @Override
    public TimelineDAO get()
    {
        final TimelineDAO delegate = new DefaultTimelineDAO(dbi);
        final CachingTimelineDAO cachingTimelineDAO = new CachingTimelineDAO(delegate);

        final MBeanExporter exporter = new MBeanExporter(mBeanServer);
        exporter.export(ObjectNames.generatedNameOf(CachingTimelineDAO.class), cachingTimelineDAO);

        return cachingTimelineDAO;
    }
}
