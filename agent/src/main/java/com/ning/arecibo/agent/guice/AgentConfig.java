package com.ning.arecibo.agent.guice;

import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.TimeSpan;
import com.ning.arecibo.agent.config.ConfigInitType;
import com.ning.arecibo.agent.config.ConfigType;

public abstract class AgentConfig
{
    @Config("arecibo.tools.coremonitor.threadpool_size")
    @Default("50")
    public abstract int getThreadpoolSize();

    @Config("arecibo.tools.coremonitor.max_active_configs")
    @Default("50000")
    public abstract int getMaxActiveConfigs();

    @Config("arecibo.tools.coremonitor.connection_timeout")
    @Default("10s")
    public abstract TimeSpan getConnectionTimeout();

    @Config("arecibo.tools.coremonitor.connection_timeout_initial")
    @Default("1s")
    public abstract TimeSpan getConnectionTimeoutInitial();

    @Config("arecibo.tools.coremonitor.config_update_interval")
    @Default("300s")
    public abstract TimeSpan getConfigUpdateInterval();

    @Config("arecibo.tools.coremonitor.config_update_initial_delay_range")
    @Default("300")
    public abstract int getConfigUpdateInitialDelayRange();

    @Config("arecibo.tools.coremonitor.jmx_port")
    @Default("8989")
    public abstract int getJMXPort();

    @Config("arecibo.tools.coremonitor.snmp_port")
    @Default("161")
    public abstract int getSNMPPort();

    @Config("arecibo.tools.coremonitor.snmp_community")
    @Default("")
    public abstract String getSNMPCommunity();

    @Config("arecibo.tools.coremonitor.snmp_compiled_mib_dir")
    @Default("compiled_mibs")
    public abstract String getSNMPCompiledMibDir();

    @Config("arecibo.tools.coremonitor.default_polling_interval_seconds")
    @Default("30s")
    public abstract TimeSpan getDefaultPollingInterval();

    @Config("arecibo.tools.coremonitor.max_polling_retry_delay")
    @Default("600s")
    public abstract TimeSpan getMaxPollingRetryDelay();

    @Config("arecibo.tools.coremonitor.per_host_concurrency")
    @Default("1")
    public abstract int getPerHostSemaphoreConcurrency();

    @Config("arecibo.tools.coremonitor.per_host_semaphore_max_wait_millis")
    @Default("0ms")
    public abstract TimeSpan getPerHostSemaphoreMaxWait();

    @Config("arecibo.tools.coremonitor.per_host_semaphore_retry_millis")
    @Default("10ms")
    public abstract TimeSpan getPerHostSemaphoreRetry();

    @Config("arecibo.tools.coremonitor.config_type")
    @Default("CONFIG_BY_EXPLICIT_PARAMS")
    public abstract ConfigInitType getConfigInitTypeParam();

    @Config("arecibo.tools.coremonitor.jmx_monitoring_profile_polling_enabled")
    @Default("true")
    public abstract boolean isJMXMonitoringProfilePollingEnabled();

    @Config("arecibo.tools.coremonitor.config_types_enabled")
    @Default("NON_HEALTH_CHECK_TYPES_ONLY")
    public abstract ConfigType[] getConfigTypesEnabled();

    @Config("arecibo.tools.coremonitor.published_host_suffix")
    @Default("")
    public abstract String getPublishedHostSuffix();

    @Config("arecibo.tools.coremonitor.published_path_suffix")
    @Default("")
    public abstract String getPublishedPathSuffix();

    @Config("arecibo.tools.coremonitor.explicit_mode_config_file_list")
    @Default("")
    public abstract List<String> getExplicitConfigFiles();

    @Config("arecibo.tools.coremonitor.explicit_mode_host_list")
    @Default("")
    public abstract List<String> getExplicitHosts();

    @Config("arecibo.tools.coremonitor.explicit_mode_host_list")
    @Default("")
    public abstract List<String> getExplicitPaths();

    @Config("arecibo.tools.coremonitor.explicit_mode_type_list")
    @Default("")
    public abstract List<String> getExplicitTypes();

    @Config("arecibo.tools.coremonitor.advertise_receiver_on_beacon")
    @Default("false")
    public abstract boolean isAdvertiseReceiverOnBeacon();

    @Config("arecibo.tools.coremonitor.relay_service_name")
    @Default("")
    public abstract String getRelayServiceName();

    @Config("arecibo.tools.coremonitor.http_user_agent")
    public String getHTTPUserAgent()
    {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("user-agent");
            String userAgent = bundle.getString("user_agent");

            return userAgent;
        }
        catch (MissingResourceException mrEx) {
            throw new RuntimeException(mrEx);
        }
    }

    @Config("arecibo.tools.coremonitor.http_proxy_host")
    @Default("")
    public abstract String getHTTPProxyHost();

    @Config("arecibo.tools.coremonitor.http_proxy_port")
    @Default("-1")
    public abstract int getHTTPProxyPort();
}
