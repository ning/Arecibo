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

package com.ning.arecibo.util.timeline;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

/**
 * This class represents one row in the hosts table
 * TODO: Integrate with the existing Arecibo host mechanism.
 */
public class HostAndId {
    public static final ResultSetMapper<HostAndId> hostAndIdMapper = new ResultSetMapper<HostAndId>() {

        @Override
        public HostAndId map(int index, ResultSet r, StatementContext ctx) throws SQLException {
            return new HostAndId(r.getString("host"), r.getInt("id"));
        }

    };

    private final String host;
    private final int hostId;

    public HostAndId(String host, int hostId) {
        this.host = host;
        this.hostId = hostId;
    }

    public String getHost() {
        return host;
    }

    public int getHostId() {
        return hostId;
    }
}
