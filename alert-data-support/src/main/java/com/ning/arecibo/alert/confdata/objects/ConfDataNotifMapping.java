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

public class ConfDataNotifMapping extends ConfDataObject
{
    public static final String TYPE_NAME = "notif_mapping";

    private static final String NOTIF_GROUP_ID_FIELD = "notif_group_id";
    private static final String NOTIF_CONFIG_ID_FIELD = "notif_config_id";

    protected volatile Long notifGroupId = null;
    protected volatile Long notifConfigId = null;

    public ConfDataNotifMapping()
    {
    }

    @Override
    public String getTypeName()
    {
        return TYPE_NAME;
    }

    @Override
    public void populatePropertiesFromMap(final Map<String, Object> map)
    {
        super.populatePropertiesFromMap(map);
        setNotifGroupId(getLong(map, NOTIF_GROUP_ID_FIELD));
        setNotifConfigId(getLong(map, NOTIF_CONFIG_ID_FIELD));
    }

    @Override
    public Map<String, Object> toPropertiesMap()
    {
        final Map<String, Object> map = super.toPropertiesMap();
        setLong(map, NOTIF_GROUP_ID_FIELD, getNotifGroupId());
        setLong(map, NOTIF_CONFIG_ID_FIELD, getNotifConfigId());

        return map;
    }

    @Override
    public void toStringBuilder(final StringBuilder sb)
    {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", NOTIF_GROUP_ID_FIELD, getNotifGroupId()));
        sb.append(String.format("   %s -> %s\n", NOTIF_CONFIG_ID_FIELD, getNotifConfigId()));
    }

    public Long getNotifGroupId()
    {
        return notifGroupId;
    }

    public void setNotifGroupId(final Long notifGroupId)
    {
        this.notifGroupId = notifGroupId;
    }

    public Long getNotifConfigId()
    {
        return notifConfigId;
    }

    public void setNotifConfigId(final Long notifConfigId)
    {
        this.notifConfigId = notifConfigId;
    }
}
