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

package com.ning.arecibo.alert.confdata.guice;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.util.jdbi.DBIProvider;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;

public class AlertDataModule extends AbstractModule
{
    private final String dbConfigPrefix;

    public AlertDataModule()
    {
        this("arecibo.alert.conf.db");
    }

    public AlertDataModule(final String dbConfigPrefix)
    {
        this.dbConfigPrefix = dbConfigPrefix;
    }

    @Override
    public void configure()
    {
        configureDBI();

        bind(ConfDataDAO.class).asEagerSingleton();
    }

    @VisibleForTesting
    protected void configureDBI()
    {
        final DBIProvider provider = new DBIProvider(System.getProperties(), dbConfigPrefix);
        configureDBIFromProvider(provider);
    }

    @VisibleForTesting
    protected void configureDBIFromProvider(final Provider<DBI> dbiProvider)
    {
        bind(DBI.class).toProvider(dbiProvider).asEagerSingleton();
        bind(IDBI.class).to(Key.get(DBI.class));
    }
}
