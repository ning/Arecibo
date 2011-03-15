package com.ning.arecibo.alert.objects;

import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroupMapping;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;
import com.ning.arecibo.util.Logger;



public class NotifGroupMapping extends ConfDataNotifGroupMapping implements ConfigurableObject
{
    private final static Logger log = Logger.getLogger(NotifGroupMapping.class);

    private volatile AlertingConfig alertingConfig = null;
    private volatile NotifGroup notifGroup = null;

    public NotifGroupMapping() {}

    @Override
    public synchronized boolean isValid(ConfigManager confManager) {
    	// make sure our notifGroup exists in the conf, and is valid
        if (!ConfigurableObjectUtils.checkNonNullAndValid(confManager.getNotifGroup(this.notifGroupId), confManager))
    		return false;

    	// make sure our alerting config exists in the conf, and is valid
        if (!ConfigurableObjectUtils.checkNonNullAndValid(confManager.getAlertingConfig(this.alertingConfigId), confManager))
    		return false;

    	return true;
    }

    @Override
    public synchronized boolean configure(ConfigManager confManager,AlertManager alertManager, LoggingManager loggingManager) {
        
        this.alertingConfig = confManager.getAlertingConfig(this.alertingConfigId);
        if(!ConfigurableObjectUtils.checkNonNullAndLog(this.alertingConfig,this.alertingConfigId,"alertConfig",confManager))
            return false;

        this.notifGroup = confManager.getNotifGroup(this.notifGroupId);
        if(!ConfigurableObjectUtils.checkNonNullAndLog(this.notifGroup,this.notifGroupId,"notifGroup",confManager))
            return false;

        
        this.alertingConfig.addNotifGroupMapping(this);
        this.notifGroup.addNotifGroupMapping(this);

        return true;
    }

    @Override
    public synchronized boolean unconfigure(ConfigManager confManager,AlertManager alertManager) {

        if(this.alertingConfig != null) {
            this.alertingConfig.removeNotifGroupMapping(this);
            this.alertingConfig = null;
        }

        if(this.notifGroup != null) {
            this.notifGroup.removeNotifGroupMapping(this);
            this.notifGroup = null;
        }

        return true;
    }

    @Override
    public synchronized boolean update(ConfigManager confManager,AlertManager alertManager, ConfigurableObject newConfig) {
        return ConfigurableObjectUtils.updateConfigurableObject((ConfDataObject) this, (ConfDataObject) newConfig);
    }

    
    public NotifGroup getNotifGroup() {
        return notifGroup;
    }
}
