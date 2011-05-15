package com.ning.arecibo.dashboard.guice;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;
import org.skife.config.TimeSpan;

public interface DashboardConfig
{
    @Config("arecibo.dashboard.collector_cache_size")
    @Default("1024")
    int getCollectorCacheSize();

    @Config("arecibo.dashboard.collector_cache_max_wait")
    @Default("1m")
    TimeSpan getCollectorCacheMaxWait();

    @Config("arecibo.dashboard.collector_cache_time_to_live")
    @Default("2m")
    TimeSpan getCollectorCacheTimeToLive();

    @Config("arecibo.dashboard.table_cache_size")
    @Default("1024")
    int getTableCacheSize();

    @Config("arecibo.dashboard.alert_host_override")
    @DefaultNull
    String getAlertHostOverride();

    @Config("arecibo.dashboard.alert_manager_enabled")
    @Default("true")
    boolean isAlertManagerEnabled();

    @Config("arecibo.dashboard.collector_host")
    String getCollectorHost();

    @Config("arecibo.dashboard.dashboard_collector_keep_alive_host")
    @DefaultNull
    String getDashboardCollectorKeepAliveHost();

    @Config("arecibo.collector.service_name")
    @Default("EventCollectorServer")
    String getCollectorServiceName();

    @Config("arecibo.dashboard.e2ez_threshold_config_cache_size")
    @Default("1024")
    int getE2EZThresholdConfigCacheSize();

    @Config("arecibo.dashboard.e2ez_time_range_map_cache_size")
    @Default("64")
    int getE2EZTimeRangeMapCacheSize();
}
