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

import com.ning.arecibo.util.Logger;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonUnwrapped;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonView;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.Folder2;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Instances of this class represent samples sent from one host and one
 * category, e.g., JVM heap size.
 * <p/>
 * This is a fat object used mainly for dashboarding and debugging purposes.
 */
public class TimelineChunkAndTimes
{
    private static final Logger log = Logger.getLogger(TimelineChunkAndTimes.class);

    public static final ResultSetMapper<TimelineChunkAndTimes> mapper = new ResultSetMapper<TimelineChunkAndTimes>()
    {
        @Override
        public TimelineChunkAndTimes map(final int index, final ResultSet rs, final StatementContext ctx) throws SQLException
        {
            // Construct the TimelineChunk
            final int sampleTimelineId = rs.getInt("sample_timeline_id");
            final int hostId = rs.getInt("host_id");
            final int sampleKindId = rs.getInt("sample_kind_id");
            final int timelineIntervalId = rs.getInt("timeline_times_id");
            final int sampleCount = rs.getInt("sample_count");
            final DateTime startTime = TimelineTimes.dateTimeFromUnixSeconds(rs.getInt("start_time"));
            byte[] samples = rs.getBytes("in_row_samples");
            if (rs.wasNull()) {
                final Blob blobSamples = rs.getBlob("blob_samples");
                samples = blobSamples.getBytes(1, (int) blobSamples.length());
            }
            final TimelineChunk timelineChunk = new TimelineChunk(sampleTimelineId, hostId, sampleKindId, timelineIntervalId, startTime, samples, sampleCount);

            // Construct the TimelineTimes
            final DateTime endTime = TimelineTimes.dateTimeFromUnixSeconds(rs.getInt("end_time"));
            final int count = rs.getInt("count");
            byte[] times = rs.getBytes("in_row_times");
            if (rs.wasNull()) {
                final Blob blobTimes = rs.getBlob("blob_times");
                times = blobTimes.getBytes(1, (int) blobTimes.length());
            }
            final TimelineTimes timelineTimesObject = new TimelineTimes(timelineIntervalId, hostId, startTime, endTime, times, count);

            final String hostName = rs.getString("host_name");
            final String sampleKind = rs.getString("sample_kind");

            return new TimelineChunkAndTimes(hostName, sampleKind, timelineChunk, timelineTimesObject);
        }
    };

    @JsonProperty
    @JsonView(TimelineChunksAndTimesViews.Base.class)
    private final String hostName;
    @JsonProperty
    @JsonView(TimelineChunksAndTimesViews.Base.class)
    private final String sampleKind;
    @JsonUnwrapped
    @JsonView(TimelineChunksAndTimesViews.Compact.class)
    private final TimelineChunk timelineChunk;
    @JsonUnwrapped
    @JsonView(TimelineChunksAndTimesViews.Compact.class)
    private final TimelineTimes timelineTimes;

    public TimelineChunkAndTimes(final String hostName, final String sampleKind, final TimelineChunk timelineChunk, final TimelineTimes timelineTimes)
    {
        this.hostName = hostName;
        this.sampleKind = sampleKind;
        this.timelineChunk = timelineChunk;
        this.timelineTimes = timelineTimes;
    }

    public String getHostName()
    {
        return hostName;
    }

    public String getSampleKind()
    {
        return sampleKind;
    }

    public TimelineChunk getTimelineChunk()
    {
        return timelineChunk;
    }

    public TimelineTimes getTimelineTimes()
    {
        return timelineTimes;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @JsonView(TimelineChunksAndTimesViews.Loose.class)
    public String getSamplesAsCSV() throws IOException
    {
        return getSamplesAsCSV(null, null);
    }

    public String getSamplesAsCSV(@Nullable final DateTime startTime, @Nullable final DateTime endTime) throws IOException
    {
        final CSVOutputProcessor processor = new CSVOutputProcessor(startTime, endTime);
        SampleCoder.scan(timelineChunk.getSamples(), timelineTimes, processor);
        return processor.getSamplesCSV();
    }

    @Override
    public String toString()
    {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(out);
            generator.writeStartObject();

            generator.writeFieldName("sampleKind");
            generator.writeString(sampleKind);

            generator.writeFieldName("samples");
            generator.writeString(getSamplesAsCSV());

            generator.writeEndObject();
            generator.close();
            return out.toString();
        }
        catch (IOException e) {
            log.error(e);
        }

        return null;
    }

    private static final class CSVOutputProcessor extends TimeRangeSampleProcessor
    {
        private final StringBuilder builder = new StringBuilder();
        private boolean firstSamples = true;

        public CSVOutputProcessor(@Nullable final DateTime startTime, @Nullable final DateTime endTime)
        {
            super(startTime, endTime);
        }

        @Override
        public void processOneSample(final DateTime sampleTimestamp, final SampleOpcode opcode, final Object value)
        {
            if (sampleTimestamp != null) {
                final String valueString = value == null ? "0" : value.toString();
                if (!firstSamples) {
                    builder.append(",");
                }
                else {
                    firstSamples = false;
                }

                builder
                    .append(TimelineTimes.unixSeconds(sampleTimestamp))
                    .append(",")
                    .append(valueString);
            }
        }

        public String getSamplesCSV()
        {
            return builder.toString();
        }
    }
}
