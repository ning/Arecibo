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
public interface NewDAO
{
    @SqlUpdate
    int insertAcknowledgementLog(@BindBean final ConfDataAcknowledgementLog acknowledgementLog);

    @SqlUpdate
    int updateAcknowledgementLog(@BindBean final ConfDataAcknowledgementLog acknowledgementLog);

    @SqlUpdate
    int insertAlertingConfig(@BindBean final ConfDataAlertingConfig alertingConfig);

    @SqlUpdate
    int updateAlertingConfig(@BindBean final ConfDataAlertingConfig alertingConfig);

    @SqlUpdate
    int insertNotifGroupMapping(@BindBean final ConfDataNotifGroupMapping notifGroupMapping);

    @SqlUpdate
    int updateNotifGroupMapping(@BindBean final ConfDataNotifGroupMapping notifGroupMapping);

    @SqlUpdate
    int insertNotifGroup(@BindBean final ConfDataNotifGroup notifGroup);

    @SqlUpdate
    int updateNotifGroup(@BindBean final ConfDataNotifGroup notifGroup);

    @SqlUpdate
    int insertAlertIncidentLog(@BindBean final ConfDataAlertIncidentLog alertIncidentLog);

    @SqlUpdate
    int updateAlertIncidentLog(@BindBean final ConfDataAlertIncidentLog alertIncidentLog);

    @SqlUpdate
    int insertManagingKeyMapping(@BindBean final ConfDataManagingKeyMapping managingKeyMapping);

    @SqlUpdate
    int updateManagingKeyMapping(@BindBean final ConfDataManagingKeyMapping managingKeyMapping);

    @SqlUpdate
    int insertNotifConfig(@BindBean final ConfDataNotifConfig notifConfig);

    @SqlUpdate
    int updateNotifConfig(@BindBean final ConfDataNotifConfig notifConfig);

    @SqlUpdate
    int insertNotifMapping(@BindBean final ConfDataNotifMapping notifMapping);

    @SqlUpdate
    int updateNotifMapping(@BindBean final ConfDataNotifMapping notifMapping);

    @SqlUpdate
    int insertLevelConfig(@BindBean final ConfDataLevelConfig levelConfig);

    @SqlUpdate
    int updateLevelConfig(@BindBean final ConfDataLevelConfig levelConfig);

    @SqlUpdate
    int insertManagingKeyLog(@BindBean final ConfDataManagingKeyLog managingKeyLog);

    @SqlUpdate
    int updateManagingKeyLog(@BindBean final ConfDataManagingKeyLog managingKeyLog);

    @SqlUpdate
    int insertManagingKey(@BindBean final ConfDataManagingKey managingKey);

    @SqlUpdate
    int updateManagingKey(@BindBean final ConfDataManagingKey managingKey);

    @SqlUpdate
    int insertMessagingDescription(@BindBean final ConfDataMessagingDescription messagingDescription);

    @SqlUpdate
    int updateMessagingDescription(@BindBean final ConfDataMessagingDescription messagingDescription);

    @SqlUpdate
    int insertNotifLog(@BindBean final ConfDataNotifLog notifLog);

    @SqlUpdate
    int updateNotifLog(@BindBean final ConfDataNotifLog notifLog);

    @SqlUpdate
    int insertPerson(@BindBean final ConfDataPerson person);

    @SqlUpdate
    int updatePerson(@BindBean final ConfDataPerson person);

    @SqlUpdate
    int insertThresholdConfig(@BindBean final ConfDataThresholdConfig thresholdConfig);

    @SqlUpdate
    int updateThresholdConfig(@BindBean final ConfDataThresholdConfig thresholdConfig);

    @SqlUpdate
    int insertThresholdContextAttr(@BindBean final ConfDataThresholdContextAttr thresholdContextAttr);

    @SqlUpdate
    int updateThresholdContextAttr(@BindBean final ConfDataThresholdContextAttr thresholdContextAttr);

    @SqlUpdate
    int insertThresholdQualifyingAttr(@BindBean final ConfDataThresholdQualifyingAttr thresholdQualifyingAttr);

    @SqlUpdate
    int updateThresholdQualifyingAttr(@BindBean final ConfDataThresholdQualifyingAttr thresholdQualifyingAttr);
}
