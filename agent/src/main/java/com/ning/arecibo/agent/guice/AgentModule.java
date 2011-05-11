package com.ning.arecibo.agent.guice;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.AbstractModule;
import com.ning.arecibo.agent.AgentDataCollectorManager;
import com.ning.arecibo.agent.config.ConfigFileUtils;
import com.ning.arecibo.agent.config.ConfigInitType;
import com.ning.arecibo.agent.config.jmx.JMXConfigIteratorFactory;
import com.ning.arecibo.agent.config.jmx.JMXDynamicUtils;
import com.ning.arecibo.agent.config.snmp.SNMPConfigIteratorFactory;
import com.ning.arecibo.agent.datasource.DataSourceUtils;
import com.ning.arecibo.agent.datasource.IdentityConfigIteratorFactory;
import com.ning.arecibo.agent.datasource.jmx.JMXClientCache;
import com.ning.arecibo.agent.datasource.jmx.JMXParserManager;
import com.ning.arecibo.util.Logger;

public class AgentModule extends AbstractModule
{
    final static Logger log = Logger.getLogger(AgentModule.class);

    @Override
	public void configure()
	{
        initializeRmiConnectionTimeouts();

        bindConstant().annotatedWith(ThreadpoolSize.class).to(Integer.getInteger("arecibo.tools.coremonitor.threadpool_size", ThreadpoolSize.DEFAULT));
        bindConstant().annotatedWith(MaxActiveConfigs.class).to(Integer.getInteger("arecibo.tools.coremonitor.max_active_configs", MaxActiveConfigs.DEFAULT));
        bindConstant().annotatedWith(ConnectionTimeout.class).to(Integer.getInteger("arecibo.tools.coremonitor.connection_timeout", ConnectionTimeout.DEFAULT));
        bindConstant().annotatedWith(ConnectionTimeoutInitial.class).to(Integer.getInteger("arecibo.tools.coremonitor.connection_timeout_initial", ConnectionTimeoutInitial.DEFAULT));
        bindConstant().annotatedWith(ConfigUpdateInterval.class).to(Integer.getInteger("arecibo.tools.coremonitor.config_update_interval", ConfigUpdateInterval.DEFAULT));
        bindConstant().annotatedWith(ConfigUpdateInitialDelayRange.class).to(Integer.getInteger("arecibo.tools.coremonitor.config_update_initial_delay_range", ConfigUpdateInitialDelayRange.DEFAULT));
        bindConstant().annotatedWith(JMXPort.class).to(Integer.getInteger("arecibo.tools.coremonitor.jmx_port", JMXPort.DEFAULT));
        bindConstant().annotatedWith(SNMPPort.class).to(Integer.getInteger("arecibo.tools.coremonitor.snmp_port", SNMPPort.DEFAULT));
		bindConstant().annotatedWith(SNMPCommunity.class).to(System.getProperty("arecibo.tools.coremonitor.snmp_community", SNMPCommunity.DEFAULT));
		bindConstant().annotatedWith(SNMPCompiledMibDir.class).to(System.getProperty("arecibo.tools.coremonitor.snmp_compiled_mib_dir", SNMPCompiledMibDir.DEFAULT));
		bindConstant().annotatedWith(DefaultPollingIntervalSeconds.class).to(Integer.getInteger("arecibo.tools.coremonitor.default_polling_interval_seconds", DefaultPollingIntervalSeconds.DEFAULT));
		bindConstant().annotatedWith(MaxPollingRetryDelay.class).to(Integer.getInteger("arecibo.tools.coremonitor.max_polling_retry_delay", MaxPollingRetryDelay.DEFAULT));
		bindConstant().annotatedWith(PerHostSemaphoreConcurrency.class).to(Integer.getInteger("arecibo.tools.coremonitor.per_host_concurrency", PerHostSemaphoreConcurrency.DEFAULT));
		bindConstant().annotatedWith(PerHostSemaphoreMaxWaitMillis.class).to(Long.getLong("arecibo.tools.coremonitor.per_host_semaphore_max_wait_millis", PerHostSemaphoreMaxWaitMillis.DEFAULT));
		bindConstant().annotatedWith(PerHostSemaphoreRetryMillis.class).to(Long.getLong("arecibo.tools.coremonitor.per_host_semaphore_retry_millis", PerHostSemaphoreRetryMillis.DEFAULT));
		bindConstant().annotatedWith(ConfigInitTypeParam.class).to(ConfigInitType.valueOf(System.getProperty("arecibo.tools.coremonitor.config_type", ConfigInitType.CONFIG_BY_EXPLICIT_PARAMS.name())));

		bindConstant().annotatedWith(JMXMonitoringProfilePollingEnabled.class).to(Boolean.parseBoolean(System.getProperty("arecibo.tools.coremonitor.jmx_monitoring_profile_polling_enabled", Boolean.toString(JMXMonitoringProfilePollingEnabled.DEFAULT))));
        bindConstant().annotatedWith(ConfigTypesEnabled.class).to(System.getProperty("arecibo.tools.coremonitor.config_types_enabled", ConfigTypesEnabled.DEFAULT));

        bindConstant().annotatedWith(PublishedHostSuffix.class).to(System.getProperty("arecibo.tools.coremonitor.published_host_suffix", PublishedHostSuffix.DEFAULT));
        bindConstant().annotatedWith(PublishedPathSuffix.class).to(System.getProperty("arecibo.tools.coremonitor.published_path_suffix", PublishedPathSuffix.DEFAULT));

        bindConstant().annotatedWith(ExplicitConfigFiles.class).to(System.getProperty("arecibo.tools.coremonitor.explicit_mode_config_file_list", ExplicitConfigFiles.DEFAULT));
        bindConstant().annotatedWith(ExplicitHosts.class).to(System.getProperty("arecibo.tools.coremonitor.explicit_mode_host_list", ExplicitHosts.DEFAULT));
        bindConstant().annotatedWith(ExplicitPaths.class).to(System.getProperty("arecibo.tools.coremonitor.explicit_mode_path_list", ExplicitPaths.DEFAULT));
        bindConstant().annotatedWith(ExplicitTypes.class).to(System.getProperty("arecibo.tools.coremonitor.explicit_mode_type_list", ExplicitTypes.DEFAULT));

        bindConstant().annotatedWith(AdvertiseReceiverOnBeacon.class).to(Boolean.parseBoolean(System.getProperty("arecibo.tools.coremonitor.advertise_receiver_on_beacon", Boolean.toString(AdvertiseReceiverOnBeacon.DEFAULT))));
        bindConstant().annotatedWith(RelayServiceName.class).to(System.getProperty("arecibo.tools.coremonitor.relay_service_name", RelayServiceName.DEFAULT));

        String defaultUserAgentString = initializeUserAgentString();

        bindConstant().annotatedWith(HTTPUserAgent.class).to(System.getProperty("arecibo.tools.coremonitor.http_user_agent", defaultUserAgentString));
        bindConstant().annotatedWith(HTTPProxyHost.class).to(System.getProperty("arecibo.tools.coremonitor.http_proxy_host", HTTPProxyHost.DEFAULT));
        bindConstant().annotatedWith(HTTPProxyPort.class).to(Integer.getInteger("arecibo.tools.coremonitor.http_proxy_port", HTTPProxyPort.DEFAULT));

        bind(JMXClientCache.class).asEagerSingleton();
        bind(JMXDynamicUtils.class).asEagerSingleton();
        bind(JMXParserManager.class).asEagerSingleton();
        bind(DataSourceUtils.class).asEagerSingleton();
        bind(IdentityConfigIteratorFactory.class).asEagerSingleton();
        bind(JMXConfigIteratorFactory.class).asEagerSingleton();
        bind(SNMPConfigIteratorFactory.class).asEagerSingleton();
        bind(ConfigFileUtils.class).asEagerSingleton();
        bind(AgentDataCollectorManager.class).asEagerSingleton();

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(AgentDataCollectorManager.class).as("arecibo.agent:name=AgentDataManager");
	}

    private void initializeRmiConnectionTimeouts() {
        
        // set the rmi timeouts for the jmx connection/read method calls
        int connectionTimeout = Integer.getInteger("arecibo.tools.coremonitor.connection_timeout", ConnectionTimeout.DEFAULT);
        long connectionTimeoutMillis = connectionTimeout * 1000L;
        System.setProperty("sun.rmi.transport.connectionTimeout",Long.toString(connectionTimeoutMillis));
        System.setProperty("sun.rmi.transport.tcp.responseTimeout",Long.toString(connectionTimeoutMillis));
    }

    private String initializeUserAgentString() {

        try {
            ResourceBundle bundle = ResourceBundle.getBundle("user-agent");
            String userAgent = bundle.getString("user_agent");

            return userAgent;
        }
        catch (MissingResourceException mrEx) {
            throw new RuntimeException(mrEx);
        }
    }
}
