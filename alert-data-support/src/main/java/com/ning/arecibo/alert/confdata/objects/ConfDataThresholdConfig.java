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

public class ConfDataThresholdConfig extends ConfDataObject
{
    private static final Logger log = Logger.getLogger(ConfDataThresholdConfig.class);

    public static final String TYPE_NAME = "threshold_config";
    public static final String INSERT_TEMPLATE_NAME = ":insert_threshold_config";
    public static final String UPDATE_TEMPLATE_NAME = ":update_threshold_config";

    private static final String ALERTING_CONFIG_ID_FIELD = "alerting_config_id";
    private static final String MONITORED_EVENT_TYPE_FIELD = "monitored_event_type";
    private static final String MONITORED_ATTRIBUTE_TYPE_FIELD = "monitored_attribute_type";
    private static final String CLEARING_INTERVAL_MS_FIELD = "clearing_interval_ms";
    private static final String MIN_THRESHOLD_VALUE_FIELD = "min_threshold_value";
    private static final String MAX_THRESHOLD_VALUE_FIELD = "max_threshold_value";
    private static final String MIN_THRESHOLD_SAMPLES_FIELD = "min_threshold_samples";
    private static final String MAX_SAMPLE_WINDOW_MS_FIELD = "max_sample_window_ms";

    protected volatile Long alertingConfigId = null;
    protected volatile String monitoredEventType = null;
    protected volatile String monitoredAttributeType = null;
    protected volatile Long clearingIntervalMs = null;
    protected volatile Double minThresholdValue = null;
    protected volatile Double maxThresholdValue = null;
    protected volatile Long minThresholdSamples = null;
    protected volatile Long maxSampleWindowMs = null;

    public ConfDataThresholdConfig()
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
        setMonitoredEventType(getString(map, MONITORED_EVENT_TYPE_FIELD));
        setMonitoredAttributeType(getString(map, MONITORED_ATTRIBUTE_TYPE_FIELD));
        setClearingIntervalMs(getLong(map, CLEARING_INTERVAL_MS_FIELD));
        setMinThresholdValue(getDouble(map, MIN_THRESHOLD_VALUE_FIELD));
        setMaxThresholdValue(getDouble(map, MAX_THRESHOLD_VALUE_FIELD));
        setMinThresholdSamples(getLong(map, MIN_THRESHOLD_SAMPLES_FIELD));
        setMaxSampleWindowMs(getLong(map, MAX_SAMPLE_WINDOW_MS_FIELD));
    }

    @Override
    public Map<String, Object> toPropertiesMap()
    {
        final Map<String, Object> map = super.toPropertiesMap();
        setLong(map, ALERTING_CONFIG_ID_FIELD, getAlertingConfigId());
        setString(map, MONITORED_EVENT_TYPE_FIELD, getMonitoredEventType());
        setString(map, MONITORED_ATTRIBUTE_TYPE_FIELD, getMonitoredAttributeType());
        setLong(map, CLEARING_INTERVAL_MS_FIELD, getClearingIntervalMs());
        setDouble(map, MIN_THRESHOLD_VALUE_FIELD, getMinThresholdValue());
        setDouble(map, MAX_THRESHOLD_VALUE_FIELD, getMaxThresholdValue());
        setLong(map, MIN_THRESHOLD_SAMPLES_FIELD, getMinThresholdSamples());
        setLong(map, MAX_SAMPLE_WINDOW_MS_FIELD, getMaxSampleWindowMs());

        return map;
    }

    @Override
    public void toStringBuilder(final StringBuilder sb)
    {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", ALERTING_CONFIG_ID_FIELD, getAlertingConfigId()));
        sb.append(String.format("   %s -> %s\n", MONITORED_EVENT_TYPE_FIELD, getMonitoredEventType()));
        sb.append(String.format("   %s -> %s\n", MONITORED_ATTRIBUTE_TYPE_FIELD, getMonitoredAttributeType()));
        sb.append(String.format("   %s -> %s\n", CLEARING_INTERVAL_MS_FIELD, getClearingIntervalMs()));
        sb.append(String.format("   %s -> %s\n", MIN_THRESHOLD_VALUE_FIELD, getMinThresholdValue()));
        sb.append(String.format("   %s -> %s\n", MAX_THRESHOLD_VALUE_FIELD, getMaxThresholdValue()));
        sb.append(String.format("   %s -> %s\n", MIN_THRESHOLD_SAMPLES_FIELD, getMinThresholdSamples()));
        sb.append(String.format("   %s -> %s\n", MAX_SAMPLE_WINDOW_MS_FIELD, getMaxSampleWindowMs()));
    }

    public Long getAlertingConfigId()
    {
        return alertingConfigId;
    }

    public void setAlertingConfigId(final Long alertingConfigId)
    {
        this.alertingConfigId = alertingConfigId;
    }

    public String getMonitoredEventType()
    {
        return monitoredEventType;
    }

    public void setMonitoredEventType(final String monitoredEventType)
    {
        this.monitoredEventType = monitoredEventType;
    }

    public String getMonitoredAttributeType()
    {
        return monitoredAttributeType;
    }

    public void setMonitoredAttributeType(final String monitoredAttributeType)
    {
        this.monitoredAttributeType = monitoredAttributeType;
    }

    public Long getClearingIntervalMs()
    {
        return clearingIntervalMs;
    }

    public void setClearingIntervalMs(final Long clearingIntervalMs)
    {
        this.clearingIntervalMs = clearingIntervalMs;
    }

    public Double getMinThresholdValue()
    {
        return minThresholdValue;
    }

    public void setMinThresholdValue(final Double minThresholdValue)
    {
        this.minThresholdValue = minThresholdValue;
    }

    public Double getMaxThresholdValue()
    {
        return maxThresholdValue;
    }

    public void setMaxThresholdValue(final Double maxThresholdValue)
    {
        this.maxThresholdValue = maxThresholdValue;
    }

    public Long getMinThresholdSamples()
    {
        return minThresholdSamples;
    }

    public void setMinThresholdSamples(final Long minThresholdSamples)
    {
        this.minThresholdSamples = minThresholdSamples;
    }

    public Long getMaxSampleWindowMs()
    {
        return maxSampleWindowMs;
    }

    public void setMaxSampleWindowMs(final Long maxSampleWindowMs)
    {
        this.maxSampleWindowMs = maxSampleWindowMs;
    }
}
