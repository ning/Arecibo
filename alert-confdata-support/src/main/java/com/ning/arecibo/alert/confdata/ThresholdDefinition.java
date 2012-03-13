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

package com.ning.arecibo.alert.confdata;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public final class ThresholdDefinition
{
    private final String thresholdDefinitionName;
    private final String monitoredEventType;
    private final String monitoredAttributeType;
    private final Long clearingIntervalMs;
    private final Double minThresholdValue;
    private final Double maxThresholdValue;
    private final Long minThresholdSamples;
    private final Long maxSampleWindowMs;
    private final Long alertingConfigurationId;
    private final Integer id;

    @JsonCreator
    public ThresholdDefinition(@JsonProperty("label") final String thresholdDefinitionName,
                               @JsonProperty("monitored_event_type") final String monitoredEventType,
                               @JsonProperty("monitored_attribute_type") final String monitoredAttributeType,
                               @JsonProperty("clearing_interval_ms") final Long clearingIntervalMs,
                               @JsonProperty("min_threshold_value") final Double minThresholdValue,
                               @JsonProperty("max_threshold_value") final Double maxThresholdValue,
                               @JsonProperty("min_threshold_samples") final Long minThresholdSamples,
                               @JsonProperty("max_sample_window_ms") final Long maxSampleWindowMs,
                               @JsonProperty("alerting_config_id") final Long alertingConfigurationId,
                               @JsonProperty("id") final Integer id)
    {
        this.thresholdDefinitionName = thresholdDefinitionName;
        this.alertingConfigurationId = alertingConfigurationId;
        this.clearingIntervalMs = clearingIntervalMs;
        this.maxSampleWindowMs = maxSampleWindowMs;
        this.maxThresholdValue = maxThresholdValue;
        this.minThresholdSamples = minThresholdSamples;
        this.minThresholdValue = minThresholdValue;
        this.monitoredAttributeType = monitoredAttributeType;
        this.monitoredEventType = monitoredEventType;
        this.id = id;
    }

    public Long getAlertingConfigurationId()
    {
        return alertingConfigurationId;
    }

    public Long getClearingIntervalMs()
    {
        return clearingIntervalMs;
    }

    public Long getMaxSampleWindowMs()
    {
        return maxSampleWindowMs;
    }

    public Double getMaxThresholdValue()
    {
        return maxThresholdValue;
    }

    public Long getMinThresholdSamples()
    {
        return minThresholdSamples;
    }

    public Double getMinThresholdValue()
    {
        return minThresholdValue;
    }

    public String getMonitoredAttributeType()
    {
        return monitoredAttributeType;
    }

    public String getMonitoredEventType()
    {
        return monitoredEventType;
    }

    public String getThresholdDefinitionName()
    {
        return thresholdDefinitionName;
    }

    public Integer getId()
    {
        return id;
    }
}