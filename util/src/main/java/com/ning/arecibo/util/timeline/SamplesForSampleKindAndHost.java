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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SamplesForSampleKindAndHost
{
    @JsonProperty
    private final String hostName;

    @JsonProperty
    private final String eventCategory;

    @JsonProperty
    private final String sampleKind;

    // TODO for now the dashboard consumes csv samples
    // we should switch it to use the compact view (compressed)
    @JsonProperty
    private final String samples;

    @JsonCreator
    public SamplesForSampleKindAndHost(@JsonProperty("hostName") final String hostName, @JsonProperty("eventCategory") final String eventCategory,
                                       @JsonProperty("sampleKind") final String sampleKind, @JsonProperty("samples") final String samples)
    {
        this.hostName = hostName;
        this.eventCategory = eventCategory;
        this.sampleKind = sampleKind;
        this.samples = samples;
    }

    public String getHostName()
    {
        return hostName;
    }

    public String getEventCategory()
    {
        return eventCategory;
    }

    public String getSampleKind()
    {
        return sampleKind;
    }

    public String getSamples()
    {
        return samples;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("SamplesForSampleKindAndHost");
        sb.append("{eventCategory='").append(eventCategory).append('\'');
        sb.append(", hostName='").append(hostName).append('\'');
        sb.append(", sampleKind='").append(sampleKind).append('\'');
        sb.append(", samples='").append(samples).append('\'');
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

        final SamplesForSampleKindAndHost that = (SamplesForSampleKindAndHost) o;

        if (!eventCategory.equals(that.eventCategory)) {
            return false;
        }
        if (!hostName.equals(that.hostName)) {
            return false;
        }
        if (!sampleKind.equals(that.sampleKind)) {
            return false;
        }
        if (!samples.equals(that.samples)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = hostName.hashCode();
        result = 31 * result + eventCategory.hashCode();
        result = 31 * result + sampleKind.hashCode();
        result = 31 * result + samples.hashCode();
        return result;
    }
}