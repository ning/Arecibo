package com.ning.arecibo.alert.confdata.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
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
        final Named moduleName = Names.named(AlertDataConstants.ALERT_DATA_DB);

        bind(DBI.class).annotatedWith(moduleName).toProvider(new DBIProvider(System.getProperties(), dbConfigPrefix)).asEagerSingleton();
        bind(IDBI.class).annotatedWith(moduleName).to(Key.get(DBI.class, moduleName));
        bind(ConfDataDAO.class).asEagerSingleton();
    }
}
