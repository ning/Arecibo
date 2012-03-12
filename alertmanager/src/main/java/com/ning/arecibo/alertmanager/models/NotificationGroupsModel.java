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

import java.util.Map;

public class NotificationGroupsModel
{
    private final Iterable<Map<String, String>> existingNotificationGroups;
    private final Iterable<Map<String, Object>> allPeopleAndGroups;

    public NotificationGroupsModel(final Iterable<Map<String, String>> existingNotificationGroups, final Iterable<Map<String, Object>> allPeopleAndGroups)
    {
        this.existingNotificationGroups = existingNotificationGroups;
        this.allPeopleAndGroups = allPeopleAndGroups;
    }

    public Iterable<Map<String, String>> getExistingNotificationGroups()
    {
        return existingNotificationGroups;
    }

    public Iterable<Map<String, Object>> getAllPeopleAndGroups()
    {
        return allPeopleAndGroups;
    }
}
