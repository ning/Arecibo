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

package com.ning.arecibo.util.timeline;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryAndSampleKinds
{
    @JsonProperty
    private final String eventCategory;
    @JsonProperty
    private final Set<String> sampleKinds = new HashSet<String>();

    public CategoryAndSampleKinds(final String eventCategory)
    {
        this.eventCategory = eventCategory;
    }

    @JsonCreator
    public CategoryAndSampleKinds(@JsonProperty("eventCategory") final String eventCategory, @JsonProperty("sampleKinds") final List<String> sampleKinds)
    {
        this.eventCategory = eventCategory;
        this.sampleKinds.addAll(sampleKinds);
    }

    public void addSampleKind(final String sampleKind)
    {
        sampleKinds.add(sampleKind);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("CategoryAndSampleKinds");
        sb.append("{eventCategory='").append(eventCategory).append('\'');
        sb.append(", sampleKinds=").append(sampleKinds);
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

        final CategoryAndSampleKinds that = (CategoryAndSampleKinds) o;

        if (!eventCategory.equals(that.eventCategory)) {
            return false;
        }
        if (!sampleKinds.equals(that.sampleKinds)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = eventCategory.hashCode();
        result = 31 * result + sampleKinds.hashCode();
        return result;
    }
}
