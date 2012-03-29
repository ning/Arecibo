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
public class NotifConfig
{
    private final String address;
    private final String notifType;
    private final Integer id;

    @JsonCreator
    public NotifConfig(@JsonProperty("address") final String address,
                       @JsonProperty("notif_type") final String notifType,
                       @JsonProperty("id") final Integer id)
    {
        this.notifType = notifType;
        this.address = address;
        this.id = id;
    }

    public String getAddress()
    {
        return address;
    }

    public String getNotifType()
    {
        return notifType;
    }

    public Integer getId()
    {
        return id;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("NotifConfig");
        sb.append("{address='").append(address).append('\'');
        sb.append(", notifType='").append(notifType).append('\'');
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

        final NotifConfig that = (NotifConfig) o;

        if (address != null ? !address.equals(that.address) : that.address != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (notifType != null ? !notifType.equals(that.notifType) : that.notifType != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + (notifType != null ? notifType.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
