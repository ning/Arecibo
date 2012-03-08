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

import java.util.Map;

public class ConfDataNotifGroup extends ConfDataObject
{
    private static final Logger log = Logger.getLogger(ConfDataNotifGroup.class);

    public static final String TYPE_NAME = "notif_group";
    public static final String INSERT_TEMPLATE_NAME = ":insert_notif_group";
    public static final String UPDATE_TEMPLATE_NAME = ":update_notif_group";

    private static final String ENABLED_FIELD = "enabled";

    protected volatile Boolean enabled = null;

    public ConfDataNotifGroup()
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
    public Map<String, Object> toPropertiesMap()
    {
        final Map<String, Object> map = super.toPropertiesMap();
        setBoolean(map, ENABLED_FIELD, getEnabled());

        return map;
    }

    @Override
    public void populatePropertiesFromMap(final Map<String, Object> map)
    {
        super.populatePropertiesFromMap(map);
        setEnabled(getBoolean(map, ENABLED_FIELD));
    }

    public Boolean getEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(final Boolean enabled)
    {
        this.enabled = enabled;
    }
}
