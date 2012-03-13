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

import com.ning.arecibo.alert.confdata.AlertingConfig;
import com.ning.arecibo.alert.confdata.ThresholdContextAttr;
import com.ning.arecibo.alert.confdata.ThresholdDefinition;
import com.ning.arecibo.alert.confdata.ThresholdQualifyingAttr;

import java.util.Map;

public class ThresholdDefinitionsModel
{
    private final Iterable<ThresholdDefinition> thresholdConfigs;
    private final Map<String, Iterable<ThresholdQualifyingAttr>> thresholdQualifyingAttrsForThresholdConfig;
    private final Map<String, Iterable<ThresholdContextAttr>> thresholdContextAttrsForThresholdConfig;
    private final Map<String, AlertingConfig> alertingConfigsForThresholdConfig;
    private final Iterable<AlertingConfig> allAlertingConfigurations;

    public ThresholdDefinitionsModel(final Iterable<ThresholdDefinition> thresholdConfigs,
                                     final Map<String, Iterable<ThresholdQualifyingAttr>> thresholdQualifyingAttrsForThresholdConfig,
                                     final Map<String, Iterable<ThresholdContextAttr>> thresholdContextAttrsForThresholdConfig,
                                     final Map<String, AlertingConfig> alertingConfigsForThresholdConfig,
                                     final Iterable<AlertingConfig> allAlertingConfigurations)
    {
        this.thresholdConfigs = thresholdConfigs;
        this.thresholdQualifyingAttrsForThresholdConfig = thresholdQualifyingAttrsForThresholdConfig;
        this.thresholdContextAttrsForThresholdConfig = thresholdContextAttrsForThresholdConfig;
        this.alertingConfigsForThresholdConfig = alertingConfigsForThresholdConfig;
        this.allAlertingConfigurations = allAlertingConfigurations;
    }

    public Map<String, AlertingConfig> getAlertingConfigsForThresholdConfig()
    {
        return alertingConfigsForThresholdConfig;
    }

    public Iterable<AlertingConfig> getAllAlertingConfigurations()
    {
        return allAlertingConfigurations;
    }

    public Iterable<ThresholdDefinition> getThresholdConfigs()
    {
        return thresholdConfigs;
    }

    public Map<String, Iterable<ThresholdContextAttr>> getThresholdContextAttrsForThresholdConfig()
    {
        return thresholdContextAttrsForThresholdConfig;
    }

    public Map<String, Iterable<ThresholdQualifyingAttr>> getThresholdQualifyingAttrsForThresholdConfig()
    {
        return thresholdQualifyingAttrsForThresholdConfig;
    }
}
