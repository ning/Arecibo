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

package com.ning.arecibo.collector;

import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ning.arecibo.collector.guice.CollectorConstants;
import com.ning.arecibo.collector.guice.CollectorModule;
import com.ning.arecibo.util.timeline.TimelineDAO;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;

public class CollectorTestModule extends CollectorModule
{
    @Override
    protected void configureDao()
    {
        final MysqlTestingHelper helper = new MysqlTestingHelper();
        bind(MysqlTestingHelper.class).toInstance(helper);
        final DBI dbi = helper.getDBI();

        final Named moduleName = Names.named(CollectorConstants.COLLECTOR_DB);
        bind(DBI.class).annotatedWith(moduleName).toInstance(dbi);
        bind(IDBI.class).annotatedWith(moduleName).toInstance(dbi);
        bind(TimelineDAO.class).asEagerSingleton();
    }
}
