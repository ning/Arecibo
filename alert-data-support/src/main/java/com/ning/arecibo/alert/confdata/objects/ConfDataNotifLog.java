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

package com.ning.arecibo.alert.confdata.objects;

import java.util.Map;
import java.sql.Timestamp;

import com.ning.arecibo.util.Logger;

public class ConfDataNotifLog extends ConfDataObject
{
    private final static Logger log = Logger.getLogger(ConfDataNotifLog.class);

    public final static String TYPE_NAME = "notif_log";
    public final static String INSERT_TEMPLATE_NAME = ":insert_notif_log";
    public final static String UPDATE_TEMPLATE_NAME = ":update_notif_log";

    private final static String ALERT_INCIDENT_ID_FIELD = "alert_incident_id";
    private final static String ALERT_NOTIF_CONFIG_ID_FIELD = "alert_notif_config_id";
    private final static String NOTIF_TIME_FIELD = "notif_time";

    protected volatile Long alertIncidentId = null;
    protected volatile Long alertNotifConfigId = null;
    protected volatile Timestamp notifTime = null;

    public ConfDataNotifLog() {}

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getInsertSqlTemplateName() {
        return INSERT_TEMPLATE_NAME;
    }

    @Override
    public String getUpdateSqlTemplateName() {
        return UPDATE_TEMPLATE_NAME;
    }

    @Override
    public void setPropertiesFromMap(Map<String,Object> map) {
        super.setPropertiesFromMap(map);
        setAlertIncidentId(getLong(map,ALERT_INCIDENT_ID_FIELD));
        setAlertNotifConfigId(getLong(map,ALERT_NOTIF_CONFIG_ID_FIELD));
        setNotifTime(getTimestamp(map, NOTIF_TIME_FIELD));
    }

    @Override
    public Map<String,Object> toPropertiesMap() {
        Map<String,Object> map = super.toPropertiesMap();
        setLong(map,ALERT_INCIDENT_ID_FIELD,getAlertIncidentId());
        setLong(map,ALERT_NOTIF_CONFIG_ID_FIELD,getAlertNotifConfigId());
        setTimestamp(map, NOTIF_TIME_FIELD, getNotifTime());

        return map;
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n",ALERT_INCIDENT_ID_FIELD,getAlertIncidentId()));
        sb.append(String.format("   %s -> %s\n",ALERT_NOTIF_CONFIG_ID_FIELD,getAlertIncidentId()));
        sb.append(String.format("   %s -> %s\n", NOTIF_TIME_FIELD, getNotifTime()));
    }

    public Long getAlertIncidentId() {
        return alertIncidentId;
    }

    public void setAlertIncidentId(Long alertIncidentId) {
        this.alertIncidentId = alertIncidentId;
    }

    public Long getAlertNotifConfigId() {
        return alertNotifConfigId;
    }

    public void setAlertNotifConfigId(Long alertNotifConfigId) {
        this.alertNotifConfigId = alertNotifConfigId;
    }

    public Timestamp getNotifTime() {
        return notifTime;
    }

    public void setNotifTime(Timestamp notifTime) {
        this.notifTime = notifTime;
    }
}
