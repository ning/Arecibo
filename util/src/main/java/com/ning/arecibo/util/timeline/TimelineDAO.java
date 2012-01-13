package com.ning.arecibo.util.timeline;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.skife.jdbi.v2.Folder2;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.IntegerMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TimelineDAO
{
    private static final String PACKAGE = TimelineDAO.class.getName();
    private final IDBI dbi;

    @Inject
    public TimelineDAO(@Named("collector_db") IDBI dbi)
    {
        this.dbi = dbi;
    }

    /**
     * Get the full collection of hosts, as a BiMap that lets us look up
     * by host id
     */
    public BiMap<Integer, String> getHosts()
    {
        return dbi.withHandle(new HandleCallback<BiMap<Integer, String>>()
        {
            @Override
            public BiMap<Integer, String> withHandle(Handle handle) throws Exception
            {
                return handle
                    .createQuery("select host_id, host_name from hosts")
                    .fold(makeBiMap(), new Folder2<BiMap<Integer, String>>()
                    {
                        @Override
                        public BiMap<Integer, String> fold(BiMap<Integer, String> accumulator, ResultSet rs, StatementContext ctx) throws SQLException
                        {
                            final int hostId = rs.getInt(1);
                            final String host = rs.getString(2);
                            accumulator.put(hostId, host);
                            return accumulator;
                        }
                    });
            }
        });
    }

    public int addHost(final String host)
    {
        return dbi.withHandle(new HandleCallback<Integer>()
        {
            @Override
            public Integer withHandle(Handle handle) throws Exception
            {
                handle
                    .createStatement("insert into hosts (host_name, created_dt) values (:host_name, unix_timestamp())")
                    .bind("host_name", host)
                    .execute();
                return handle.createQuery("select last_insert_id()")
                    .map(IntegerMapper.FIRST)
                    .first();
            }
        });
    }

    public int addSampleKind(final String sampleKind)
    {
        return dbi.withHandle(new HandleCallback<Integer>()
        {
            @Override
            public Integer withHandle(Handle handle) throws Exception
            {
                handle
                    .createStatement("insert into sample_kinds (sample_kind) values (:sample_kind)")
                    .bind("sample_kind", sampleKind)
                    .execute();
                return handle
                        .createQuery("select last_insert_id()")
                    .map(IntegerMapper.FIRST)
                    .first();
            }
        });
    }

    private BiMap<Integer, String> makeBiMap()
    {
        return HashBiMap.create();
    }

    public BiMap<Integer, String> getSampleKinds()
    {
        return dbi.withHandle(new HandleCallback<BiMap<Integer, String>>()
        {
            @Override
            public BiMap<Integer, String> withHandle(Handle handle) throws Exception
            {
                return handle
                    .createQuery("select sample_kind_id, sample_kind from sample_kinds")
                    .fold(makeBiMap(), new Folder2<BiMap<Integer, String>>()
                    {

                        @Override
                        public BiMap<Integer, String> fold(BiMap<Integer, String> accumulator, ResultSet rs, StatementContext ctx) throws SQLException
                        {
                            final int sampleKindId = rs.getInt(0);
                            final String sampleKind = rs.getString(1);
                            accumulator.put(sampleKindId, sampleKind);
                            return accumulator;
                        }
                    });
            }
        });
    }

    public int insertTimelineTimes(final TimelineTimes timelineTimes)
    {
        return dbi.withHandle(new HandleCallback<Integer>()
        {
            @Override
            public Integer withHandle(Handle handle) throws Exception
            {
                handle
                    .createStatement("insert into timeline_times (host_id, start_time, end_time, count, times)" +
                        " values (:host_id, :start_time, :end_time, :count, :times)")
                    .bind("host_id", timelineTimes.getHostId())
                    .bind("start_time", TimelineTimes.unixSeconds(timelineTimes.getStartTime()))
                    .bind("end_time", TimelineTimes.unixSeconds(timelineTimes.getEndTime()))
                    .bind("count", timelineTimes.getSampleCount())
                    .bind("times", timelineTimes.getIntTimeArray())
                    .execute();
                return handle
                    .createQuery("select last_insert_id()")
                    .map(IntegerMapper.FIRST)
                    .first();
            }
        });
    }

    public int insertTimelineChunk(final TimelineChunk timelineChunk)
    {
        return dbi.withHandle(new HandleCallback<Integer>()
        {
            @Override
            public Integer withHandle(Handle handle) throws Exception
            {
                handle
                    .createStatement("insert into timeline_chunks (host_id, sample_kind_id, sample_count, timeline_times_id, sample_bytes)" +
                        "values (:host_id, :sample_kind_id, :sample_count, :timeline_times_id, :sample_bytes)")
                    .bind("host_id", timelineChunk.getHostId())
                    .bind("sample_kind_id", timelineChunk.getSampleKindId())
                    .bind("sample_count", timelineChunk.getSampleCount())
                    .bind("timeline_times_id", timelineChunk.getTimelineTimesId())
                    .bind("sample_bytes", timelineChunk.getSamples())
                    .execute();
                return handle
                    .createQuery("select last_insert_id()")
                    .map(IntegerMapper.FIRST)
                    .first();
            }
        });
    }
}
