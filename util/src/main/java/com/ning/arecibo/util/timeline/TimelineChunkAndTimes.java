package com.ning.arecibo.util.timeline;

import com.ning.arecibo.util.Logger;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.annotate.JsonValue;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.Folder2;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

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

    public static final Folder2<List<TimelineChunkAndTimes>> folder = new Folder2<List<TimelineChunkAndTimes>>()
    {
        @Override
        public List<TimelineChunkAndTimes> fold(final List<TimelineChunkAndTimes> accumulator, final ResultSet rs, final StatementContext ctx) throws SQLException
        {
            accumulator.add(TimelineChunkAndTimes.mapper.map(1, rs, ctx));
            return accumulator;
        }
    };

    private static final ResultSetMapper<TimelineChunkAndTimes> mapper = new ResultSetMapper<TimelineChunkAndTimes>()
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
            final Blob blobSamples = rs.getBlob("sample_bytes");
            final byte[] samples = blobSamples.getBytes(1, (int) blobSamples.length());
            final TimelineChunk timelineChunk = new TimelineChunk(sampleTimelineId, hostId, sampleKindId, timelineIntervalId, samples, sampleCount);

            // Construct the TimelineTimes
            final DateTime startTime = TimelineTimes.dateTimeFromUnixSeconds(rs.getInt("start_time"));
            final DateTime endTime = TimelineTimes.dateTimeFromUnixSeconds(rs.getInt("end_time"));
            final int count = rs.getInt("count");
            final byte[] blobTimes = rs.getBytes("times");
            final TimelineTimes timelineTimesObject = new TimelineTimes(timelineIntervalId, hostId, startTime, endTime, blobTimes, count);

            final String hostName = rs.getString("host_name");
            final String sampleKind = rs.getString("sample_kind");

            return new TimelineChunkAndTimes(hostName, sampleKind, timelineChunk, timelineTimesObject);
        }
    };

    private final String hostName;
    private final String sampleKind;
    private final TimelineChunk timelineChunk;
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

    @JsonValue
    @Override
    public String toString()
    {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(out);
            generator.writeStartObject();

            generator.writeFieldName("sampleKind");
            generator.writeString(sampleKind);

            final CSVOutputProcessor processor = new CSVOutputProcessor();
            SampleCoder.scan(timelineChunk.getSamples(), timelineTimes, processor);
            generator.writeFieldName("samples");
            generator.writeString(processor.getSamplesCSV());

            generator.writeEndObject();
            generator.close();
            return out.toString();
        }
        catch (IOException e) {
            log.error(e);
        }

        return null;
    }

    private static final class CSVOutputProcessor implements SampleProcessor
    {
        final StringBuilder builder = new StringBuilder();
        boolean firstSamples = true;

        /**
         * Process sampleCount sequential samples with identical values.  sampleCount will usually be 1,
         * but may be larger than 1.  Implementors may just loop processing identical values, but some
         * implementations may optimize adding a bunch of repeated values
         *
         * @param timestamps   a TimelineTimestamps instance, indexed by sample number to get the time at which the sample was captured.
         * @param sampleNumber the number of the sample within the timeline, used to index timestamps
         * @param sampleCount  the count of sequential, identical values
         * @param opcode       the opcode of the sample value, which may not be a REPEAT opcode
         * @param value        the value of this kind of sample over the count of samples starting at the time
         *                     given by the sampleNumber indexing the TimelineTimestamps.
         */
        @Override
        public void processSamples(final TimelineTimes timestamps, final int sampleNumber, final int sampleCount, final SampleOpcode opcode, final Object value)
        {
            // Create a CSV of: timestamp, sample value, timestamp, sample value, etc.
            for (int i = 0; i < sampleCount; i++) {
                if (!firstSamples) {
                    builder.append(",");
                }
                else {
                    firstSamples = false;
                }

                builder
                    .append(TimelineTimes.unixSeconds(timestamps.getSampleTimestamp(sampleNumber + i)))
                    .append(",")
                    .append(value == null ? 0 : value.toString());
            }
        }

        public String getSamplesCSV()
        {
            return builder.toString();
        }
    }
}
