package com.ning.arecibo.alert.objects;

import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.manage.AlertManager;

public interface StatefulConfigurableObject extends ConfigurableObject {

    public enum LastConfigAction {CONFIGURE,UPDATE}

    public boolean postUpdateConfigure(ConfigManager configManager, AlertManager alertManager);
}
