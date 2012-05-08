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

import java.util.Set;
import java.util.TreeSet;

public class CategoryAndSampleKindsForHosts implements Comparable<CategoryAndSampleKindsForHosts>
{
    @JsonProperty
    private final CategoryAndSampleKinds categoryAndSampleKinds;
    @JsonProperty
    private final Set<String> hosts;

    public CategoryAndSampleKindsForHosts(final String eventCategory)
    {
        this(new CategoryAndSampleKinds(eventCategory), new TreeSet<String>());
    }

    @JsonCreator
    public CategoryAndSampleKindsForHosts(@JsonProperty("categoryAndSampleKinds") final CategoryAndSampleKinds categoryAndSampleKinds, @JsonProperty("hosts") final Set<String> hosts)
    {
        this.categoryAndSampleKinds = categoryAndSampleKinds;
        this.hosts = hosts;
    }

    public void add(final String sampleKind, final String host)
    {
        categoryAndSampleKinds.addSampleKind(sampleKind);
        hosts.add(host);
    }

    public CategoryAndSampleKinds getCategoryAndSampleKinds()
    {
        return categoryAndSampleKinds;
    }

    public Set<String> getHosts()
    {
        return hosts;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("CategoryAndSampleKindsForHosts");
        sb.append("{categoryAndSampleKinds=").append(categoryAndSampleKinds);
        sb.append(", hosts=").append(hosts);
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

        final CategoryAndSampleKindsForHosts that = (CategoryAndSampleKindsForHosts) o;

        if (categoryAndSampleKinds != null ? !categoryAndSampleKinds.equals(that.categoryAndSampleKinds) : that.categoryAndSampleKinds != null) {
            return false;
        }
        if (hosts != null ? !hosts.equals(that.hosts) : that.hosts != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = categoryAndSampleKinds != null ? categoryAndSampleKinds.hashCode() : 0;
        result = 31 * result + (hosts != null ? hosts.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(final CategoryAndSampleKindsForHosts o)
    {
        return categoryAndSampleKinds.compareTo(o.getCategoryAndSampleKinds());
    }
}
