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

import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationGroupsModel
{
    private final List<NotificationGroup> notificationGroups = new ArrayList<NotificationGroup>();
    private final Iterable<Map<String, Object>> allNotifications;

    public static final class NotificationGroup
    {
        private final String groupName;
        private final Multimap<String, String> emails;
        private final String enabled;

        public NotificationGroup(final String groupName, final Multimap<String, String> emails, final String enabled)
        {
            this.groupName = groupName;
            this.emails = emails;
            this.enabled = enabled;
        }

        public String getGroupName()
        {
            return groupName;
        }

        public Multimap<String, String> getEmails()
        {
            return emails;
        }

        public String getEnabled()
        {
            return enabled;
        }
    }

    public NotificationGroupsModel(final Iterable<Map<String, Object>> existingNotificationGroups, final Map<String, Multimap<String, String>> emailsAndNotificationTypesForGroup, final Iterable<Map<String, Object>> allNotifications)
    {
        this.allNotifications = allNotifications;

        for (final Map<String, Object> group : existingNotificationGroups) {
            final String groupName = (String) group.get("label");
            final String enabled = group.get("enabled").toString().equals("1") ? "true" : "false";
            final NotificationGroup notificationGroup = new NotificationGroup(groupName, emailsAndNotificationTypesForGroup.get(groupName), enabled);
            notificationGroups.add(notificationGroup);
        }
    }

    public List<NotificationGroup> getNotificationGroups()
    {
        return notificationGroups;
    }

    public Iterable<Map<String, Object>> getAllNotifications()
    {
        return allNotifications;
    }
}
