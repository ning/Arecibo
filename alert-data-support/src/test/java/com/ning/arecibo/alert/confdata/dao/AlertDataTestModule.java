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

package com.ning.arecibo.alert.confdata.dao;

import com.google.inject.Provider;
import com.ning.arecibo.alert.confdata.guice.AlertDataModule;
import com.ning.arecibo.dao.MysqlTestingHelper;
import com.ning.arecibo.dao.MysqlTestingHelperProvider;
import org.skife.jdbi.v2.DBI;

public class AlertDataTestModule extends AlertDataModule
{
    @Override
    protected void configureDBI()
    {
        final MysqlTestingHelper helper = new MysqlTestingHelper();
        bind(MysqlTestingHelper.class).toInstance(helper);

        final Provider<DBI> provider = new MysqlTestingHelperProvider(helper);
        configureDBIFromProvider(provider);
    }
}

