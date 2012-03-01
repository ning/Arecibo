package com.ning.arecibo.dashboard.guice;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;
import org.skife.config.TimeSpan;

public interface DashboardConfig
{
    @Config("arecibo.dashboard.alert_host_override")
    @DefaultNull
    String getAlertHostOverride();

    @Config("arecibo.dashboard.alert_manager_enabled")
    @Default("false")
    boolean isAlertManagerEnabled();

    @Config("arecibo.dashboard.serviceLocatorKlass")
    @Default("com.ning.arecibo.util.service.DummyServiceLocator")
    String getServiceLocatorClass();

    @Config("arecibo.dashboard.galaxy.updateInterval")
    @Default("5m")
    TimeSpan getGalaxyUpdateInterval();
}
