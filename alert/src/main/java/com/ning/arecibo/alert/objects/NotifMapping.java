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
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifMapping;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;
import com.ning.arecibo.util.Logger;

public class NotifMapping extends ConfDataNotifMapping implements ConfigurableObject
{
    private static final Logger log = Logger.getLogger(NotifMapping.class);

    private volatile NotifConfig notifConfig = null;
    private volatile NotifGroup notifGroup = null;

    public NotifMapping()
    {
    }

    @Override
    public synchronized boolean isValid(ConfigManager confManager)
    {
        // make sure our notifGroup exists in the conf, and is valid
        if (!ConfigurableObjectUtils.checkNonNullAndValid(confManager.getNotifGroup(this.notifGroupId), confManager)) {
            return false;
        }

        // make sure our notif config exists in the conf, and is valid
        if (!ConfigurableObjectUtils.checkNonNullAndValid(confManager.getNotifConfig(this.notifConfigId), confManager)) {
            return false;
        }

        return true;
    }

    @Override
    public synchronized boolean configure(ConfigManager confManager, AlertManager alertManager, LoggingManager loggingManager)
    {

        this.notifConfig = confManager.getNotifConfig(this.notifConfigId);
        if (!ConfigurableObjectUtils.checkNonNullAndLog(this.notifConfig, this.notifConfigId, "notifConfig", confManager)) {
            return false;
        }

        this.notifGroup = confManager.getNotifGroup(this.notifGroupId);
        if (!ConfigurableObjectUtils.checkNonNullAndLog(this.notifGroup, this.notifGroupId, "notifGroup", confManager)) {
            return false;
        }


        this.notifConfig.addNotificationMapping(this);
        this.notifGroup.addNotificationMapping(this);

        return true;
    }

    @Override
    public synchronized boolean unconfigure(ConfigManager confManager, AlertManager alertManager)
    {

        if (this.notifConfig != null) {
            this.notifConfig.removeNotificationMapping(this);
            this.notifConfig = null;
        }


        if (this.notifGroup != null) {
            this.notifGroup.removeNotificationMapping(this);
            this.notifGroup = null;
        }

        return true;
    }

    @Override
    public synchronized boolean update(ConfigManager confManager, AlertManager alertManager, ConfigurableObject newConfig)
    {
        return ConfigurableObjectUtils.updateConfigurableObject((ConfDataObject) this, (ConfDataObject) newConfig);
    }


    public NotifConfig getNotifConfig()
    {
        return notifConfig;
    }
}
