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

public class TimelineTimesMapper implements ResultSetMapper<TimelineTimes>
{
    public static final int MAX_IN_ROW_BLOB_SIZE = 400;

	@Override
    public TimelineTimes map(final int index, final ResultSet rs, final StatementContext ctx) throws SQLException
    {
        final int timelineIntervalId = rs.getInt("timeline_times_id");
        final int hostId = rs.getInt("host_id");
        final int eventCategoryId = rs.getInt("event_category_id");
        final DateTime startTime = TimelineTimes.dateTimeFromUnixSeconds(rs.getInt("start_time"));
        final DateTime endTime = TimelineTimes.dateTimeFromUnixSeconds(rs.getInt("end_time"));
        final int count = rs.getInt("count");
        final int aggregationLevel = rs.getInt("aggregation_level");
        final boolean notValid = rs.getInt("not_valid") != 0;
        byte[] times = rs.getBytes("in_row_times");
        if (rs.wasNull()) {
            final Blob blobTimes = rs.getBlob("blob_times");
            times = blobTimes.getBytes(1, (int) blobTimes.length());
        }
        return new TimelineTimes(timelineIntervalId, hostId, eventCategoryId, startTime, endTime, times, count, aggregationLevel, notValid);
    }
}
