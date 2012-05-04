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

package com.ning.arecibo.dashboard.config;

import com.ning.arecibo.util.timeline.CategoryAndSampleKinds;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CustomGroup
{
    private final String name;
    private final List<CategoryAndSampleKinds> kinds;

    @JsonCreator
    public CustomGroup(@JsonProperty("name") final String name, @JsonProperty("kinds") final List<CategoryAndSampleKinds> kinds)
    {
        this.name = name;
        this.kinds = kinds;
    }

    public String getName()
    {
        return name;
    }

    public List<CategoryAndSampleKinds> getKinds()
    {
        return kinds;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("CustomGroup");
        sb.append("{name='").append(name).append('\'');
        sb.append(", kinds=").append(kinds);
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

        final CustomGroup that = (CustomGroup) o;

        if (kinds != null ? !kinds.equals(that.kinds) : that.kinds != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (kinds != null ? kinds.hashCode() : 0);
        return result;
    }
}
