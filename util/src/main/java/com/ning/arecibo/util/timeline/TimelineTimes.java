package com.ning.arecibo.util.timeline;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.ning.arecibo.util.Logger;

public class TimelineTimes extends CachedObject {
    private static Logger log = Logger.getLogger(TimelineTimes.class);
    public static final ResultSetMapper<TimelineTimes> mapper = new ResultSetMapper<TimelineTimes>() {

        @Override
        public TimelineTimes map(int index, ResultSet rs, StatementContext ctx) throws SQLException {
            try {
                final int timelineIntervalId = rs.getInt("timeline_interval_id");
                final int hostId = rs.getInt("host_id");
                final DateTime startTime = TimelineTimes.dateTimeFromUnixSeconds(rs.getInt("start_time"));
                final DateTime endTime = TimelineTimes.dateTimeFromUnixSeconds(rs.getInt("end_time"));
                final int count = rs.getInt("count");
                final Blob blobTimes = rs.getBlob("timeline_times");
                final DataInputStream stream = new DataInputStream(blobTimes.getBinaryStream());
                final List<DateTime> timelineTimes = new ArrayList<DateTime>(count);
                for (int i=0; i<count; i++) {
                    timelineTimes.add(TimelineTimes.dateTimeFromUnixSeconds(stream.readInt()));
                }
                return new TimelineTimes(timelineIntervalId, hostId, startTime, endTime, timelineTimes);
            }
            catch (IOException e) {
                log.error(e, "Exception in accumulateTimelines()");
                return null;
            }
        }
    };

    private final int hostId;
    private final DateTime startTime;
    private final DateTime endTime;
    private final List<DateTime> times;

    public TimelineTimes(long timelineIntervalId, int hostId, DateTime startTime, DateTime endTime, List<DateTime> times) {
        super(timelineIntervalId);
        this.hostId = hostId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.times = times;
    }

    public int getHostId() {
        return hostId;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public DateTime getEndTime() {
        return endTime;
    }


    public int getSampleCount() {
        return times.size();
    }

    public DateTime getSampleTimestamp(final int sampleNumber) {
        if (sampleNumber < 0 || sampleNumber > times.size()) {
            return null;
        }
        else {
            return times.get(sampleNumber);
        }
    }

    public int getSampleNumberForTimestamp(final DateTime timestamp) {
        // TODO: do the binary search
        throw new IllegalArgumentException("NYI");
    }

    public static DateTime dateTimeFromUnixSeconds(final int unixTime) {
        return new DateTime(((long)unixTime) * 1000L);
    }

    public static int unixSeconds(final DateTime dateTime) {
        final long millis = dateTime.getMillis();
        return (int)(millis / 1000L);
    }
}
