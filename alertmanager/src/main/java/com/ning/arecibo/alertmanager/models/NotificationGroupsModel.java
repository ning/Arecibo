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

import com.ning.arecibo.alert.confdata.NotifConfig;
import com.ning.arecibo.alert.confdata.NotifGroup;

import java.util.Map;

public class NotificationGroupsModel
{
    private final Iterable<NotifGroup> notificationGroups;
    private final Map<String, Iterable<NotifConfig>> emailsAndNotificationTypesForGroup;
    private final Iterable<NotifConfig> allNotifications;

    public NotificationGroupsModel(final Iterable<NotifGroup> notificationGroups, final Map<String, Iterable<NotifConfig>> emailsAndNotificationTypesForGroup, final Iterable<NotifConfig> allNotifications)
    {
        this.notificationGroups = notificationGroups;
        this.emailsAndNotificationTypesForGroup = emailsAndNotificationTypesForGroup;
        this.allNotifications = allNotifications;
    }

    public Iterable<NotifGroup> getNotificationGroups()
    {
        return notificationGroups;
    }

    public Map<String, Iterable<NotifConfig>> getEmailsAndNotificationTypesForGroup()
    {
        return emailsAndNotificationTypesForGroup;
    }

    public Iterable<NotifConfig> getAllNotifications()
    {
        return allNotifications;
    }
}
