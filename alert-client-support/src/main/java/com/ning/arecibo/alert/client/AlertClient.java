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

import com.ning.arecibo.alert.confdata.AlertingConfig;
import com.ning.arecibo.alert.confdata.NotifConfig;
import com.ning.arecibo.alert.confdata.NotifGroup;
import com.ning.arecibo.alert.confdata.Person;
import com.ning.arecibo.alert.confdata.ThresholdContextAttr;
import com.ning.arecibo.alert.confdata.ThresholdDefinition;
import com.ning.arecibo.alert.confdata.ThresholdQualifyingAttr;
import com.sun.jersey.api.client.UniformInterfaceException;

import javax.annotation.Nullable;

// TODO Consider extracting the POJO objects from alert-data-support to share them
public interface AlertClient
{
    // People and Groups (Aliases)

    public int createPerson(final String firstName, final String lastName, final String nickName) throws UniformInterfaceException;

    public int createGroup(final String name);

    public Iterable<Person> findAllPeopleAndGroups() throws UniformInterfaceException;

    public Person findPersonOrGroupById(final int id) throws UniformInterfaceException;

    public void deletePersonOrGroupById(final int id) throws UniformInterfaceException;


    // Associated email addresses to People and Groups (SMS notifications limit the body to 140 characters)

    public int createEmailNotificationForPersonOrGroup(final int id, final String address);

    public int createSmsNotificationForPersonOrGroup(final int id, final String address);

    public Iterable<NotifConfig> findAllNotifications() throws UniformInterfaceException;

    public NotifConfig findNotificationById(final int id) throws UniformInterfaceException;

    public Iterable<NotifConfig> findNotificationsForPersonOrGroupId(final int id) throws UniformInterfaceException;

    public void deleteNotificationById(int id) throws UniformInterfaceException;


    // Notification Groups, for creating groups of recipients for alert emails

    public int createNotificationGroup(final String groupName, boolean enabled, final Iterable<Integer> notificationsIds) throws UniformInterfaceException;

    public Iterable<NotifGroup> findAllNotificationGroups() throws UniformInterfaceException;

    public Iterable<NotifConfig> findEmailsAndNotificationTypesForGroupById(final int id) throws UniformInterfaceException;


    // Alerting Configurations, which allow you to associate notification options with threshold definitions

    public int createAlertingConfig(final String name, final boolean repeatUntilCleared, final boolean notifyOnRecovery,
                                    final boolean enabled, final Iterable<Integer> notificationGroupsIds) throws UniformInterfaceException;

    public Iterable<AlertingConfig> findAllAlertingConfigurations() throws UniformInterfaceException;

    public AlertingConfig findAlertingConfigById(final Long id) throws UniformInterfaceException;

    public Iterable<NotifGroup> findNotificationGroupsForAlertingConfigById(final int id) throws UniformInterfaceException;

    //  Threshold Definitions, which is where you define the rules that will trigger alerting.

    public int createThresholdConfig(final String name, final String monitoredEventType, final String monitoredAttributeType,
                                     @Nullable final Double minThresholdValue, @Nullable final Double maxThresholdValue,
                                     final Long minThresholdSamples, final Long maxSampleWindowMs,
                                     final Long clearingIntervalMs, final int alertingConfigId) throws UniformInterfaceException;

    public int createThresholdQualifyingAttr(final int thresholdConfigId, final String attributeType, final String attributeValue) throws UniformInterfaceException;

    public int createThresholdContextAttr(final int thresholdConfigId, final String attributeType) throws UniformInterfaceException;

    public Iterable<ThresholdDefinition> findAllThresholdConfigs() throws UniformInterfaceException;

    public Iterable<ThresholdQualifyingAttr> findThresholdQualifyingAttrsForThresholdId(final int thresholdConfigId) throws UniformInterfaceException;

    public Iterable<ThresholdContextAttr> findThresholdContextAttrsForThresholdId(final int thresholdConfigId) throws UniformInterfaceException;
}