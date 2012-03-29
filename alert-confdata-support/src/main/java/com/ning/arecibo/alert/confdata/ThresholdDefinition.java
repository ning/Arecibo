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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
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
    private final Integer alertingConfigurationId;
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
                               @JsonProperty("alerting_config_id") final Integer alertingConfigurationId,
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

    public Integer getAlertingConfigurationId()
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

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThresholdDefinition");
        sb.append("{alertingConfigurationId=").append(alertingConfigurationId);
        sb.append(", thresholdDefinitionName='").append(thresholdDefinitionName).append('\'');
        sb.append(", monitoredEventType='").append(monitoredEventType).append('\'');
        sb.append(", monitoredAttributeType='").append(monitoredAttributeType).append('\'');
        sb.append(", clearingIntervalMs=").append(clearingIntervalMs);
        sb.append(", minThresholdValue=").append(minThresholdValue);
        sb.append(", maxThresholdValue=").append(maxThresholdValue);
        sb.append(", minThresholdSamples=").append(minThresholdSamples);
        sb.append(", maxSampleWindowMs=").append(maxSampleWindowMs);
        sb.append(", id=").append(id);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ThresholdDefinition that = (ThresholdDefinition) o;

        if (alertingConfigurationId != null ? !alertingConfigurationId.equals(that.alertingConfigurationId) : that.alertingConfigurationId != null) {
            return false;
        }
        if (clearingIntervalMs != null ? !clearingIntervalMs.equals(that.clearingIntervalMs) : that.clearingIntervalMs != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (maxSampleWindowMs != null ? !maxSampleWindowMs.equals(that.maxSampleWindowMs) : that.maxSampleWindowMs != null) {
            return false;
        }
        if (maxThresholdValue != null ? !maxThresholdValue.equals(that.maxThresholdValue) : that.maxThresholdValue != null) {
            return false;
        }
        if (minThresholdSamples != null ? !minThresholdSamples.equals(that.minThresholdSamples) : that.minThresholdSamples != null) {
            return false;
        }
        if (minThresholdValue != null ? !minThresholdValue.equals(that.minThresholdValue) : that.minThresholdValue != null) {
            return false;
        }
        if (monitoredAttributeType != null ? !monitoredAttributeType.equals(that.monitoredAttributeType) : that.monitoredAttributeType != null) {
            return false;
        }
        if (monitoredEventType != null ? !monitoredEventType.equals(that.monitoredEventType) : that.monitoredEventType != null) {
            return false;
        }
        if (thresholdDefinitionName != null ? !thresholdDefinitionName.equals(that.thresholdDefinitionName) : that.thresholdDefinitionName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = thresholdDefinitionName != null ? thresholdDefinitionName.hashCode() : 0;
        result = 31 * result + (monitoredEventType != null ? monitoredEventType.hashCode() : 0);
        result = 31 * result + (monitoredAttributeType != null ? monitoredAttributeType.hashCode() : 0);
        result = 31 * result + (clearingIntervalMs != null ? clearingIntervalMs.hashCode() : 0);
        result = 31 * result + (minThresholdValue != null ? minThresholdValue.hashCode() : 0);
        result = 31 * result + (maxThresholdValue != null ? maxThresholdValue.hashCode() : 0);
        result = 31 * result + (minThresholdSamples != null ? minThresholdSamples.hashCode() : 0);
        result = 31 * result + (maxSampleWindowMs != null ? maxSampleWindowMs.hashCode() : 0);
        result = 31 * result + (alertingConfigurationId != null ? alertingConfigurationId.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}