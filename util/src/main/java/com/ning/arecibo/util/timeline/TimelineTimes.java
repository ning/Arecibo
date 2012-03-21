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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TimelineTimes extends CachedObject
{
    public static final ResultSetMapper<TimelineTimes> mapper = new ResultSetMapper<TimelineTimes>()
    {
        @Override
        public TimelineTimes map(final int index, final ResultSet rs, final StatementContext ctx) throws SQLException
        {
            final int timelineIntervalId = rs.getInt("timeline_times_id");
            final int hostId = rs.getInt("host_id");
            final DateTime startTime = dateTimeFromUnixSeconds(rs.getInt("start_time"));
            final DateTime endTime = dateTimeFromUnixSeconds(rs.getInt("end_time"));
            final int count = rs.getInt("count");
            byte[] times = rs.getBytes("in_row_times");
            if (rs.wasNull()) {
                final Blob blobTimes = rs.getBlob("blob_times");
                times = blobTimes.getBytes(1, (int) blobTimes.length());
            }
            return new TimelineTimes(timelineIntervalId, hostId, startTime, endTime, times, count);
        }
    };

    private final int hostId;
    private final DateTime startTime;
    private final DateTime endTime;
    private final int sampleCount;
    private final byte[] compressedTimes;

    public TimelineTimes(final long timelineIntervalId, final int hostId, final DateTime startTime, final DateTime endTime, final List<DateTime> dateTimes)
    {
        super(timelineIntervalId);
        this.hostId = hostId;
        this.startTime = startTime;
        this.sampleCount = dateTimes.size();
        this.endTime = endTime;
        final int[] times = new int[dateTimes.size()];
        int i = 0;
        for (DateTime dateTime : dateTimes) {
            times[i++] = unixSeconds(dateTime);
        }
        compressedTimes = TimelineCoder.compressTimes(times);
    }

    public TimelineTimes(final long timelineIntervalId, final int hostId, final DateTime startTime, final DateTime endTime, final byte[] compressedTimes, final int sampleCount)
    {
        super(timelineIntervalId);
        this.hostId = hostId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sampleCount = sampleCount;
        this.compressedTimes = compressedTimes;
    }

    public int getHostId()
    {
        return hostId;
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
