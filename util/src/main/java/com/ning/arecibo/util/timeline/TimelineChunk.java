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
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Instances of this class represent timeline sequences read from the database
 * for a single host and single sample kind.  The samples are held in a byte
 * array.
 */
public class TimelineChunk extends CachedObject
{
    public static final ResultSetMapper<TimelineChunk> mapper = new ResultSetMapper<TimelineChunk>()
    {
        @Override
        public TimelineChunk map(final int index, final ResultSet rs, final StatementContext ctx) throws SQLException
        {
            final int sampleTimelineId = rs.getInt("sample_timeline_id");
            final int hostId = rs.getInt("host_id");
            final int sampleKindId = rs.getInt("sample_kind_id");
            final int timelineIntervalId = rs.getInt("timeline_times_id");
            final int sampleCount = rs.getInt("sample_count");
            final DateTime startTime = new DateTime(TimelineTimes.dateTimeFromUnixSeconds(rs.getInt("start_time")));
            byte[] samples = rs.getBytes("in_row_samples");
            if (rs.wasNull()) {
                final Blob blobSamples = rs.getBlob("blob_samples");
                samples = blobSamples.getBytes(1, (int) blobSamples.length());
            }
            return new TimelineChunk(sampleTimelineId, hostId, sampleKindId, timelineIntervalId, startTime, samples, sampleCount);
        }
    };

    private final int hostId;
    private final int sampleKindId;
    private final int timelineTimesId;
    private final DateTime startTime;
    private final byte[] samples;
    private final int sampleCount;

    public TimelineChunk(final long sampleTimelineId, final int hostId, final int sampleKindId, final int timelineTimesId, final DateTime startTime, final byte[] samples, final int sampleCount)
    {
        super(sampleTimelineId);
        this.hostId = hostId;
        this.sampleKindId = sampleKindId;
        this.timelineTimesId = timelineTimesId;
        this.startTime = startTime;
        this.samples = samples;
        this.sampleCount = sampleCount;
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

    public DateTime getStartTime() {
        return startTime;
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
