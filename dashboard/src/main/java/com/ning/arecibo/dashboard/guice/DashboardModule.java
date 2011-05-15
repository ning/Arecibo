package com.ning.arecibo.dashboard.guice;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ning.arecibo.dashboard.alert.AlertRESTClient;
import com.ning.arecibo.dashboard.alert.AlertStatusManager;
import com.ning.arecibo.dashboard.alert.ClusterAwareAlertClient;
import com.ning.arecibo.dashboard.alert.e2ez.E2EZConfigManager;
import com.ning.arecibo.dashboard.alert.e2ez.E2EZStatusManager;
import com.ning.arecibo.dashboard.context.ContextMbeanManager;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAO;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAOKeepAliveManager;
import com.ning.arecibo.dashboard.format.DashboardFormatManager;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;
import com.ning.arecibo.dashboard.table.DashboardTableBeanFactory;
import com.ning.arecibo.event.publisher.HdfsEventPublisher;
import com.ning.arecibo.event.publisher.RandomEventServiceChooser;
import com.ning.arecibo.util.jdbi.DBIProvider;

public class DashboardModule extends AbstractModule
{
    @Override
	public void configure()
	{
        bind(DashboardFormatManager.class).asEagerSingleton();
        bind(GalaxyStatusManager.class).asEagerSingleton();
        bind(AlertStatusManager.class).asEagerSingleton();
        bind(ClusterAwareAlertClient.class).asEagerSingleton();
        bind(AlertRESTClient.class).asEagerSingleton();
        bind(E2EZStatusManager.class).asEagerSingleton();
        bind(E2EZConfigManager.class).asEagerSingleton();

        // set up the database connection, with named statistics
        final Named moduleName = Names.named(DashboardConstants.DASHBOARD_COLLECTOR_DB);

        bind(DBI.class).annotatedWith(moduleName).toProvider(new DBIProvider(System.getProperties(), "arecibo.events.collector.db")).asEagerSingleton();
        bind(IDBI.class).annotatedWith(moduleName).to(Key.get(DBI.class, moduleName));
        bind(DashboardCollectorDAO.class).asEagerSingleton();
        bind(DashboardCollectorDAOKeepAliveManager.class).asEagerSingleton();
        bind(DashboardTableBeanFactory.class).asEagerSingleton();
        bind(ContextMbeanManager.class).asEagerSingleton();

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(RandomEventServiceChooser.class).as("arecibo:type=HdfsEventServiceChooser");
        builder.export(HdfsEventPublisher.class).as("arecibo:name=HdfsEventPublisher");

        builder.export(DashboardCollectorDAO.class).as("arecibo.dashboard:name=DashboardCollectorDAO");
        builder.export(DashboardCollectorDAOKeepAliveManager.class).as("arecibo.dashboard:name=DAOKeepAliveManager");
        builder.export(DashboardTableBeanFactory.class).as("arecibo.dashboard:name=TableBeanFactory");
        builder.export(ContextMbeanManager.class).as("arecibo.dashboard:name=ContextMbeanManager");
	}
}
