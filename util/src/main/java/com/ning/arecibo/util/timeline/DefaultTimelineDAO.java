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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.ning.arecibo.util.Logger;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.Folder2;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.ResultIterator;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.IntegerMapper;
import org.skife.jdbi.v2.util.StringMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

// TODO Make queries stream-able
public class DefaultTimelineDAO implements TimelineDAO
{
    private static final Logger log = Logger.getLoggerViaExpensiveMagic();
    private static final int MAX_IN_ROW_BLOB_SIZE = 400;

    private final IDBI dbi;

    @Inject
    public DefaultTimelineDAO(final IDBI dbi)
    {
        this.dbi = dbi;
    }

    @Override
    public Integer getHostId(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.withHandle(new HandleCallback<Integer>()
        {
            @Override
            public Integer withHandle(final Handle handle) throws Exception
            {
                return handle
                    .createQuery("select host_id from hosts where host_name = :host_name")
                    .bind("host_name", host)
                    .map(IntegerMapper.FIRST)
                    .first();
            }
        });
    }

    @Override
    public String getHost(final Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.withHandle(new HandleCallback<String>()
        {
            @Override
            public String withHandle(final Handle handle) throws Exception
            {
                return handle
                    .createQuery("select host_name from hosts where host_id = :host_id")
                    .bind("host_id", hostId)
                    .map(StringMapper.FIRST)
                    .first();
            }
        });
    }

