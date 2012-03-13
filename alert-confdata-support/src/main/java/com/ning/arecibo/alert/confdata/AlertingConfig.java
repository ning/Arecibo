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
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertingConfig
{
    private final String alertingConfigurationName;
    private final String repeatMode;
    private final Long repeatInterval;
    private final boolean notifyOnRecovery;
    private final boolean enabled;
    private final Integer id;

    @JsonCreator
    public AlertingConfig(@JsonProperty("label") final String alertingConfigurationName,
                          @JsonProperty("enabled") final String enabled,
                          @JsonProperty("notif_on_recovery") final String notifOnRecovery,
                          @JsonProperty("notif_repeat_interval_ms") final Long repeatInterval,
                          @JsonProperty("notif_repeat_mode") final String repeatMode,
                          @JsonProperty("id") final Integer id)
    {
        this.alertingConfigurationName = alertingConfigurationName;
        this.enabled = !(enabled != null && enabled.equals("0"));
        this.notifyOnRecovery = !(notifOnRecovery != null && notifOnRecovery.equals("0"));
        this.repeatInterval = repeatInterval;
        this.repeatMode = repeatMode;
        this.id = id;
    }

    public String getAlertingConfigurationName()
    {
        return alertingConfigurationName;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public boolean isNotifyOnRecovery()
    {
        return notifyOnRecovery;
    }

    public Long getRepeatInterval()
    {
        return repeatInterval;
    }

    public String getRepeatMode()
    {
        return repeatMode;
    }

    public Integer getId()
    {
        return id;
    }
}