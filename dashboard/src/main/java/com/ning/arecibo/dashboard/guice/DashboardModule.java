package com.ning.arecibo.dashboard.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ning.arecibo.dashboard.alert.AlertRESTClient;
import com.ning.arecibo.dashboard.alert.AlertStatusManager;
import com.ning.arecibo.dashboard.alert.ClusterAwareAlertClient;
import com.ning.arecibo.dashboard.context.ContextMbeanManager;
import com.ning.arecibo.dashboard.format.DashboardFormatManager;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;
import com.ning.arecibo.event.publisher.HdfsEventPublisher;
import com.ning.arecibo.event.publisher.RandomEventServiceChooser;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jdbi.DBIProvider;
import com.ning.arecibo.util.service.DummyServiceLocator;
import com.ning.arecibo.util.service.ServiceLocator;
import org.skife.config.ConfigurationObjectFactory;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

public class DashboardModule extends AbstractModule
{
    private static final Logger log = Logger.getLogger(DashboardModule.class);

    @Override
    public void configure()
    {
        final DashboardConfig config = new ConfigurationObjectFactory(System.getProperties()).build(DashboardConfig.class);
        bind(DashboardConfig.class).toInstance(config);

        configureServiceLocator(config);

        bind(DashboardFormatManager.class).asEagerSingleton();
        bind(GalaxyStatusManager.class).asEagerSingleton();
        bind(AlertStatusManager.class).asEagerSingleton();
        bind(ClusterAwareAlertClient.class).asEagerSingleton();
        bind(AlertRESTClient.class).asEagerSingleton();

        // set up the database connection, with named statistics
        final Named moduleName = Names.named(DashboardConstants.DASHBOARD_COLLECTOR_DB);

        bind(DBI.class).annotatedWith(moduleName).toProvider(new DBIProvider(System.getProperties(), "arecibo.events.collector.db")).asEagerSingleton();
        bind(IDBI.class).annotatedWith(moduleName).to(Key.get(DBI.class, moduleName));
        bind(ContextMbeanManager.class).asEagerSingleton();

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(RandomEventServiceChooser.class).as("arecibo:type=HdfsEventServiceChooser");
        builder.export(HdfsEventPublisher.class).as("arecibo:name=HdfsEventPublisher");

        builder.export(ContextMbeanManager.class).as("arecibo.dashboard:name=ContextMbeanManager");
    }

    private void configureServiceLocator(final DashboardConfig config)
    {
        try {
            bind(ServiceLocator.class).to((Class<? extends ServiceLocator>) Class.forName(config.getServiceLocatorClass())).asEagerSingleton();
        }
        catch (ClassNotFoundException e) {
            log.error("Unable to find ServiceLocator", e);
            bind(ServiceLocator.class).to(DummyServiceLocator.class).asEagerSingleton();
        }
    }
}
