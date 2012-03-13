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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AlertingConfigurationsModel
{
    private final List<AlertingConfiguration> alertingConfigurations = new ArrayList<AlertingConfiguration>();
    private final Iterable<Map<String, Object>> allNotificationGroups;

    public static final class AlertingConfiguration
    {
        private final String alertingConfigurationName;
        private final String repeatMode;
        private final String repeatInterval;
        private final String notifyOnRecovery;
        private final Iterable<String> notificationGroups;
        private final String enabled;

        public AlertingConfiguration(final String alertingConfigurationName, final String enabled, final Iterable<String> notificationGroups,
                                     final String notifyOnRecovery, final String repeatInterval, final String repeatMode)
        {
            this.alertingConfigurationName = alertingConfigurationName;
            this.enabled = enabled;
            this.notificationGroups = notificationGroups;
            this.notifyOnRecovery = notifyOnRecovery;
            this.repeatInterval = repeatInterval;
            this.repeatMode = repeatMode;
        }

        public String getAlertingConfigurationName()
        {
            return alertingConfigurationName;
        }

        public String getEnabled()
        {
            return enabled;
        }

        public Iterable<String> getNotificationGroups()
        {
            return notificationGroups;
        }

        public String getNotifyOnRecovery()
        {
            return notifyOnRecovery;
        }

        public String getRepeatInterval()
        {
            return repeatInterval;
        }

        public String getRepeatMode()
        {
            return repeatMode;
        }
    }

    public AlertingConfigurationsModel(final Iterable<Map<String, Object>> alertingConfigurations, final Map<String, List<Map<String, Object>>> notificationsGroupsForAlertingConfig, final Iterable<Map<String, Object>> allNotificationGroups)
    {
        this.allNotificationGroups = allNotificationGroups;

        for (final Map<String, Object> person : alertingConfigurations) {
            final String alertingConfigurationName = (String) person.get("label");
            final Set<String> emails = new HashSet<String>();

            final List<Map<String, Object>> notificationGroups = notificationsGroupsForAlertingConfig.get(alertingConfigurationName);
            for (final Map<String, Object> notification : notificationGroups) {
                emails.add((String) notification.get("address"));
            }

            final AlertingConfiguration alertingConfiguration = new AlertingConfiguration(
                alertingConfigurationName,
                ModelUtils.toString(person.get("enabled")),
                emails,
                ModelUtils.toString(person.get("notify_on_recovery")),
                ModelUtils.toString(person.get("repeat_interval")),
                ModelUtils.toString(person.get("repeat_mode"))
            );
            this.alertingConfigurations.add(alertingConfiguration);
        }
    }

    public List<AlertingConfiguration> getAlertingConfigurations()
    {
        return alertingConfigurations;
    }


    public Iterable<Map<String, Object>> getAllNotificationGroups()
    {
        return allNotificationGroups;
    }
}
