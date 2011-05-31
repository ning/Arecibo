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
