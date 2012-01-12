package com.ning.arecibo.util.timeline;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

/**
 * Instances of this class represent timeline sequences read from the database
 * for a single host and single sample kind.  The samples are held in a byte
 * array.
 */
public class TimelineChunk extends CachedObject {
    public static final ResultSetMapper<TimelineChunk> mapper = new ResultSetMapper<TimelineChunk>() {

        @Override
        public TimelineChunk map(int index, ResultSet rs, StatementContext ctx) throws SQLException {
            final int hostId = rs.getInt("host_id");
            final int sampleKindId = rs.getInt("sample_kind_id");
            final int timelineIntervalId = rs.getInt("timeline_interval_id");
            final int sampleCount = rs.getInt("sample_count");
            final Blob blobSamples = rs.getBlob("timeline_times");
            final byte[] samples = blobSamples.getBytes(0, (int)blobSamples.length());
            return new TimelineChunk(hostId, sampleKindId, timelineIntervalId, samples, sampleCount);
        }
    };

    private final int hostId;
    private final int sampleKindId;
    private final byte[] samples;
    private final int sampleCount;
    private int timelineTimesId = 0;

    public TimelineChunk(int hostId, int sampleKindId, int timelineTimesId, byte[] samples, int sampleCount) {
        super(timelineTimesId);
        this.hostId = hostId;
        this.sampleKindId = sampleKindId;
        this.samples = samples;
        this.sampleCount = sampleCount;
    }

    public int getHostId() {
        return hostId;
    }

    public int getSampleKindId() {
        return sampleKindId;
    }

    public byte[] getSamples() {
        return samples;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setTimelineTimesId(int timelineTimesId) {
        this.timelineTimesId = timelineTimesId;
    }

    public int getTimelineTimesId() {
        return timelineTimesId;
    }
}
