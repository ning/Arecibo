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

import com.ning.arecibo.alert.confdata.enums.NotificationRepeatMode;
import com.ning.arecibo.util.Logger;

import java.util.Map;

public class ConfDataAlertingConfig extends ConfDataObject
{
    private static final Logger log = Logger.getLogger(ConfDataAlertingConfig.class);

    public static final String TYPE_NAME = "alerting_config";
    public static final String INSERT_TEMPLATE_NAME = ":insert_alerting_config";
    public static final String UPDATE_TEMPLATE_NAME = ":update_alerting_config";

    private static final String PARENT_CONFIG_ID_FIELD = "parent_config_id";
    private static final String LEVEL_CONFIG_ID_FIELD = "level_config_id";
    private static final String STATUS_FIELD = "status";
    private static final String TYPE_FIELD = "type";
    private static final String ENABLED_FIELD = "enabled";
    private static final String NOTIF_REPEAT_MODE_FIELD = "notif_repeat_mode";
    private static final String NOTIF_REPEAT_INTERVAL_MS_FIELD = "notif_repeat_interval_ms";
    private static final String NOTIF_ON_RECOVERY_FIELD = "notif_on_recovery";

    protected volatile Long parentConfigId = null;
    protected volatile Long levelConfigId = null;
    protected volatile String status = null;
    protected volatile String type = null;
    protected volatile Boolean enabled = null;
    protected volatile NotificationRepeatMode notifRepeatMode = null;
    protected volatile Long notifRepeatIntervalMs = null;
    protected volatile Boolean notifOnRecovery = null;

    public ConfDataAlertingConfig()
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
        setParentConfigId(getLong(map, PARENT_CONFIG_ID_FIELD));
        setLevelConfigId(getLong(map, LEVEL_CONFIG_ID_FIELD));
        setStatus(getString(map, STATUS_FIELD));
        setType(getString(map, TYPE_FIELD));
        setEnabled(getBoolean(map, ENABLED_FIELD));
        setNotifRepeatMode(getEnum(map, NOTIF_REPEAT_MODE_FIELD, NotificationRepeatMode.class));
        setNotifRepeatIntervalMs(getLong(map, NOTIF_REPEAT_INTERVAL_MS_FIELD));
        setNotifOnRecovery(getBoolean(map, NOTIF_ON_RECOVERY_FIELD));
    }

    @Override
    public Map<String, Object> toPropertiesMap()
    {
        final Map<String, Object> map = super.toPropertiesMap();
        setLong(map, PARENT_CONFIG_ID_FIELD, getParentConfigId());
        setLong(map, LEVEL_CONFIG_ID_FIELD, getLevelConfigId());
        setString(map, STATUS_FIELD, getStatus());
        setString(map, TYPE_FIELD, getType());
        setBoolean(map, ENABLED_FIELD, getEnabled());
        setEnum(map, NOTIF_REPEAT_MODE_FIELD, getNotifRepeatMode());
        setLong(map, NOTIF_REPEAT_INTERVAL_MS_FIELD, getNotifRepeatIntervalMs());
        setBoolean(map, NOTIF_ON_RECOVERY_FIELD, getNotifOnRecovery());

        return map;
    }

    @Override
    public void toStringBuilder(final StringBuilder sb)
    {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", PARENT_CONFIG_ID_FIELD, getParentConfigId()));
        sb.append(String.format("   %s -> %s\n", LEVEL_CONFIG_ID_FIELD, getLevelConfigId()));
        sb.append(String.format("   %s -> %s\n", STATUS_FIELD, getStatus()));
        sb.append(String.format("   %s -> %s\n", TYPE_FIELD, getType()));
        sb.append(String.format("   %s -> %s\n", ENABLED_FIELD, getEnabled()));
        sb.append(String.format("   %s -> %s\n", NOTIF_REPEAT_MODE_FIELD, getNotifRepeatMode()));
        sb.append(String.format("   %s -> %s\n", NOTIF_REPEAT_INTERVAL_MS_FIELD, getNotifRepeatIntervalMs()));
        sb.append(String.format("   %s -> %s\n", NOTIF_ON_RECOVERY_FIELD, getNotifOnRecovery()));
    }

    public Boolean getEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(final Boolean enabled)
    {
        this.enabled = enabled;
    }

    public Long getParentConfigId()
    {
        return parentConfigId;
    }

    public void setParentConfigId(final Long parentConfigId)
    {
        this.parentConfigId = parentConfigId;
    }

    public Long getLevelConfigId()
    {
        return levelConfigId;
    }

    public void setLevelConfigId(final Long levelConfigId)
    {
        this.levelConfigId = levelConfigId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(final String status)
    {
        this.status = status;
    }

    public String getType()
    {
        return type;
    }

    public void setType(final String type)
    {
        this.type = type;
    }

    public NotificationRepeatMode getNotifRepeatMode()
    {
        return notifRepeatMode;
    }

    public void setNotifRepeatMode(final NotificationRepeatMode notifRepeatMode)
    {
        this.notifRepeatMode = notifRepeatMode;
    }

    public Long getNotifRepeatIntervalMs()
    {
        return notifRepeatIntervalMs;
    }

    public void setNotifRepeatIntervalMs(final Long notifRepeatIntervalMs)
    {
        this.notifRepeatIntervalMs = notifRepeatIntervalMs;
    }

    public Boolean getNotifOnRecovery()
    {
        return notifOnRecovery;
    }

    public void setNotifOnRecovery(final Boolean notifOnRecovery)
    {
        this.notifOnRecovery = notifOnRecovery;
    }
}
