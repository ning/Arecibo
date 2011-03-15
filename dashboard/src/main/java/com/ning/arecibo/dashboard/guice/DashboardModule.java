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
        bindConstant().annotatedWith(CollectorCacheSize.class).to(Integer.getInteger("arecibo.dashboard.collector_cache_size", CollectorCacheSize.DEFAULT));
        bindConstant().annotatedWith(CollectorCacheMaxWaitMS.class).to(Long.getLong("arecibo.dashboard.collector_cache_max_wait_ms", CollectorCacheMaxWaitMS.DEFAULT));
        bindConstant().annotatedWith(CollectorCacheTimeToLiveMS.class).to(Long.getLong("arecibo.dashboard.collector_cache_time_to_live_ms", CollectorCacheTimeToLiveMS.DEFAULT));
        bindConstant().annotatedWith(TableCacheSize.class).to(Integer.getInteger("arecibo.dashboard.table_cache_size", TableCacheSize.DEFAULT));
		bindConstant().annotatedWith(AlertHostOverride.class).to(System.getProperty("arecibo.dashboard.alert_host_override" , AlertHostOverride.DEFAULT));
		bindConstant().annotatedWith(AlertPortOverride.class).to(Integer.getInteger("arecibo.dashboard.alert_port_override", AlertPortOverride.DEFAULT));
		bindConstant().annotatedWith(AlertManagerEnabled.class).to(Boolean.parseBoolean(System.getProperty("arecibo.dashboard.alert_manager_enabled", Boolean.toString(AlertManagerEnabled.DEFAULT))));
		bindConstant().annotatedWith(CollectorHostOverride.class).to(System.getProperty("arecibo.dashboard.collector_host_override", CollectorHostOverride.DEFAULT));
		bindConstant().annotatedWith(CollectorRMIPortOverride.class).to(Integer.getInteger("arecibo.dashboard.collector_rmiport_override", CollectorRMIPortOverride.DEFAULT));
		bindConstant().annotatedWith(DashboardCollectorKeepAliveHost.class).to(System.getProperty("arecibo.dashboard.dashboard_collector_keep_alive_host", DashboardCollectorKeepAliveHost.DEFAULT));
		bindConstant().annotatedWith(CollectorServiceName.class).to(System.getProperty("arecibo.collector.service_name", CollectorServiceName.DEFAULT));
		bindConstant().annotatedWith(E2EZThresholdConfigCacheSize.class).to(Integer.getInteger("arecibo.dashboard.e2ez_threshold_config_cache_size", E2EZThresholdConfigCacheSize.DEFAULT));
		bindConstant().annotatedWith(E2EZTimeRangeMapCacheSize.class).to(Integer.getInteger("arecibo.dashboard.e2ez_time_range_map_cache_size", E2EZTimeRangeMapCacheSize.DEFAULT));

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
