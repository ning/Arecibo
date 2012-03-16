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

import com.google.common.collect.ImmutableMap;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonValue;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 * Instances of this class represent samples sent from one host and one
 * category, e.g., JVM, representing one point in time.
 */
@SuppressWarnings("unchecked")
public class HostSamplesForTimestamp
{
    private static final String KEY_HOST = "H";
    private static final String KEY_CATEGORY = "V";
    private static final String KEY_TIMESTAMP = "T";
    private static final String KEY_SAMPLES = "S";

    private final int hostId;
    private final String category;
    private final DateTime timestamp;
    // A map from sample id to sample value for that timestamp
    private final Map<Integer, ScalarSample> samples;

    public HostSamplesForTimestamp(final int hostId, final String category, final DateTime timestamp)
    {
        this(hostId, category, timestamp, new HashMap<Integer, ScalarSample>());
    }

    @JsonCreator
    public HostSamplesForTimestamp(@JsonProperty(KEY_HOST) final int hostId, @JsonProperty(KEY_CATEGORY) final String category, @JsonProperty(KEY_TIMESTAMP) final DateTime timestamp, @JsonProperty(KEY_SAMPLES) final Map<Integer, ScalarSample> samples)
    {
        this.hostId = hostId;
        this.category = category;
        this.timestamp = timestamp;
        this.samples = samples;
    }

    public int getHostId()
    {
        return hostId;
    }

    public String getCategory()
    {
        return category;
    }

    public DateTime getTimestamp()
    {
        return timestamp;
    }

    public Map<Integer, ScalarSample> getSamples()
    {
        return samples;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("HostSamplesForTimestamp");
        sb.append("{category='").append(category).append('\'');
        sb.append(", hostId=").append(hostId);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", samples=").append(samples);
        sb.append('}');

        return sb.toString();
    }

    @JsonValue
    public Map<String, Object> toMap()
    {
        return ImmutableMap.of(KEY_HOST, hostId, KEY_CATEGORY, category, KEY_TIMESTAMP, timestamp, KEY_SAMPLES, samples);
    }
}
