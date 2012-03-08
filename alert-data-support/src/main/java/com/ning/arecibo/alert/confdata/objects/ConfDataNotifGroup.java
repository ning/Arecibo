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

public class ConfDataNotifGroup extends ConfDataObject
{
    public static final String TYPE_NAME = "notif_group";

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
