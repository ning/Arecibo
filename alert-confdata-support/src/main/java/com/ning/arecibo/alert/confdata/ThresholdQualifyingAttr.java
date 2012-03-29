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
public class ThresholdQualifyingAttr
{
    private final String attributeType;
    private final String attributeValue;

    @JsonCreator
    public ThresholdQualifyingAttr(@JsonProperty("attribute_type") final String attributeType,
                                   @JsonProperty("attribute_value") final String attributeValue)
    {
        this.attributeType = attributeType;
        this.attributeValue = attributeValue;
    }

    public String getAttributeType()
    {
        return attributeType;
    }

    public String getAttributeValue()
    {
        return attributeValue;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThresholdQualifyingAttr");
        sb.append("{attributeType='").append(attributeType).append('\'');
        sb.append(", attributeValue='").append(attributeValue).append('\'');
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

        final ThresholdQualifyingAttr that = (ThresholdQualifyingAttr) o;

        if (attributeType != null ? !attributeType.equals(that.attributeType) : that.attributeType != null) {
            return false;
        }
        if (attributeValue != null ? !attributeValue.equals(that.attributeValue) : that.attributeValue != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = attributeType != null ? attributeType.hashCode() : 0;
        result = 31 * result + (attributeValue != null ? attributeValue.hashCode() : 0);
        return result;
    }
}