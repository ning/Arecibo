package com.ning.arecibo.alert.objects;

import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdContextAttr;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;
import com.ning.arecibo.util.Logger;




public class ThresholdContextAttr extends ConfDataThresholdContextAttr implements ConfigurableObject
{
    private final static Logger log = Logger.getLogger(ThresholdContextAttr.class);

    private volatile ThresholdConfig thresholdConfig = null;
 
    public ThresholdContextAttr() {
    }

    @Override
    public synchronized boolean isValid(ConfigManager confManager) {
    	
    	// make sure our thresholdAlertConfig exists in the conf, and is valid
        if (!ConfigurableObjectUtils.checkNonNullAndValid(confManager.getThresholdConfig(this.thresholdConfigId), confManager))
    		return false;
    	
    	return true;
    }    

    @Override
    public synchronized boolean configure(ConfigManager confManager,AlertManager alertManager,LoggingManager loggingManager) {

        this.thresholdConfig = confManager.getThresholdConfig(this.thresholdConfigId);
        if(!ConfigurableObjectUtils.checkNonNullAndLog(this.thresholdConfig,this.thresholdConfigId,"thresholdConfig",confManager))
            return false;

        this.thresholdConfig.addThresholdContextAttr(this);
        return true;
    }

    @Override
    public synchronized boolean unconfigure(ConfigManager confManager,AlertManager alertManager) {

        if(this.thresholdConfig != null) {
            this.thresholdConfig.removeThresholdContextAttr(this);
            this.thresholdConfig = null;
        }
        
        return true;
    }

    @Override
    public synchronized boolean update(ConfigManager confManager,AlertManager alertManager, ConfigurableObject newConfig) {
        return ConfigurableObjectUtils.updateConfigurableObject((ConfDataObject) this, (ConfDataObject) newConfig);
    }
}
