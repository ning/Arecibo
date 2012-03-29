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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

public class TimelineTimes extends CachedObject
{
    private final int hostId;
    private final String eventCategory;
    private final DateTime startTime;
    private final DateTime endTime;
    private final int aggregationLevel;
    private final boolean notValid;

    @JsonProperty(value = "timeSampleCount")
    @JsonView(TimelineChunksAndTimesViews.Compact.class)
    private final int sampleCount;
    @JsonProperty
    @JsonView(TimelineChunksAndTimesViews.Compact.class)
    private final byte[] compressedTimes;

    public TimelineTimes(final long timelineIntervalId, final int hostId, final String eventCategory, final DateTime startTime, final DateTime endTime, final List<DateTime> dateTimes)
    {
        super(timelineIntervalId);
        this.hostId = hostId;
        this.eventCategory = eventCategory;
        this.startTime = startTime;
        this.sampleCount = dateTimes.size();
        this.endTime = endTime;
        final int[] times = new int[dateTimes.size()];
        int i = 0;
        for (final DateTime dateTime : dateTimes) {
            times[i++] = unixSeconds(dateTime);
        }
        compressedTimes = TimelineCoder.compressTimes(times);
        aggregationLevel = 0;
        notValid = false;
    }

    public TimelineTimes(final long timelineIntervalId, final int hostId, final String eventCategory, final DateTime startTime, final DateTime endTime, final byte[] compressedTimes, final int sampleCount)
    {
        super(timelineIntervalId);
        this.hostId = hostId;
        this.eventCategory = eventCategory;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sampleCount = sampleCount;
        this.compressedTimes = compressedTimes;
        aggregationLevel = 0;
        notValid = false;
    }

    public TimelineTimes(final long timelineIntervalId, final int hostId, final String eventCategory, final DateTime startTime, final DateTime endTime,
                         final byte[] compressedTimes, final int sampleCount, final int aggregationLevel, final boolean notValid)
    {
        super(timelineIntervalId);
        this.hostId = hostId;
        this.eventCategory = eventCategory;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sampleCount = sampleCount;
        this.compressedTimes = compressedTimes;
        this.aggregationLevel = aggregationLevel;
        this.notValid = notValid;
    }

    public int getHostId()
    {
        return hostId;
    }

    public String getEventCategory()
    {
        return eventCategory;
    }

    public DateTime getStartTime()
    {
        return startTime;
    }

    public DateTime getEndTime()
    {
        return endTime;
    }

    public int getSampleCount()
    {
        return sampleCount;
    }

    public TimeCursor getTimeCursor()
    {
        return new TimeCursor(this);
    }

    public byte[] getCompressedTimes()
    {
        return compressedTimes;
    }

    public int getAggregationLevel()
    {
        return aggregationLevel;
    }

    public boolean getNotValid()
    {
        return notValid;
    }

    public static DateTime dateTimeFromUnixSeconds(final int unixTime)
    {
        return new DateTime(((long) unixTime) * 1000L, DateTimeZone.UTC);
    }

    public static int unixSeconds(final DateTime dateTime)
    {
        final long millis = dateTime.toDateTime(DateTimeZone.UTC).getMillis();
        return (int) (millis / 1000L);
    }

}
