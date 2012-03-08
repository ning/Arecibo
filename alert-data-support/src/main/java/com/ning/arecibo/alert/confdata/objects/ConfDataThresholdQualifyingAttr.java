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

public class ConfDataThresholdQualifyingAttr extends ConfDataObject
{
    private static final Logger log = Logger.getLogger(ConfDataThresholdQualifyingAttr.class);

    public static final String TYPE_NAME = "threshold_qualifying_attr";
    public static final String INSERT_TEMPLATE_NAME = ":insert_threshold_qualifying_attr";
    public static final String UPDATE_TEMPLATE_NAME = ":update_threshold_qualifying_attr";

    private static final String THRESHOLD_CONFIG_ID_FIELD = "threshold_config_id";
    private static final String ATTRIBUTE_TYPE_FIELD = "attribute_type";
    private static final String ATTRIBUTE_VALUE_FIELD = "attribute_value";

    protected volatile Long thresholdConfigId = null;
    protected volatile String attributeType = null;
    protected volatile String attributeValue = null;

    public ConfDataThresholdQualifyingAttr()
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
        setThresholdConfigId(getLong(map, THRESHOLD_CONFIG_ID_FIELD));
        setAttributeType(getString(map, ATTRIBUTE_TYPE_FIELD));
        setAttributeValue(getString(map, ATTRIBUTE_VALUE_FIELD));
    }

    @Override
    public Map<String, Object> toPropertiesMap()
    {
        final Map<String, Object> map = super.toPropertiesMap();
        setLong(map, THRESHOLD_CONFIG_ID_FIELD, getThresholdConfigId());
        setString(map, ATTRIBUTE_TYPE_FIELD, getAttributeType());
        setString(map, ATTRIBUTE_VALUE_FIELD, getAttributeValue());

        return map;
    }

    @Override
    public void toStringBuilder(final StringBuilder sb)
    {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", THRESHOLD_CONFIG_ID_FIELD, getThresholdConfigId()));
        sb.append(String.format("   %s -> %s\n", ATTRIBUTE_TYPE_FIELD, getAttributeType()));
        sb.append(String.format("   %s -> %s\n", ATTRIBUTE_VALUE_FIELD, getAttributeValue()));
    }

    public Long getThresholdConfigId()
    {
        return thresholdConfigId;
    }

    public void setThresholdConfigId(final Long thresholdConfigId)
    {
        this.thresholdConfigId = thresholdConfigId;
    }

    public String getAttributeType()
    {
        return attributeType;
    }

    public void setAttributeType(final String attributeType)
    {
        this.attributeType = attributeType;
    }

    public String getAttributeValue()
    {
        return attributeValue;
    }

    public void setAttributeValue(final String attributeValue)
    {
        this.attributeValue = attributeValue;
    }
}
