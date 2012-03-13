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

package com.ning.arecibo.alertmanager.models;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ThresholdDefinitionsModel
{
    private final List<ThresholdDefinition> thresholdDefinitions = new ArrayList<ThresholdDefinition>();
    private final Iterable<Map<String, Object>> allAlertingConfigurations;

    public static final class ThresholdDefinition
    {
        private final String thresholdDefinitionName;
        private final String monitoredEventType;
        private final String monitoredAttributeType;
        private final String clearingIntervalMs;
        private final String minThresholdValue;
        private final String maxThresholdValue;
        private final String minThresholdSamples;
        private final String maxSampleWindowMs;
        private final String alertingConfiguration;
        private final Multimap<String, String> qualifyingAttrs;
        private final List<String> contextAttrs;

        public ThresholdDefinition(final String thresholdDefinitionName, final String monitoredEventType, final String monitoredAttributeType,
                                   final String clearingIntervalMs, final String minThresholdValue, final String maxThresholdValue,
                                   final String minThresholdSamples, final String maxSampleWindowMs, final Multimap<String, String> qualifyingAttrs,
                                   final List<String> contextAttrs, final String alertingConfiguration)
        {
            this.thresholdDefinitionName = thresholdDefinitionName;
            this.alertingConfiguration = alertingConfiguration;
            this.clearingIntervalMs = clearingIntervalMs;
            this.contextAttrs = contextAttrs;
            this.maxSampleWindowMs = maxSampleWindowMs;
            this.maxThresholdValue = maxThresholdValue;
            this.minThresholdSamples = minThresholdSamples;
            this.minThresholdValue = minThresholdValue;
            this.monitoredAttributeType = monitoredAttributeType;
            this.monitoredEventType = monitoredEventType;
            this.qualifyingAttrs = qualifyingAttrs;
        }

        public String getAlertingConfiguration()
        {
            return alertingConfiguration;
        }

        public String getClearingIntervalMs()
        {
            return clearingIntervalMs;
        }

        public List<String> getContextAttrs()
        {
            return contextAttrs;
        }

        public String getMaxSampleWindowMs()
        {
            return maxSampleWindowMs;
        }

        public String getMaxThresholdValue()
        {
            return maxThresholdValue;
        }

        public String getMinThresholdSamples()
        {
            return minThresholdSamples;
        }

        public String getMinThresholdValue()
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

        public Multimap<String, String> getQualifyingAttrs()
        {
            return qualifyingAttrs;
        }

        public String getThresholdDefinitionName()
        {
            return thresholdDefinitionName;
        }
    }

    public ThresholdDefinitionsModel(final Map<String, Map<String, Object>> existingThresholdDefinitions, final Iterable<Map<String, Object>> alertingConfigurations)
    {
        this.allAlertingConfigurations = alertingConfigurations;

        for (final String thresholdConfigName : existingThresholdDefinitions.keySet()) {
            final Map<String, Object> thresholdConfig = existingThresholdDefinitions.get(thresholdConfigName);

            final Multimap<String, String> qualifyingAttrs = HashMultimap.create();
            final Object existingQualifyingAttrs = thresholdConfig.get("qualifyingAttrs");
            if (existingQualifyingAttrs != null) {
                for (final Map<String, Object> qualifyingAttr : (Iterable<Map<String, Object>>) existingQualifyingAttrs) {
                    qualifyingAttrs.put((String) qualifyingAttr.get("attribute_type"), (String) qualifyingAttr.get("attribute_value"));
                }
            }

            final List<String> contextAttrs = new ArrayList<String>();
            final Object existingContextAttrs = thresholdConfig.get("contextAttrs");
            if (existingContextAttrs != null) {
                for (final Map<String, Object> contextAttr : (Iterable<Map<String, Object>>) existingContextAttrs) {
                    contextAttrs.add((String) contextAttr.get("attribute_type"));
                }
            }

            final ThresholdDefinition thresholdDefinition = new ThresholdDefinition(
                thresholdConfigName,
                ModelUtils.toString(thresholdConfig.get("monitored_event_type")),
                ModelUtils.toString(thresholdConfig.get("monitored_attribute_type")),
                ModelUtils.toString(thresholdConfig.get("clearing_interval_ms")),
                ModelUtils.toString(thresholdConfig.get("min_threshold_value")),
                ModelUtils.toString(thresholdConfig.get("max_threshold_value")),
                ModelUtils.toString(thresholdConfig.get("min_threshold_samples")),
                ModelUtils.toString(thresholdConfig.get("max_sample_window_ms")),
                qualifyingAttrs,
                contextAttrs,
                ModelUtils.toString(thresholdConfig.get("alertingConfig"))
            );
            thresholdDefinitions.add(thresholdDefinition);
        }
    }

    public List<ThresholdDefinition> getThresholdDefinitions()
    {
        return thresholdDefinitions;
    }

    public Iterable<Map<String, Object>> getAllAlertingConfigurations()
    {
        return allAlertingConfigurations;
    }
}