    @Override
    public BiMap<Integer, String> getHosts() throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.withHandle(new HandleCallback<BiMap<Integer, String>>()
        {
            @Override
            public BiMap<Integer, String> withHandle(final Handle handle) throws Exception
            {
                return handle
                    .createQuery("select host_id, host_name from hosts")
                    .fold(makeBiMap(), new Folder2<BiMap<Integer, String>>()
                    {
                        @Override
                        public BiMap<Integer, String> fold(final BiMap<Integer, String> accumulator, final ResultSet rs, final StatementContext ctx) throws SQLException
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

    @Override
    public Integer getOrAddHost(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.withHandle(new HandleCallback<Integer>()
        {
            @Override
            public Integer withHandle(final Handle handle) throws Exception
            {
                handle
                    .createStatement("insert ignore into hosts (host_name, created_dt) values (:host_name, unix_timestamp())")
                    .bind("host_name", host)
                    .execute();
                return getHostId(host);
            }
        });
    }

    @Override
    public Integer getSampleKindId(final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.withHandle(new HandleCallback<Integer>()
        {
            @Override
            public Integer withHandle(final Handle handle) throws Exception
            {
                return handle
                    .createQuery("select sample_kind_id from sample_kinds where sample_kind = :sample_kind")
                    .bind("sample_kind", sampleKind)
                    .map(IntegerMapper.FIRST)
                    .first();
            }
        });
    }

    @Override
    public String getSampleKind(final Integer sampleKindId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.withHandle(new HandleCallback<String>()
        {
            @Override
            public String withHandle(final Handle handle) throws Exception
            {
                return handle
                    .createQuery("select sample_kind from sample_kinds where sample_kind_id = :sample_kind_id")
                    .bind("sample_kind_id", sampleKindId)
                    .map(StringMapper.FIRST)
                    .first();
            }
        });
    }

    @Override
    public Integer getOrAddSampleKind(final Integer hostId, final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.withHandle(new HandleCallback<Integer>()
        {
            @Override
            public Integer withHandle(final Handle handle) throws Exception
            {
                handle
                    .createStatement("insert ignore into sample_kinds (sample_kind) values (:sample_kind)")
                    .bind("sample_kind", sampleKind)
                    .execute();
                return getSampleKindId(sampleKind);
            }
        });
    }

    @Override
    public Iterable<String> getSampleKindsByHostName(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.withHandle(new HandleCallback<List<String>>()
        {
            @Override
            public List<String> withHandle(final Handle handle) throws Exception
            {
                return handle
                    .createQuery(
                        "select distinct\n" +
                            "  k.sample_kind\n" +
                            "from timeline_chunks c\n" +
                            "join hosts h using (host_id)\n" +
                            "join sample_kinds k using (sample_kind_id)\n" +
                            "where h.host_name = :host_name\n" +
                            ";")
                    .bind("host_name", host)
                    .fold(new ArrayList<String>(), new Folder2<List<String>>()
                    {
                        @Override
                        public List<String> fold(final List<String> accumulator, final ResultSet rs, final StatementContext ctx) throws SQLException
                        {
                            final String sampleKind = rs.getString(1);
                            accumulator.add(sampleKind);
                            return accumulator;
                        }
                    });
            }
        });
    }

    @Override
    public BiMap<Integer, String> getSampleKinds() throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.withHandle(new HandleCallback<BiMap<Integer, String>>()
        {
            @Override
            public BiMap<Integer, String> withHandle(final Handle handle) throws Exception
            {
                return handle
                    .createQuery("select sample_kind_id, sample_kind from sample_kinds")
                    .fold(makeBiMap(), new Folder2<BiMap<Integer, String>>()
                    {
                        @Override
                        public BiMap<Integer, String> fold(final BiMap<Integer, String> accumulator, final ResultSet rs, final StatementContext ctx) throws SQLException
                        {
                            final int sampleKindId = rs.getInt(1);
                            final String sampleKind = rs.getString(2);
                            accumulator.put(sampleKindId, sampleKind);
                            return accumulator;
                        }
                    });
            }
        });
    }

    @Override
    public Integer insertTimelineTimes(final TimelineTimes timelineTimes) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.inTransaction(new TransactionCallback<Integer>()
        {
            @Override
            public Integer inTransaction(final Handle handle, final TransactionStatus status) throws Exception
            {
                final byte[] compressedTimes = timelineTimes.getCompressedTimes();
                final Update update = handle
                    .createStatement("insert into timeline_times (host_id, start_time, end_time, count, in_row_times, in_row_times, blob_times)" +
                        " values (:host_id, :start_time, :end_time, :count, :in_row_times, :blob_times)")
                    .bind("host_id", timelineTimes.getHostId())
                    .bind("start_time", TimelineTimes.unixSeconds(timelineTimes.getStartTime()))
                    .bind("end_time", TimelineTimes.unixSeconds(timelineTimes.getEndTime()))
                    .bind("count", timelineTimes.getSampleCount());
                // Use the in-row field if the blob is small enough
                if (compressedTimes.length > MAX_IN_ROW_BLOB_SIZE) {
                    update
                        .bindNull("in_row_times", Types.VARBINARY)
                        .bind("blob_times", compressedTimes);
                }
                else {
                    update
                        .bind("in_row_times", compressedTimes)
                        .bindNull("blob_times", Types.BLOB);
                }
                update
                    .execute();
                return handle
                    .createQuery("select last_insert_id()")
                    .map(IntegerMapper.FIRST)
                    .first();
            }
        });
    }

    @Override
    public Integer insertTimelineChunk(final TimelineChunk timelineChunk) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.inTransaction(new TransactionCallback<Integer>()
        {
            @Override
            public Integer inTransaction(final Handle handle, final TransactionStatus status) throws Exception
            {
                final Update update = handle
                    .createStatement("insert into timeline_chunks (host_id, sample_kind_id, sample_count, timeline_times_id, in_row_samples, blob_samples)" +
                        "values (:host_id, :sample_kind_id, :sample_count, :timeline_times_id, :in_row_samples, :blob_samples)")
                    .bind("host_id", timelineChunk.getHostId())
                    .bind("sample_kind_id", timelineChunk.getSampleKindId())
                    .bind("sample_count", timelineChunk.getSampleCount())
                    .bind("timeline_times_id", timelineChunk.getTimelineTimesId());
                final byte[] compressedSamples = timelineChunk.getSamples();
                if (compressedSamples.length > MAX_IN_ROW_BLOB_SIZE) {
                    update
                        .bindNull("in_row_samples", Types.VARBINARY)
                        .bind("blob_samples", compressedSamples);
                }
                else {
                    update
                        .bind("in_row_samples", compressedSamples)
                        .bindNull("blob_samples", Types.BLOB);
                }
                update
                    .execute();
                return handle
                    .createQuery("select last_insert_id()")
                    .map(IntegerMapper.FIRST)
                    .first();
            }
        });
    }

    @Override
    public List<TimelineChunkAndTimes> getSamplesByHostName(final String hostName, final DateTime startTime, final DateTime endTime) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.withHandle(new HandleCallback<List<TimelineChunkAndTimes>>()
        {
            @Override
            public List<TimelineChunkAndTimes> withHandle(final Handle handle) throws Exception
            {
                return handle
                    .createQuery(
                        "select distinct\n" +
                            "  h.host_id\n" +
                            ", h.host_name\n" +
                            ", k.sample_kind_id\n" +
                            ", k.sample_kind\n" +
                            ", c.sample_timeline_id\n" +
                            ", c.timeline_times_id\n" +
                            ", c.sample_count\n" +
                            ", c.in_row_samples\n" +
                            ", c.blob_samples\n" +
                            ", t.start_time\n" +
                            ", t.end_time\n" +
                            ", t.count\n" +
                            ", t.in_row_times\n" +
                            ", t.blob_times\n" +
                            "from timeline_chunks c\n" +
                            "join hosts h using (host_id)\n" +
                            "join sample_kinds k using (sample_kind_id)\n" +
                            "join timeline_times t using (timeline_times_id)\n" +
                            "where t.start_time >= :start_time\n" +
                            "and t.end_time <= :end_time\n" +
                            "and t.not_valid = 0\n" +
                            "and h.host_name = :host_name\n" +
                            "order by start_time asc\n" +
                            ";")
                    .bind("host_name", hostName)
                    .bind("start_time", TimelineTimes.unixSeconds(startTime))
                    .bind("end_time", TimelineTimes.unixSeconds(endTime))
                    .fold(new ArrayList<TimelineChunkAndTimes>(), TimelineChunkAndTimes.folder);
            }
        });
    }

    @Override
    public List<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(final String hostName, final String sampleKind, final DateTime startTime, final DateTime endTime) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return dbi.withHandle(new HandleCallback<List<TimelineChunkAndTimes>>()
        {
            @Override
            public List<TimelineChunkAndTimes> withHandle(final Handle handle) throws Exception
            {
                return handle
                    .createQuery(
                        "select distinct\n" +
                            "  h.host_id\n" +
                            ", h.host_name\n" +
                            ", k.sample_kind_id\n" +
                            ", k.sample_kind\n" +
                            ", c.sample_timeline_id\n" +
                            ", c.timeline_times_id\n" +
                            ", c.sample_count\n" +
                            ", c.in_row_samples\n" +
                            ", c.blob_samples\n" +
                            ", t.start_time\n" +
                            ", t.end_time\n" +
                            ", t.count\n" +
                            ", t.in_row_times\n" +
                            ", t.blob_times\n" +
                            "from timeline_chunks c\n" +
                            "join hosts h using (host_id)\n" +
                            "join sample_kinds k using (sample_kind_id)\n" +
                            "join timeline_times t using (timeline_times_id)\n" +
                            "where t.start_time >= :start_time\n" +
                            "and t.end_time <= :end_time\n" +
                            "and h.host_name = :host_name\n" +
                            "and k.sample_kind = :sample_kind\n" +
                            "and t.not_valid = 0\n" +
                            "order by start_time asc\n" +
                            ";")
                    .bind("host_name", hostName)
                    .bind("sample_kind", sampleKind)
                    .bind("start_time", TimelineTimes.unixSeconds(startTime))
                    .bind("end_time", TimelineTimes.unixSeconds(endTime))
                    .fold(new ArrayList<TimelineChunkAndTimes>(), TimelineChunkAndTimes.folder);
            }
        });
    }

