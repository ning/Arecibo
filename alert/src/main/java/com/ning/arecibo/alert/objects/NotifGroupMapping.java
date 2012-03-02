/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
