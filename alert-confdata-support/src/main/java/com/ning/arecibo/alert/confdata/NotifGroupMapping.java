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
public class NotifGroupMapping
{
    private final Integer notifGroupId;
    private final Integer alertingConfigId;
    private final Integer id;

    @JsonCreator
    public NotifGroupMapping(@JsonProperty("notif_group_id") final Integer notifGroupId,
                             @JsonProperty("alerting_config_id") final Integer alertingConfigId,
                             @JsonProperty("id") final Integer id)
    {
        this.notifGroupId = notifGroupId;
        this.alertingConfigId = alertingConfigId;
        this.id = id;
    }

    public Integer getId()
    {
        return id;
    }

    public Integer getAlertingConfigId()
    {
        return alertingConfigId;
    }

    public Integer getNotifGroupId()
    {
        return notifGroupId;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("NotifMapping");
        sb.append("{id=").append(id);
        sb.append(", notifGroupId=").append(notifGroupId);
        sb.append(", alertingConfigId=").append(alertingConfigId);
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

        final NotifGroupMapping that = (NotifGroupMapping) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (alertingConfigId != null ? !alertingConfigId.equals(that.alertingConfigId) : that.alertingConfigId != null) {
            return false;
        }
        if (notifGroupId != null ? !notifGroupId.equals(that.notifGroupId) : that.notifGroupId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = notifGroupId != null ? notifGroupId.hashCode() : 0;
        result = 31 * result + (alertingConfigId != null ? alertingConfigId.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}