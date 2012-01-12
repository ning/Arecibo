package com.ning.arecibo.util.timeline;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.name.Named;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.Folder2;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.IntegerMapper;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;

public class TimelineDAO {
    private static final String PACKAGE = TimelineDAO.class.getName();
    private static final Folder2<List<TimelineChunk>> timelineFolder = new Folder2<List<TimelineChunk>>() {

        @Override
        public List<TimelineChunk> fold(List<TimelineChunk>  accumulator, ResultSet rs, StatementContext ctx) throws SQLException {
            accumulator.add(TimelineChunk.mapper.map(0, rs, ctx));
            return accumulator;
        }

    };

    private static final Folder2<List<TimelineTimes>> timelineTimesFolder = new Folder2<List<TimelineTimes>>() {

        @Override
        public List<TimelineTimes> fold(List<TimelineTimes>  accumulator, ResultSet rs, StatementContext ctx) throws SQLException {
            accumulator.add(TimelineTimes.mapper.map(0, rs, ctx));
            return accumulator;
        }

    };

    private final IDBI dbi;

    @Inject
    public TimelineDAO(@Named("collector_db") IDBI dbi) {
        this.dbi = dbi;
    }

    /**
     * Get the full collection of hosts, as a BiMap that lets us look up
     * by host id
     * @return
     */
    public BiMap<Integer, String> getHosts() {
        return dbi.withHandle(new HandleCallback<BiMap<Integer, String>>() {

            @Override
            public BiMap<Integer, String> withHandle(Handle handle) throws Exception {
                return handle.createQuery(PACKAGE + ":getVersionCounters")
                .fold(makeBiMap(), new Folder2<BiMap<Integer, String>>() {

                    @Override
                    public BiMap<Integer, String> fold(BiMap<Integer, String> accumulator, ResultSet rs, StatementContext ctx) throws SQLException {
                        final int hostId = rs.getInt("host_id");
                        final String host = rs.getString("host");
                        accumulator.put(hostId, host);
                        return accumulator;
                    }

                });
            }
        });

    }

    public int addHost(final String host) {
        return dbi.withHandle(new HandleCallback<Integer>() {

            @Override
            public Integer withHandle(Handle handle) throws Exception {
                handle.createStatement(PACKAGE + ":addHost")
                    .bind("host_name", host)
                    .execute();
                return handle.createQuery(PACKAGE + ":getLastInsertedId")
                    .map(IntegerMapper.FIRST)
                    .first();
            }

        });
    }

    public int addSampleKind(final String sampleKind) {
        return dbi.withHandle(new HandleCallback<Integer>() {

            @Override
            public Integer withHandle(Handle handle) throws Exception {
                handle.createStatement(PACKAGE + ":addSampleKind")
                    .bind("sample_kind", sampleKind)
                    .execute();
                return handle.createQuery(PACKAGE + ":getLastInsertedId")
                    .map(IntegerMapper.FIRST)
                    .first();
            }

        });
    }

    private BiMap<Integer, String> makeBiMap() {
        return HashBiMap.create();
    }

    public BiMap<Integer, String> getSampleKinds() {
        return dbi.withHandle(new HandleCallback<BiMap<Integer, String>>() {

            @Override
            public BiMap<Integer, String> withHandle(Handle handle) throws Exception {
                return handle.createQuery(PACKAGE + ":getSampleKinds")
                .fold(makeBiMap(), new Folder2<BiMap<Integer, String>>() {

                    @Override
                    public BiMap<Integer, String> fold(BiMap<Integer, String> accumulator, ResultSet rs, StatementContext ctx) throws SQLException {
                        final int sampleKindId = rs.getInt("sample_kind_id");
                        final String sampleKind = rs.getString("sample_kind");
                        accumulator.put(sampleKindId, sampleKind);
                        return accumulator;
                    }

                });
            }
        });

    }

