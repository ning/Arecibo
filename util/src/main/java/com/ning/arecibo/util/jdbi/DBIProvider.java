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

package com.ning.arecibo.util.jdbi;

import com.google.inject.Provider;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.logging.Log4JLog;

import java.util.Properties;

public class DBIProvider implements Provider<DBI>
{
    private final Properties props;
    private final String prefix;

    public DBIProvider(final Properties props, final String prefix)
    {
        this.prefix = prefix;
        this.props = props;
    }

    @Override
    public DBI get()
    {
        final BoneCPConfig dbConfig = new BoneCPConfig();

        dbConfig.setJdbcUrl(props.getProperty(prefix + ".url"));
        dbConfig.setUsername(props.getProperty(prefix + ".user"));
        dbConfig.setPassword(props.getProperty(prefix + ".password"));
        dbConfig.setMinConnectionsPerPartition(Integer.valueOf(props.getProperty(prefix + ".minIdleConnections", "1")));
        dbConfig.setMaxConnectionsPerPartition(Integer.valueOf(props.getProperty(prefix + ".maxActiveConnections", "50")));
        dbConfig.setPartitionCount(1);
        // Needs to be less than MySQL wait_timeout
        dbConfig.setIdleConnectionTestPeriodInMinutes(Integer.valueOf(props.getProperty(prefix + ".connectionTestPeriodInMinutes", "5")));

        final DBI dbi = new DBI(new BoneCPDataSource(dbConfig));

        dbi.setSQLLog(new Log4JLog());
        return dbi;
    }
}
