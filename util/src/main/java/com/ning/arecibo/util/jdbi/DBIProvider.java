package com.ning.arecibo.util.jdbi;

import java.util.Properties;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.logging.Log4JLog;
import com.google.inject.Provider;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;

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
        BoneCPConfig dbConfig = new BoneCPConfig();

        dbConfig.setJdbcUrl(props.getProperty(prefix + ".url"));
        dbConfig.setUsername(props.getProperty(prefix + ".user"));
        dbConfig.setPassword(props.getProperty(prefix + ".password"));
        dbConfig.setMinConnectionsPerPartition(Integer.valueOf(props.getProperty(prefix + ".minIdleConnections", "1")));
        dbConfig.setMaxConnectionsPerPartition(Integer.valueOf(props.getProperty(prefix + ".maxActiveConnections", "50")));
        dbConfig.setPartitionCount(1);

        final DBI dbi = new DBI(new BoneCPDataSource(dbConfig));

        dbi.setSQLLog(new Log4JLog());
        return dbi;
    }
}
