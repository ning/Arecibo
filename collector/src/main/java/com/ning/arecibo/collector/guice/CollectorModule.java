package com.ning.arecibo.collector.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ning.arecibo.collector.ResolutionUtils;
import com.ning.arecibo.collector.config.CollectorConfig;
import com.ning.arecibo.collector.dao.CollectorDAO;
import com.ning.arecibo.collector.dao.MySQLCollectorDAO;
import com.ning.arecibo.collector.dao.OracleCollectorDAO;
import com.ning.arecibo.util.jdbi.DBIProvider;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

public class CollectorModule extends AbstractModule
{
    public final static String COLLECTOR_DB = "collector_db";

    private final CollectorConfig config;

    public CollectorModule(CollectorConfig config)
    {
        this.config = config;
    }

    @Override
    public void configure()
    {
        // set up db connection, with named statistics
        final Named moduleName = Names.named(COLLECTOR_DB);

        bind(DBI.class).annotatedWith(moduleName).toProvider(new DBIProvider(
            config.getJdbcUrl(),
            config.getDBUsername(),
            config.getDBPassword(),
            config.getMinConnectionsPerPartition(),
            config.getMaxConnectionsPerPartition()
        )).asEagerSingleton();
        bind(IDBI.class).annotatedWith(moduleName).to(Key.get(DBI.class, moduleName));

        if (config.getDBType().equals("MYSQL")) {
            bind(CollectorDAO.class).to(MySQLCollectorDAO.class).asEagerSingleton();
        }
        else if (config.getDBType().equals("ORACLE")) {
            bind(CollectorDAO.class).to(OracleCollectorDAO.class).asEagerSingleton();
        }
        else {
            throw new IllegalArgumentException("Support for DB not implemented: " + config.getDBType());
        }

        bind(ResolutionUtils.class).toInstance(new ResolutionUtils());

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(CollectorDAO.class).as("arecibo.collector:name=CollectorDAO");
    }
}

