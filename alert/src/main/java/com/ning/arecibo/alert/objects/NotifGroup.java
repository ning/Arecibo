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
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroup;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;
import com.ning.arecibo.util.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class NotifGroup extends ConfDataNotifGroup implements ConfigurableObject
{
    private static final Logger log = Logger.getLogger(NotifGroup.class);

    private final ConcurrentSkipListSet<NotifGroupMapping> notifGroupMappings;
    private final ConcurrentSkipListSet<NotifMapping> notifMappings;

    public NotifGroup()
    {

        this.notifGroupMappings = new ConcurrentSkipListSet<NotifGroupMapping>(ConfigurableObjectComparator.getInstance());
        this.notifMappings = new ConcurrentSkipListSet<NotifMapping>(ConfigurableObjectComparator.getInstance());
    }

    @Override
    public void toStringBuilder(StringBuilder sb)
    {
        super.toStringBuilder(sb);

        if (notifGroupMappings.size() > 0) {
            sb.append("    linked notifGroupMapping ids:\n");
            for (NotifGroupMapping notifGroupMapping : notifGroupMappings) {
                sb.append(String.format("        %s\n", notifGroupMapping.getLabel()));
            }
        }

        if (notifMappings.size() > 0) {
            sb.append("    linked notifMapping ids:\n");
            for (NotifMapping notifMapping : notifMappings) {
                sb.append(String.format("         %s\n", notifMapping.getLabel()));
            }
        }
    }

    @Override
    public boolean isValid(ConfigManager configManager)
    {
        if (this.enabled == null || !this.enabled) {
            return false;
        }

        return true;
    }

    @Override
    public boolean configure(ConfigManager confManager, AlertManager alertManager, LoggingManager loggingManager)
    {
        return true;
    }

    @Override
    public boolean unconfigure(ConfigManager confManager, AlertManager alertManager)
    {

        // unconfigure related objects
        for (NotifGroupMapping notifGroupMapping : notifGroupMappings) {
            notifGroupMapping.unconfigure(confManager, alertManager);
        }

        for (NotifMapping notifMapping : notifMappings) {
            notifMapping.unconfigure(confManager, alertManager);
        }

        return true;
    }

    @Override
    public synchronized boolean update(ConfigManager confManager, AlertManager alertManager, ConfigurableObject newConfig)
    {
        return ConfigurableObjectUtils.updateConfigurableObject((ConfDataObject) this, (ConfDataObject) newConfig);
    }


    public void addNotifGroupMapping(NotifGroupMapping notifGroupMapping)
    {
        if (!this.notifGroupMappings.contains(notifGroupMapping)) {
            this.notifGroupMappings.add(notifGroupMapping);
        }
    }

    public void removeNotifGroupMapping(NotifGroupMapping notifGroupMapping)
    {
        this.notifGroupMappings.remove(notifGroupMapping);
    }

    public Set<NotifGroupMapping> getNotifGroupMappings()
    {
        return this.notifGroupMappings;
    }


    public void addNotificationMapping(NotifMapping notifMapping)
    {
        if (!this.notifMappings.contains(notifMapping)) {
            this.notifMappings.add(notifMapping);
        }
    }

    public void removeNotificationMapping(NotifMapping notifMapping)
    {
        this.notifMappings.remove(notifMapping);
    }

    public Set<NotifMapping> getNotificationMappings()
    {
        return this.notifMappings;
    }

    public boolean sendNotification(Notification notification)
    {

        // if this NotificationGroup is not enabled, it shouldn't have been even configured.
        // however, check here just in case
        if (!this.enabled) {
            return false;
        }

        boolean gotFailure = false;
        for (NotifMapping notifMapping : notifMappings) {
            NotifConfig notificationConfig = notifMapping.getNotifConfig();
            boolean success = notificationConfig.sendNotification(notification);
            if (success) {
                gotFailure = true;
            }
        }

        return !gotFailure;
    }
}
