package com.ning.arecibo.alert.objects;

import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;

public interface ConfigurableObject
{
    public Long getId();
    public String getLabel();
    public boolean isValid(ConfigManager configManager);
    public boolean configure(ConfigManager configManager,AlertManager alertManager, LoggingManager loggingManager);
    public boolean unconfigure(ConfigManager configManager,AlertManager alertManager);
    public boolean update(ConfigManager configManager,AlertManager alertManager, ConfigurableObject updateConfig);
}
