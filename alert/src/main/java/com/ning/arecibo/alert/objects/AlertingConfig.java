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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.confdata.enums.NotificationRepeatMode;
import com.ning.arecibo.alert.confdata.objects.ConfDataAlertingConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;
import com.ning.arecibo.util.Logger;



public class AlertingConfig extends ConfDataAlertingConfig implements ConfigurableObject
{
    private final static Logger log = Logger.getLogger(ThresholdConfig.class);

    private final ConcurrentSkipListSet<ThresholdConfig> thresholdConfigs;
    private final ConcurrentSkipListSet<NotifGroupMapping> notifGroupMappings;
    private final ConcurrentSkipListSet<ManagingKeyMapping> managingKeyMappings;

    public AlertingConfig() {
        this.thresholdConfigs = new ConcurrentSkipListSet<ThresholdConfig>(ConfigurableObjectComparator.getInstance());
        this.notifGroupMappings = new ConcurrentSkipListSet<NotifGroupMapping>(ConfigurableObjectComparator.getInstance());
        this.managingKeyMappings = new ConcurrentSkipListSet<ManagingKeyMapping>(ConfigurableObjectComparator.getInstance());
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        super.toStringBuilder(sb);

        if(thresholdConfigs.size() > 0) {
            sb.append("    linked thresholdConfig ids:\n");
            for(ThresholdConfig thresholdConfig: thresholdConfigs) {
                sb.append(String.format("        %s\n",thresholdConfig.getLabel()));
            }
        }

        if(notifGroupMappings.size() > 0) {
            sb.append("    linked notifGroupMapping ids:\n");
            for(NotifGroupMapping notifGroupMapping: notifGroupMappings) {
                sb.append(String.format("        %s\n",notifGroupMapping.getLabel()));
            }
        }

        if(managingKeyMappings.size() > 0) {
            sb.append("    linked managingKeyMapping ids:\n");
            for(ManagingKeyMapping managingKeyMapping: managingKeyMappings) {
                sb.append(String.format("        %s\n",managingKeyMapping.getLabel()));
            }
        }
    }

    @Override
    public boolean isValid(ConfigManager confManager) {

        if(!this.getEnabled())
            return false;
        
    	return true;
    }

    @Override
    public boolean configure(ConfigManager confManager,AlertManager alertManager, LoggingManager loggingManager) {
        return true;
    }

    @Override
    public boolean unconfigure(ConfigManager confManager,AlertManager alertManager) {

        for(ThresholdConfig thresholdConfig: thresholdConfigs) {
            thresholdConfig.unconfigure(confManager, alertManager);
        }

        for(NotifGroupMapping notifGroupMapping: notifGroupMappings) {
            notifGroupMapping.unconfigure(confManager, alertManager);
        }

        for(ManagingKeyMapping managingKeyMapping: managingKeyMappings) {
            managingKeyMapping.unconfigure(confManager, alertManager);
        }

        return true;
    }

    @Override
    public synchronized boolean update(ConfigManager confManager,AlertManager alertManager, ConfigurableObject newConfig) {
        return ConfigurableObjectUtils.updateConfigurableObject((ConfDataObject)this,(ConfDataObject)newConfig);
    }



    public void addThresholdConfig(ThresholdConfig thresholdConfig) {
        if(!this.thresholdConfigs.contains(thresholdConfig))
            this.thresholdConfigs.add(thresholdConfig);
    }

    public void removeThresholdConfig(ThresholdConfig thresholdConfig) {
        this.thresholdConfigs.remove(thresholdConfig);
    }

    public Set<ThresholdConfig> getThresholdConfigs() {
        return this.thresholdConfigs;
    }



    public void addNotifGroupMapping(NotifGroupMapping notifGroupMapping) {
        if(!this.notifGroupMappings.contains(notifGroupMapping))
            this.notifGroupMappings.add(notifGroupMapping);
    }

    public void removeNotifGroupMapping(NotifGroupMapping notifGroupMapping) {
        this.notifGroupMappings.remove(notifGroupMapping);
    }

    public Set<NotifGroupMapping> getNotifGroupMappings() {
        return this.notifGroupMappings;
    }



    public void addManagingKeyMapping(ManagingKeyMapping managingKeyMapping) {
        if(!this.managingKeyMappings.contains(managingKeyMapping))
            this.managingKeyMappings.add(managingKeyMapping);
    }

    public void removeManagingKeyMapping(ManagingKeyMapping managingKeyMapping) {
        this.managingKeyMappings.remove(managingKeyMapping);
    }

    public Set<ManagingKeyMapping> getManagingKeyMappings() {
        return this.managingKeyMappings;
    }


    public List<ManagingKey> getCurrentManagingKeys(ConfigManager confManager) {

        ArrayList<ManagingKey> currentManagingKeys = new ArrayList<ManagingKey>();
        for(ManagingKeyMapping mapping:this.managingKeyMappings) {
            currentManagingKeys.add(confManager.getManagingKey(mapping.getManagingKeyId()));
        }

        return currentManagingKeys;
    }

    public synchronized Long getNotifRepeatIntervalMsIfEnabled() {
        if(this.getNotifRepeatMode() != null &&
                this.getNotifRepeatMode().equals(NotificationRepeatMode.UNTIL_CLEARED) &&
                this.getNotifRepeatIntervalMs() != null) {
            return this.getNotifRepeatIntervalMs();
        }
        else
            return null;
    }

    public boolean sendNotification(Notification notification) {

        // if this AlertingConfig is not enabled, it shouldn't have been even configured.
        // however, check here just in case
        if(!this.getEnabled()) {
            return false;
        }

        //TODO: Consult parentConfig, etc...

        boolean gotFailure = false;
        for (NotifGroupMapping groupMapping : notifGroupMappings) {
            NotifGroup notifGroup = groupMapping.getNotifGroup();
            boolean success = notifGroup.sendNotification(notification);
            if(!success)
                gotFailure = true;
        }

        return !gotFailure;
    }
}
