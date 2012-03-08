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

package com.ning.arecibo.alert.confdata.dao;

import com.ning.arecibo.alert.confdata.objects.ConfDataAcknowledgementLog;
import com.ning.arecibo.alert.confdata.objects.ConfDataAlertIncidentLog;
import com.ning.arecibo.alert.confdata.objects.ConfDataAlertingConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataLevelConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKey;
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKeyLog;
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKeyMapping;
import com.ning.arecibo.alert.confdata.objects.ConfDataMessagingDescription;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroup;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroupMapping;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifLog;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifMapping;
import com.ning.arecibo.alert.confdata.objects.ConfDataPerson;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdContextAttr;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdQualifyingAttr;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.stringtemplate.ExternalizedSqlViaStringTemplate3;

@ExternalizedSqlViaStringTemplate3()
public interface ConfDataQueries
{
    @SqlUpdate
    int insertConfDataAcknowledgementLog(@BindBean final ConfDataAcknowledgementLog acknowledgementLog);

    @SqlUpdate
    int updateConfDataAcknowledgementLog(@BindBean final ConfDataAcknowledgementLog acknowledgementLog);

    @SqlUpdate
    int deleteConfDataAcknowledgementLog(@BindBean final ConfDataAcknowledgementLog acknowledgementLog);

    @SqlUpdate
    int insertConfDataAlertingConfig(@BindBean final ConfDataAlertingConfig alertingConfig);

    @SqlUpdate
    int updateConfDataAlertingConfig(@BindBean final ConfDataAlertingConfig alertingConfig);

    @SqlUpdate
    int deleteConfDataAlertingConfig(@BindBean final ConfDataAlertingConfig alertingConfig);

    @SqlUpdate
    int insertConfDataNotifGroupMapping(@BindBean final ConfDataNotifGroupMapping notifGroupMapping);

    @SqlUpdate
    int updateConfDataNotifGroupMapping(@BindBean final ConfDataNotifGroupMapping notifGroupMapping);

    @SqlUpdate
    int deleteConfDataNotifGroupMapping(@BindBean final ConfDataNotifGroupMapping notifGroupMapping);

    @SqlUpdate
    int insertConfDataNotifGroup(@BindBean final ConfDataNotifGroup notifGroup);

    @SqlUpdate
    int updateConfDataNotifGroup(@BindBean final ConfDataNotifGroup notifGroup);

    @SqlUpdate
    int deleteConfDataNotifGroup(@BindBean final ConfDataNotifGroup notifGroup);

    @SqlUpdate
    int insertConfDataAlertIncidentLog(@BindBean final ConfDataAlertIncidentLog alertIncidentLog);

    @SqlUpdate
    int updateConfDataAlertIncidentLog(@BindBean final ConfDataAlertIncidentLog alertIncidentLog);

    @SqlUpdate
    int deleteConfDataAlertIncidentLog(@BindBean final ConfDataAlertIncidentLog alertIncidentLog);

    @SqlUpdate
    int insertConfDataManagingKeyMapping(@BindBean final ConfDataManagingKeyMapping managingKeyMapping);

    @SqlUpdate
    int updateConfDataManagingKeyMapping(@BindBean final ConfDataManagingKeyMapping managingKeyMapping);

    @SqlUpdate
    int deleteConfDataManagingKeyMapping(@BindBean final ConfDataManagingKeyMapping managingKeyMapping);

    @SqlUpdate
    int insertConfDataNotifConfig(@BindBean final ConfDataNotifConfig notifConfig);

    @SqlUpdate
    int updateConfDataNotifConfig(@BindBean final ConfDataNotifConfig notifConfig);

    @SqlUpdate
    int deleteConfDataNotifConfig(@BindBean final ConfDataNotifConfig notifConfig);

    @SqlUpdate
    int insertConfDataNotifMapping(@BindBean final ConfDataNotifMapping notifMapping);

    @SqlUpdate
    int updateConfDataNotifMapping(@BindBean final ConfDataNotifMapping notifMapping);

    @SqlUpdate
    int deleteConfDataNotifMapping(@BindBean final ConfDataNotifMapping notifMapping);

    @SqlUpdate
    int insertConfDataLevelConfig(@BindBean final ConfDataLevelConfig levelConfig);

    @SqlUpdate
    int updateConfDataLevelConfig(@BindBean final ConfDataLevelConfig levelConfig);

    @SqlUpdate
    int deleteConfDataLevelConfig(@BindBean final ConfDataLevelConfig levelConfig);

    @SqlUpdate
    int insertConfDataManagingKeyLog(@BindBean final ConfDataManagingKeyLog managingKeyLog);

    @SqlUpdate
    int updateConfDataManagingKeyLog(@BindBean final ConfDataManagingKeyLog managingKeyLog);

    @SqlUpdate
    int deleteConfDataManagingKeyLog(@BindBean final ConfDataManagingKeyLog managingKeyLog);

    @SqlUpdate
    int insertConfDataManagingKey(@BindBean final ConfDataManagingKey managingKey);

    @SqlUpdate
    int updateConfDataManagingKey(@BindBean final ConfDataManagingKey managingKey);

    @SqlUpdate
    int deleteConfDataManagingKey(@BindBean final ConfDataManagingKey managingKey);

    @SqlUpdate
    int insertConfDataMessagingDescription(@BindBean final ConfDataMessagingDescription messagingDescription);

    @SqlUpdate
    int updateConfDataMessagingDescription(@BindBean final ConfDataMessagingDescription messagingDescription);

    @SqlUpdate
    int deleteConfDataMessagingDescription(@BindBean final ConfDataMessagingDescription messagingDescription);

    @SqlUpdate
    int insertConfDataNotifLog(@BindBean final ConfDataNotifLog notifLog);

    @SqlUpdate
    int updateConfDataNotifLog(@BindBean final ConfDataNotifLog notifLog);

    @SqlUpdate
    int deleteConfDataNotifLog(@BindBean final ConfDataNotifLog notifLog);

    @SqlUpdate
    int insertConfDataPerson(@BindBean final ConfDataPerson person);

    @SqlUpdate
    int updateConfDataPerson(@BindBean final ConfDataPerson person);

    @SqlUpdate
    int deleteConfDataPerson(@BindBean final ConfDataPerson person);

    @SqlUpdate
    int insertConfDataThresholdConfig(@BindBean final ConfDataThresholdConfig thresholdConfig);

    @SqlUpdate
    int updateConfDataThresholdConfig(@BindBean final ConfDataThresholdConfig thresholdConfig);

    @SqlUpdate
    int deleteConfDataThresholdConfig(@BindBean final ConfDataThresholdConfig thresholdConfig);

    @SqlUpdate
    int insertConfDataThresholdContextAttr(@BindBean final ConfDataThresholdContextAttr thresholdContextAttr);

    @SqlUpdate
    int updateConfDataThresholdContextAttr(@BindBean final ConfDataThresholdContextAttr thresholdContextAttr);

    @SqlUpdate
    int deleteConfDataThresholdContextAttr(@BindBean final ConfDataThresholdContextAttr thresholdContextAttr);

    @SqlUpdate
    int insertConfDataThresholdQualifyingAttr(@BindBean final ConfDataThresholdQualifyingAttr thresholdQualifyingAttr);

    @SqlUpdate
    int updateConfDataThresholdQualifyingAttr(@BindBean final ConfDataThresholdQualifyingAttr thresholdQualifyingAttr);

    @SqlUpdate
    int deleteConfDataThresholdQualifyingAttr(@BindBean final ConfDataThresholdQualifyingAttr thresholdQualifyingAttr);
}
