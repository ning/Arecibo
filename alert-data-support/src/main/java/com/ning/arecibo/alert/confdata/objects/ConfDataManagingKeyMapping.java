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

public class ConfDataManagingKeyMapping extends ConfDataObject
{
    private static final Logger log = Logger.getLogger(ConfDataManagingKeyMapping.class);

    public static final String TYPE_NAME = "managing_key_mapping";
    public static final String INSERT_TEMPLATE_NAME = ":insert_managing_key_mapping";
    public static final String UPDATE_TEMPLATE_NAME = ":update_managing_key_mapping";

    private static final String ALERTING_CONFIG_ID_FIELD = "alerting_config_id";
    private static final String MANAGING_KEY_ID_FIELD = "managing_key_id";

    protected volatile Long alertingConfigId = null;
    protected volatile Long managingKeyId = null;

    public ConfDataManagingKeyMapping()
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
        setAlertingConfigId(getLong(map, ALERTING_CONFIG_ID_FIELD));
        setManagingKeyId(getLong(map, MANAGING_KEY_ID_FIELD));
    }

    @Override
    public Map<String, Object> toPropertiesMap()
    {
        final Map<String, Object> map = super.toPropertiesMap();
        setLong(map, ALERTING_CONFIG_ID_FIELD, getAlertingConfigId());
        setLong(map, MANAGING_KEY_ID_FIELD, getManagingKeyId());

        return map;
    }

    @Override
    public void toStringBuilder(final StringBuilder sb)
    {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", ALERTING_CONFIG_ID_FIELD, getAlertingConfigId()));
        sb.append(String.format("   %s -> %s\n", MANAGING_KEY_ID_FIELD, getManagingKeyId()));
    }

    public Long getAlertingConfigId()
    {
        return alertingConfigId;
    }

    public void setAlertingConfigId(final Long alertingConfigId)
    {
        this.alertingConfigId = alertingConfigId;
    }

    public Long getManagingKeyId()
    {
        return managingKeyId;
    }

    public void setManagingKeyId(final Long managingKeyId)
    {
        this.managingKeyId = managingKeyId;
    }
}
