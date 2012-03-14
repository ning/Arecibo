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

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("AlertingConfig");
        sb.append("{alertingConfigurationName='").append(alertingConfigurationName).append('\'');
        sb.append(", repeatMode='").append(repeatMode).append('\'');
        sb.append(", repeatInterval=").append(repeatInterval);
        sb.append(", notifyOnRecovery=").append(notifyOnRecovery);
        sb.append(", enabled=").append(enabled);
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

        final AlertingConfig that = (AlertingConfig) o;

        if (enabled != that.enabled) {
            return false;
        }
        if (notifyOnRecovery != that.notifyOnRecovery) {
            return false;
        }
        if (alertingConfigurationName != null ? !alertingConfigurationName.equals(that.alertingConfigurationName) : that.alertingConfigurationName != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (repeatInterval != null ? !repeatInterval.equals(that.repeatInterval) : that.repeatInterval != null) {
            return false;
        }
        if (repeatMode != null ? !repeatMode.equals(that.repeatMode) : that.repeatMode != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = alertingConfigurationName != null ? alertingConfigurationName.hashCode() : 0;
        result = 31 * result + (repeatMode != null ? repeatMode.hashCode() : 0);
        result = 31 * result + (repeatInterval != null ? repeatInterval.hashCode() : 0);
        result = 31 * result + (notifyOnRecovery ? 1 : 0);
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}