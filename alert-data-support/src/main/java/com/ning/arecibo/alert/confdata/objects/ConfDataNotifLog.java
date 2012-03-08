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

import com.ning.arecibo.util.Logger;

import java.sql.Timestamp;
import java.util.Map;

public class ConfDataNotifLog extends ConfDataObject
{
    private static final Logger log = Logger.getLogger(ConfDataNotifLog.class);

    public static final String TYPE_NAME = "notif_log";
    public static final String INSERT_TEMPLATE_NAME = ":insert_notif_log";
    public static final String UPDATE_TEMPLATE_NAME = ":update_notif_log";

    private static final String ALERT_INCIDENT_ID_FIELD = "alert_incident_id";
    private static final String ALERT_NOTIF_CONFIG_ID_FIELD = "alert_notif_config_id";
    private static final String NOTIF_TIME_FIELD = "notif_time";

    protected volatile Long alertIncidentId = null;
    protected volatile Long alertNotifConfigId = null;
    protected volatile Timestamp notifTime = null;

    public ConfDataNotifLog()
    {
    }

    @Override
    public String getTypeName()
    {
        return TYPE_NAME;
    }

    @Override
    public String getInsertSqlTemplateName()
    {
        return INSERT_TEMPLATE_NAME;
    }

    @Override
    public String getUpdateSqlTemplateName()
    {
        return UPDATE_TEMPLATE_NAME;
    }

    @Override
    public void populatePropertiesFromMap(final Map<String, Object> map)
    {
        super.populatePropertiesFromMap(map);
        setAlertIncidentId(getLong(map, ALERT_INCIDENT_ID_FIELD));
        setAlertNotifConfigId(getLong(map, ALERT_NOTIF_CONFIG_ID_FIELD));
        setNotifTime(getTimestamp(map, NOTIF_TIME_FIELD));
    }

    @Override
    public Map<String, Object> toPropertiesMap()
    {
        final Map<String, Object> map = super.toPropertiesMap();
        setLong(map, ALERT_INCIDENT_ID_FIELD, getAlertIncidentId());
        setLong(map, ALERT_NOTIF_CONFIG_ID_FIELD, getAlertNotifConfigId());
        setTimestamp(map, NOTIF_TIME_FIELD, getNotifTime());

        return map;
    }

    @Override
    public void toStringBuilder(final StringBuilder sb)
    {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", ALERT_INCIDENT_ID_FIELD, getAlertIncidentId()));
        sb.append(String.format("   %s -> %s\n", ALERT_NOTIF_CONFIG_ID_FIELD, getAlertIncidentId()));
        sb.append(String.format("   %s -> %s\n", NOTIF_TIME_FIELD, getNotifTime()));
    }

    public Long getAlertIncidentId()
    {
        return alertIncidentId;
    }

    public void setAlertIncidentId(final Long alertIncidentId)
    {
        this.alertIncidentId = alertIncidentId;
    }

    public Long getAlertNotifConfigId()
    {
        return alertNotifConfigId;
    }

    public void setAlertNotifConfigId(final Long alertNotifConfigId)
    {
        this.alertNotifConfigId = alertNotifConfigId;
    }

    public Timestamp getNotifTime()
    {
        return notifTime;
    }

    public void setNotifTime(final Timestamp notifTime)
    {
        this.notifTime = notifTime;
    }
}
