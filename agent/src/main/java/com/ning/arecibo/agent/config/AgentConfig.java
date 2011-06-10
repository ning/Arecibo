package com.ning.arecibo.agent.config;

import org.skife.config.Default;

public interface AgentConfig
{
    @org.skife.config.Config("arecibo.tools.coremonitor.threadpool_size")
    @Default("50")
    int getMonitorThreadPoolSize();

    @org.skife.config.Config("arecibo.tools.coremonitor.max_active_configs")
    @Default("50000")
    int getMonitorMaxActiveConfigs();

    @org.skife.config.Config("arecibo.tools.coremonitor.connection_timeout")
    @Default("10")
    int getMonitorConnectionTimeout();

    @org.skife.config.Config("arecibo.tools.coremonitor.connection_timeout_initial")
    @Default("1")
    int getMonitorConnectionTimeoutInitial();

    @org.skife.config.Config("arecibo.tools.coremonitor.config_update_interval")
    @Default("300")
        // 5 minutes
    int getMonitorConfigUpdateInterval();

    @org.skife.config.Config("arecibo.tools.coremonitor.config_update_initial_delay_range")
    @Default("300")
        // 5 minutes
    int getMonitorConfigUpdateInitialDelayRange();

    @org.skife.config.Config("arecibo.tools.coremonitor.jmx_port")
    @Default("8989")
    int getMonitorJmxPort();

    @org.skife.config.Config("arecibo.tools.coremonitor.snmp_port")
    @Default("161")
    int getMonitorSnmpPort();

    @org.skife.config.Config("arecibo.tools.coremonitor.snmp_community")
    @Default("")
    String getMonitorSnmpCommunity();

    @org.skife.config.Config("arecibo.tools.coremonitor.snmp_compiled_mib_dir")
    @Default("compiled_mibs")
    String getMonitorSnmpCompiledMibDir();

    @org.skife.config.Config("arecibo.tools.coremonitor.default_polling_interval_seconds")
    @Default("30")
    int getMonitorDefaultPollingIntervalSeconds();

    @org.skife.config.Config("arecibo.tools.coremonitor.max_polling_retry_delay")
    @Default("600")
        // 10 minutes
    int getMonitorMaxPollingRetryDelay();

    @org.skife.config.Config("arecibo.tools.coremonitor.per_host_concurrency")
    @Default("1")
    int getMonitorPerHostSemaphoreConcurrency();

    @org.skife.config.Config("arecibo.tools.coremonitor.per_host_semaphore_max_wait_millis")
    @Default("0")
    long getMonitorPerHostSemaphoreMaxWaitMillis();

    @org.skife.config.Config("arecibo.tools.coremonitor.per_host_semaphore_retry_millis")
    @Default("10")
    int getMonitorPerHostSemaphoreRetryMillis();

    @org.skife.config.Config("arecibo.tools.coremonitor.config_type")
    @Default("CONFIG_BY_EXPLICIT_PARAMS")
    ConfigInitType getMonitorConfigType();

    @org.skife.config.Config("arecibo.tools.coremonitor.jmx_monitoring_profile_polling_enabled")
    @Default("true")
    boolean isMonitorJmxMonitoringProfilePollingEnabled();

    @org.skife.config.Config("arecibo.tools.coremonitor.config_types_enabled")
    @Default("NON_HEALTH_CHECK_TYPES_ONLY")
    String getMonitorConfigTypesEnabled();

    @org.skife.config.Config("arecibo.tools.coremonitor.published_host_suffix")
    @Default("")
    String getMonitorPublishedHostSuffix();

    @org.skife.config.Config("arecibo.tools.coremonitor.published_path_suffix")
    @Default("")
    String getMonitorPublishedPathSuffix();

    @org.skife.config.Config("arecibo.tools.coremonitor.explicit_mode_config_file_list")
    @Default("")
    String getMonitorExplicitModeConfigFileList();

    @org.skife.config.Config("arecibo.tools.coremonitor.explicit_mode_host_list")
    @Default("")
    String getMonitorExplicitModeHostList();

    @org.skife.config.Config("arecibo.tools.coremonitor.explicit_mode_path_list")
    @Default("")
    String getMonitorExplicitModePathList();

    @org.skife.config.Config("arecibo.tools.coremonitor.explicit_mode_type_list")
    @Default("")
    String getMonitorExplicitModeTypeList();

    @org.skife.config.Config("arecibo.tools.coremonitor.advertise_receiver_on_beacon")
    @Default("false")
    boolean getMonitorAdvertiseReceiverOnBeacon();

    @org.skife.config.Config("arecibo.tools.coremonitor.relay_service_name")
    @Default("")
    String getMonitorRelayServiceName();

    @org.skife.config.Config("arecibo.tools.coremonitor.http_user_agent")
    @Default("arecibo")
    String getMonitorHttpUserAgent();

    @org.skife.config.Config("arecibo.tools.coremonitor.http_proxy_host")
    @Default("")
    String getMonitorHttpProxyHost();

    @org.skife.config.Config("arecibo.tools.coremonitor.http_proxy_port")
    @Default("-1")
    int getMonitorHttpProxyPort();

}
