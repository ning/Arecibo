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

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonView;
import org.joda.time.DateTime;

/**
 * Instances of this class represent timeline sequences read from the database
 * for a single host and single sample kind.  The samples are held in a byte
 * array.
 */
public class TimelineChunk extends CachedObject
{
    private final int hostId;
    private final int sampleKindId;
    private final int timelineTimesId;

    @JsonProperty
    @JsonView(TimelineChunksAndTimesViews.Compact.class)
    private final DateTime startTime;
    @JsonProperty
    @JsonView(TimelineChunksAndTimesViews.Compact.class)
    private final DateTime endTime;
    @JsonProperty
    @JsonView(TimelineChunksAndTimesViews.Compact.class)
    private final byte[] samples;
    @JsonProperty
    @JsonView(TimelineChunksAndTimesViews.Compact.class)
    private final int sampleCount;

    public TimelineChunk(final long sampleTimelineId, final int hostId, final int sampleKindId, final int timelineTimesId, final DateTime startTime, final DateTime endTime, final byte[] samples, final int sampleCount)
    {
        super(sampleTimelineId);
        this.hostId = hostId;
        this.sampleKindId = sampleKindId;
        this.timelineTimesId = timelineTimesId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.samples = samples;
        this.sampleCount = sampleCount;
    }

    public TimelineChunk(final long sampleTimelineId, final TimelineChunk other)
    {
        super(sampleTimelineId);
        this.hostId = other.hostId;
        this.sampleKindId = other.sampleKindId;
        this.timelineTimesId = other.timelineTimesId;
        this.startTime = other.startTime;
        this.endTime = other.endTime;
        this.samples = other.samples;
        this.sampleCount = other.sampleCount;
    }

    public int getHostId()
    {
        return hostId;
    }

    public int getSampleKindId()
    {
        return sampleKindId;
    }

    public int getTimelineTimesId()
    {
        return timelineTimesId;
    }

    public DateTime getStartTime()
    {
        return startTime;
    }

    public DateTime getEndTime()
    {
        return endTime;
    }

    public byte[] getSamples()
    {
        return samples;
    }

    public int getSampleCount()
    {
        return sampleCount;
    }
}
