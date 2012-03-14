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
public class ThresholdContextAttr
{
    private final String attributeType;

    @JsonCreator
    public ThresholdContextAttr(@JsonProperty("attribute_type") final String attributeType)
    {
        this.attributeType = attributeType;
    }

    public String getAttributeType()
    {
        return attributeType;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("ThresholdContextAttr");
        sb.append("{attributeType='").append(attributeType).append('\'');
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

        final ThresholdContextAttr that = (ThresholdContextAttr) o;

        if (attributeType != null ? !attributeType.equals(that.attributeType) : that.attributeType != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return attributeType != null ? attributeType.hashCode() : 0;
    }
}