    public List<TimelineChunk> getSampleKindTimelinesForHosts(final int sampleKindId, final List<String> hosts, final DateTime startTime, final DateTime endTime) {
        return dbi.withHandle(new HandleCallback<List<TimelineChunk>>() {

            @Override
            public List<TimelineChunk> withHandle(Handle handle) throws Exception {
                return handle.createQuery(PACKAGE + ":getSampleKindTimelinesForHosts")
                .define("hosts",hosts)
                .bind("sample_kind_id", sampleKindId)
                .bind("start_time", TimelineTimes.unixSeconds(startTime))
                .bind("end_time", TimelineTimes.unixSeconds(endTime))
                .fold(makeTimelineChunkList(), timelineFolder);
            }
        });

    }

    public List<TimelineChunk> getSampleKindTimelinesById(final List<Long> objectIds) {
        return dbi.withHandle(new HandleCallback<List<TimelineChunk>>() {

            @Override
            public List<TimelineChunk> withHandle(Handle handle) throws Exception {
                return handle.createQuery(PACKAGE + ":getSampleKindTimelinesById")
                .define("object_ids", objectIds)
                .fold(makeTimelineChunkList(), timelineFolder);
            }
        });

    }

    public List<TimelineTimes> getTimelineTimesForHosts(final int sampleKindId, final List<String> hosts, final DateTime startTime, final DateTime endTime) {
        return dbi.withHandle(new HandleCallback<List<TimelineTimes>>() {

            @Override
            public List<TimelineTimes> withHandle(Handle handle) throws Exception {
                return handle.createQuery(PACKAGE + ":getTimelinesForHosts")
                .define("hosts",hosts)
                .bind("sample_kind_id", sampleKindId)
                .bind("start_time", TimelineTimes.unixSeconds(startTime))
                .bind("end_time", TimelineTimes.unixSeconds(endTime))
                .fold(makeTimestampsList(), timelineTimesFolder);
            }
        });

    }

    public int insertTimelineTimes(final TimelineTimes timelineTimes) {
        return dbi.withHandle(new HandleCallback<Integer>() {

            @Override
            public Integer withHandle(Handle handle) throws Exception {
                handle.createStatement(PACKAGE + ":insertTimelineTimes")
                    .bind("host_id", timelineTimes.getHostId())
                    .bind("start_time", TimelineTimes.unixSeconds(timelineTimes.getStartTime()))
                    .bind("end_time", TimelineTimes.unixSeconds(timelineTimes.getEndTime()))
                    .bind("count", timelineTimes.getSampleCount())
                    .bind("times", timelineTimes.getIntTimeArray())
                    .execute();
                return handle.createQuery(PACKAGE + ":getLastInsertedId")
                    .map(IntegerMapper.FIRST)
                    .first();
            }
        });

    }

    public int insertTimelineChunk(final TimelineChunk timelineChunk) {
        return dbi.withHandle(new HandleCallback<Integer>() {

            @Override
            public Integer withHandle(Handle handle) throws Exception {
                handle.createStatement(PACKAGE + ":insertTimelineTimes")
                    .bind("host_id", timelineChunk.getHostId())
                    .bind("sample_kind_id", timelineChunk.getSampleKindId())
                    .bind("timeline_times_id", timelineChunk.getTimelineTimesId())
                    .bind("count", timelineChunk.getSampleCount())
                    .bind("times", timelineChunk.getSamples())
                    .execute();
                return handle.createQuery(PACKAGE + ":getLastInsertedId")
                    .map(IntegerMapper.FIRST)
                    .first();
            }
        });

    }

    public List<TimelineTimes> getTimelineTimesById(final List<Long> objectIds) {
        return dbi.withHandle(new HandleCallback<List<TimelineTimes>>() {

            @Override
            public List<TimelineTimes> withHandle(Handle handle) throws Exception {
                return handle.createQuery(PACKAGE + ":getSampleKindTimelinesById")
                .define("object_ids", objectIds)
                .fold(makeTimestampsList(), timelineTimesFolder);
            }
        });

    }

    private List<TimelineTimes> makeTimestampsList() {
        return new ArrayList<TimelineTimes>();
    }

    private List<TimelineChunk> makeTimelineChunkList() {
        return new ArrayList<TimelineChunk>();
    }
}