    @Override
    // TODO: Using strings instead of string templates for this stuff is really ugly.
    // Is there some reason this DAO doesn't use string templates?
    public void getSamplesByHostNamesAndSampleKinds(final List<String> hostNames,
                                                    @Nullable final List<String> sampleKinds,
                                                    final DateTime startTime,
                                                    final DateTime endTime,
                                                    final TimelineChunkAndTimesConsumer chunkConsumer) throws UnableToObtainConnectionException, CallbackFailedException
    {
        final String hostNameStrings = stringifyList(hostNames);
        final String sampleKindStrings = sampleKinds == null ? "" : stringifyList(sampleKinds);
        final String sampleKindPredicate = sampleKinds == null ? "" : String.format("and k.sample_kind in (%s)\n", sampleKindStrings);
        dbi.withHandle(new HandleCallback<Void>()
        {
            @Override
            public Void withHandle(final Handle handle) throws Exception
            {
                ResultIterator<TimelineChunkAndTimes> iterator = null;
                try {
                    iterator = handle
                        .createQuery(
                            "select distinct\n" +
                                "  h.host_id\n" +
                                ", h.host_name\n" +
                                ", k.sample_kind_id\n" +
                                ", k.sample_kind\n" +
                                ", c.sample_timeline_id\n" +
                                ", c.timeline_times_id\n" +
                                ", c.sample_count\n" +
                                ", c.in_row_samples\n" +
                                ", c.blob_samples\n" +
                                ", t.start_time\n" +
                                ", t.end_time\n" +
                                ", t.count\n" +
                                ", t.in_row_times\n" +
                                ", t.blob_times\n" +
                                "from timeline_chunks c\n" +
                                "join hosts h using (host_id)\n" +
                                "join sample_kinds k using (sample_kind_id)\n" +
                                "join timeline_times t using (timeline_times_id)\n" +
                                "where t.end_time >= :start_time\n" +
                                "and t.start_time <= :end_time\n" +
                                "and h.host_name in (" + hostNameStrings + ")\n" +
                                sampleKindPredicate +
                                "and t.not_valid = 0\n" +
                                "order by h.host_name, k.sample_kind, t.start_time asc\n" +
                                ";")
                        .bind("start_time", TimelineTimes.unixSeconds(startTime))
                        .bind("end_time", TimelineTimes.unixSeconds(endTime))
                        .map(TimelineChunkAndTimes.mapper)
                        .iterator();
                    while (iterator.hasNext()) {
                        chunkConsumer.processTimelineChunkAndTimes(iterator.next());
                    }
                    return null;
                }
                finally {
                    if (iterator != null) {
                        try {
                            iterator.close();
                        }
                        catch (Exception e) {
                            log.error("Exception closing TimelineChunkAndTimes iterator for hosts %s and sample %s", hostNameStrings, sampleKindStrings);
                        }
                    }
                }
            }
        });

    }

    private String stringifyList(final List<String> strings) {
        final StringBuilder builder = new StringBuilder();
        for (String string : strings) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append("'").append(string).append("'");
        }
        return builder.toString();
    }

    private BiMap<Integer, String> makeBiMap()
    {
        return HashBiMap.create();
    }
}
