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

package com.ning.arecibo.alertmanager.models;

import com.ning.arecibo.alert.confdata.AlertingConfig;
import com.ning.arecibo.alert.confdata.NotifGroup;

import java.util.Map;

public class AlertingConfigurationsModel
{
    private final Iterable<AlertingConfig> alertingConfigurations;
    private final Map<String, Iterable<NotifGroup>> notificationsGroupsForAlertingConfig;
    private final Iterable<NotifGroup> allNotificationGroups;

    public AlertingConfigurationsModel(final Iterable<AlertingConfig> alertingConfigurations, final Map<String, Iterable<NotifGroup>> notificationsGroupsForAlertingConfig, final Iterable<NotifGroup> allNotificationGroups)
    {
        this.alertingConfigurations = alertingConfigurations;
        this.notificationsGroupsForAlertingConfig = notificationsGroupsForAlertingConfig;
        this.allNotificationGroups = allNotificationGroups;
    }

    public Iterable<AlertingConfig> getAlertingConfigurations()
    {
        return alertingConfigurations;
    }

    public Map<String, Iterable<NotifGroup>> getNotificationsGroupsForAlertingConfig()
    {
        return notificationsGroupsForAlertingConfig;
    }

    public Iterable<NotifGroup> getAllNotificationGroups()
    {
        return allNotificationGroups;
    }
}
