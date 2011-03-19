package com.ning.arecibo.util.jdbi;

import com.google.inject.Provider;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.logging.Log4JLog;
import org.skife.jdbi.v2.unstable.stringtemplate.ClasspathGroupLoader;
import org.skife.jdbi.v2.unstable.stringtemplate.StringTemplateStatementLocator;

import java.util.Properties;

public class DBIProvider implements Provider<DBI>
{
    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final int minIdleConnections;
    private final int maxActiveConnections;

    public DBIProvider(final Properties props, final String prefix)
    {
        this(props.getProperty(prefix + ".url"),
            props.getProperty(prefix + ".user"),
            props.getProperty(prefix + ".password"),
            Integer.valueOf(props.getProperty(prefix + ".minIdleConnections", "1")),
            Integer.valueOf(props.getProperty(prefix + ".maxActiveConnections", "50")));
    }

    public DBIProvider(String jdbcUrl, String user, String password, int minIdleConnections, int maxIdleConnections)
    {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        this.minIdleConnections = minIdleConnections;
        this.maxActiveConnections = maxIdleConnections;
    }

    @Override
    public DBI get()
    {
        BoneCPConfig dbConfig = new BoneCPConfig();

        dbConfig.setJdbcUrl(jdbcUrl);
        dbConfig.setUsername(user);
        dbConfig.setPassword(password);
        dbConfig.setMinConnectionsPerPartition(minIdleConnections);
        dbConfig.setMaxConnectionsPerPartition(maxActiveConnections);
        dbConfig.setPartitionCount(1);

        final DBI dbi = new DBI(new BoneCPDataSource(dbConfig));

        dbi.setStatementLocator(new StringTemplateStatementLocator(new ClasspathGroupLoader(".")));
        dbi.setSQLLog(new Log4JLog());
        return dbi;
    }
}
