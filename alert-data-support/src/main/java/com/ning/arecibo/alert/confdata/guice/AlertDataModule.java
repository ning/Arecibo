package com.ning.arecibo.alert.confdata.guice;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.util.jdbi.DBIProvider;

public class AlertDataModule implements Module
{
    private final String dbConfigPrefix;

    public AlertDataModule() {
        this("arecibo.alert.conf.db");
    }

    public AlertDataModule(String dbConfigPrefix) {
        this.dbConfigPrefix = dbConfigPrefix;
    }

	public void configure(Binder binder)
	{
        final Named moduleName = Names.named(AlertDataConstants.ALERT_DATA_DB);

        binder.bind(DBI.class).annotatedWith(moduleName).toProvider(new DBIProvider(System.getProperties(), dbConfigPrefix)).asEagerSingleton();
        binder.bind(IDBI.class).annotatedWith(moduleName).to(Key.get(DBI.class, moduleName));
        binder.bind(ConfDataDAO.class).asEagerSingleton();
	}
}
