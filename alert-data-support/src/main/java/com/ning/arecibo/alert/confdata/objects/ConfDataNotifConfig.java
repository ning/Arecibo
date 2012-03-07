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

import com.ning.arecibo.alert.confdata.enums.NotificationType;
import com.ning.arecibo.util.Logger;

import java.util.Map;


public class ConfDataNotifConfig extends ConfDataObject
{
    private static final Logger log = Logger.getLogger(ConfDataNotifConfig.class);

    public static final String TYPE_NAME = "notif_config";
    public static final String INSERT_TEMPLATE_NAME = ":insert_notif_config";
    public static final String UPDATE_TEMPLATE_NAME = ":update_notif_config";

    private static final String PERSON_ID_FIELD = "person_id";
    private static final String NOTIF_TYPE_FIELD = "notif_type";
    private static final String ADDRESS_FIELD = "address";

    protected volatile Long personId = null;
    protected volatile NotificationType notifType = null;
    protected volatile String address = null;

    public ConfDataNotifConfig()
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
    public void setPropertiesFromMap(final Map<String, Object> map)
    {
        super.setPropertiesFromMap(map);
        setPersonId(getLong(map, PERSON_ID_FIELD));
        setNotifType(getEnum(map, NOTIF_TYPE_FIELD, NotificationType.class));
        setAddress(getString(map, ADDRESS_FIELD));
    }

    @Override
    public Map<String, Object> toPropertiesMap()
    {
        final Map<String, Object> map = super.toPropertiesMap();
        setLong(map, PERSON_ID_FIELD, getPersonId());
        setEnum(map, NOTIF_TYPE_FIELD, getNotifType());
        setString(map, ADDRESS_FIELD, getAddress());

        return map;
    }

    @Override
    public void toStringBuilder(final StringBuilder sb)
    {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", PERSON_ID_FIELD, getPersonId()));
        sb.append(String.format("   %s -> %s\n", NOTIF_TYPE_FIELD, getNotifType()));
        sb.append(String.format("   %s -> %s\n", ADDRESS_FIELD, getAddress()));
    }

    public Long getPersonId()
    {
        return personId;
    }

    public void setPersonId(final Long personId)
    {
        this.personId = personId;
    }

    public NotificationType getNotifType()
    {
        return notifType;
    }

    public void setNotifType(final NotificationType notifType)
    {
        this.notifType = notifType;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(final String address)
    {
        this.address = address;
    }
}
