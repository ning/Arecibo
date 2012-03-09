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

package com.ning.arecibo.alert.client;

import com.google.common.collect.Multimap;
import com.sun.jersey.api.client.UniformInterfaceException;

import java.util.Map;

// TODO Consider extracting the POJO objects from alert-data-support to share them
public interface AlertClient
{
    // People and Groups (Aliases)

    public int createPerson(final String firstName, final String lastName, final String nickName) throws UniformInterfaceException;

    public int createGroup(final String name);

    public Map<String, Object> findPersonOrGroupById(final int id) throws UniformInterfaceException;

    public void deletePersonOrGroupById(final int id) throws UniformInterfaceException;


    // Associated email addresses to People and Groups (SMS notifications limit the body to 140 characters)

    public int createEmailNotificationForPersonOrGroup(final int id, final String address);

    public int createSmsNotificationForPersonOrGroup(final int id, final String address);

    public Map<String, Object> findNotificationById(final int id) throws UniformInterfaceException;

    public Iterable<Map<String, Object>> findNotificationsForPersonOrGroupId(final int id) throws UniformInterfaceException;

    public void deleteNotificationById(int id) throws UniformInterfaceException;


    // Notification Groups, for creating groups of recipients for alert emails

    public int createNotificationGroup(final String groupName, boolean enabled, final Iterable<Integer> notificationsIds) throws UniformInterfaceException;

    public Multimap<String, String> findEmailsAndNotificationTypesForGroupById(final int id) throws UniformInterfaceException;


    // Alerting Configurations, which allow you to associate notification options with threshold definitions

    public int createAlertingConfig(final String name, final boolean repeatUntilCleared, final boolean notifyOnRecovery,
                                    final boolean enabled, final Iterable<Integer> notificationGroupsIds) throws UniformInterfaceException;
}