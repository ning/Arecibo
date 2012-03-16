package com.ning.arecibo.collector.persistent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.IntegerMapper;

import com.google.inject.Inject;
import com.mogwee.executors.Executors;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.util.timeline.DefaultTimelineDAO;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineTimes;

/**
 * This class runs a thread that periodically looks for unaggregated timeline_times.
 * When it finds them, it creates a single new timeline_times object representing the
 * full sequence, then searches for all TimelineChunks referring to the original
 * timeline_times_ids and aggregates them
 * TODO: Use string templates rather than open-coding the SQL
 */
public class TimelineAggregator {
    private static ResultSetMapper<TimelineTimes> timelineTimesCandidateMapper = new ResultSetMapper<TimelineTimes>() {

        @Override
        public TimelineTimes map(int index, ResultSet r, StatementContext ctx) throws SQLException {
            return new TimelineTimes(r.getInt("host_id"),
                                              r.getInt("timeline_times_id"),
                                              TimelineTimes.dateTimeFromUnixSeconds(r.getInt("start_time")),
                                              TimelineTimes.dateTimeFromUnixSeconds(r.getInt("end_time")),
                                              r.getBytes("times"),
                                              r.getInt("sample_count"));
        }
    };

    private final IDBI dbi;
    private final DefaultTimelineDAO timelineDao;
    private final CollectorConfig config;

    @Inject
    public TimelineAggregator(final IDBI dbi, DefaultTimelineDAO timelineDao, CollectorConfig config) {
        this.dbi = dbi;
        this.timelineDao = timelineDao;
        this.config = config;
    }

    private List<TimelineTimes> getTimelineTimesAggregationCandidates() {
        return dbi.withHandle(new HandleCallback<List<TimelineTimes>>() {

            @Override
            public List<TimelineTimes> withHandle(Handle handle) throws Exception {
                return handle
                    .createQuery("select timeline_times_id, host_id, start_time, end_time, times, count from timeline_times " +
                                 "where not_valid = 0 and aggregation_level = 0 " +
                                 "order by host_id, start_time")
                    .map(timelineTimesCandidateMapper)
                    .list();

            }
        });

    }

    /**
     * This returns a list of lists of TimelineChunks.  Each of the lists is
     * a time-ordered sequence of chunks for one host and one sample kind.
     * @param timelineTimesIds the timelineTimes ids for the TimelineTimes chunks
     * to be aggregated
     * @return the list of lists of host/sample chunks for the supplied timelineTimesIds
     * TODO: If we ever do multi-level aggregation, ordering by timeline_times_id
     * isn't the same as ordering by start_time.  We'd really have to add a start_time
     * column to the timeline_chunks table for that case, though we don't need it with
     * single-level aggreation.  And we could always sort in Java memory by matching
     * up with the timeline_times start times.
     */
    private List<List<TimelineChunk>> getHostSampleTimelineChunks(final String idString) {
        final List<TimelineChunk> chunks = dbi.withHandle(new HandleCallback<List<TimelineChunk>>() {

            @Override
            public List<TimelineChunk> withHandle(Handle handle) throws Exception {
                return handle
                    .createQuery("select sample_timeline_id, host_id, sample_kind_id, timeline_times_id, sample_count, sample_bytes " +
                                 "from timeline_chunks " +
                                 "where timeline_times_id in (" + idString + ") " +
                                 "order by host_id, sample_kind_id, timeline_times_id")
                    .map(TimelineChunk.mapper)
                    .list();

            }
        });

        final List<List<TimelineChunk>> orderedHostSampleChunks = new ArrayList<List<TimelineChunk>>();
        int lastHostId = 0;
        int lastSampleId = 0;
        List<TimelineChunk> hostSampleChunks = new ArrayList<TimelineChunk>();
        for (TimelineChunk chunk : chunks) {
            final int sampleId = chunk.getSampleKindId();
            final int hostId = chunk.getHostId();
            if (lastHostId == 0 || lastHostId != hostId || lastSampleId != sampleId) {
                lastHostId = hostId;
                lastSampleId = sampleId;
                if (hostSampleChunks.size() > 0) {
                    orderedHostSampleChunks.add(hostSampleChunks);
                    hostSampleChunks = new ArrayList<TimelineChunk>();
                }
                hostSampleChunks.add(chunk);
            }
        }
        if (hostSampleChunks.size() > 0) {
            orderedHostSampleChunks.add(hostSampleChunks);
        }
        return orderedHostSampleChunks;
    }

    private int aggregateTimelineCandidates(final List<TimelineTimes> timelineTimesCandidates) {
        int aggregatesCreated = 0;
        final int chunksToAggregate = config.getChunksToAggregate();
        while (timelineTimesCandidates.size() >= chunksToAggregate) {
            final List<TimelineTimes> chunkCandidates = timelineTimesCandidates.subList(0, chunksToAggregate);
            aggregateHostSampleChunks(chunkCandidates);
            aggregatesCreated++;
        }
        return aggregatesCreated;
    }

    /**
     * The sequence of events is:
     * <ul>
     * <li>Build the aggregated TimelineTimes object, and save it, setting not_valid to true, and
     * aggregation_level to 1.  This means that it won't be noticed by any of the dashboard
     * queries.  The save operation returns the new timeline_times_id</li>
     * <li>Retrieve all sample chunks associated with the TimelineTimes objects were aggregating,
     * ordered by host_id, sample_kind_id and start_time.  Aggregate and save those, with
     * timeline_time_id of the newly-created aggregated TimelineTimes object</li>
     * <li>Then, in a single transaction, update the aggregated TimelineTimes object to have not_valid = 0,
     * and also delete the TimelineTimes objects that were the basis of the aggregation, and flush
     * any TimelineTime chunks that happen to be in the cache.</li>
     * <li>Finally, delete the sample chunks that we aggregated.  Since sample chunks are only accessed
     * by timeline_time_id, so the old sample chunks can no longer be referenced  Therefore they don't
     * need to be deleted.</li>
     * <p>
     * @param timelineTimesChunks the TimlineTime chunks to be aggregated
     */
    private void aggregateHostSampleChunks(final List<TimelineTimes> timelineTimesChunks) {
        final TimelineTimes firstTimesChunk = timelineTimesChunks.get(0);
        final TimelineTimes lastTimesChunk = timelineTimesChunks.get(timelineTimesChunks.size() - 1);
        final int hostId = firstTimesChunk.getHostId();
        final DateTime startTime = firstTimesChunk.getStartTime();
        final DateTime endTime = lastTimesChunk.getEndTime();
        // Compute the total size of the aggregated stuff
        int totalTimelineSize = 0;
        int sampleCount = 0;
        final List<Long> timelineTimesIds = new ArrayList<Long>(timelineTimesChunks.size());
        for (TimelineTimes timelineTimes : timelineTimesChunks) {
            totalTimelineSize += timelineTimes.getTimeArray().length;
            sampleCount += timelineTimes.getSampleCount();
            timelineTimesIds.add(timelineTimes.getObjectId());
        }
        final int totalSampleCount = sampleCount;
        final byte[] aggregatedTimes = new byte[totalTimelineSize];
        int timeChunkIndex = 0;
        for (TimelineTimes chunk : timelineTimesChunks) {
            final int chunkTimeLength = chunk.getTimeArray().length;
            System.arraycopy(chunk.getTimeArray(), 0, aggregatedTimes, timeChunkIndex, chunkTimeLength);
            timeChunkIndex += chunkTimeLength;
        }
        final int newTimelineTimesId = dbi.inTransaction(new TransactionCallback<Integer>() {

            @Override
            public Integer inTransaction(final Handle handle, final TransactionStatus status) throws Exception
            {
                handle
                    .createStatement("insert into timeline_times (host_id, start_time, end_time, count, times, aggregation_level, not_valid)" +
                        " values (:host_id, :start_time, :end_time, :count, :times, 1, 0)")
                    .bind("host_id", hostId)
                    .bind("start_time", TimelineTimes.unixSeconds(startTime))
                    .bind("end_time", TimelineTimes.unixSeconds(endTime))
                    .bind("count", totalSampleCount)
                    .bind("times", aggregatedTimes)
                    .execute();
                return handle
                    .createQuery("select last_insert_id()")
                    .map(IntegerMapper.FIRST)
                    .first();
            }

        });

        final StringBuilder builder = new StringBuilder();
        for (long timelineTimesId : timelineTimesIds) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append(timelineTimesId);
        }
        final String idString = builder.toString();
        aggregateSampleChunks(idString, newTimelineTimesId, totalSampleCount);

        // This is the atomic operation: set the new aggregated TimelineTimes object valid, and the
        // ones that were aggregated invalid.  This should be very fast.
        dbi.inTransaction(new TransactionCallback<Void>() {

            @Override
            public Void inTransaction(final Handle handle, final TransactionStatus status) throws Exception
            {
                handle
                    .createStatement("update timeline_times set not_valid = 0, aggregation_level = 1 " +
                                     "where timeline_times_id = :timeline_times_id")
                    .bind("timeline_times_id", newTimelineTimesId)
                    .execute();
                handle
                    .createStatement("update timeline_times set not_valid = 1 where timeline_times_id in (" + idString + ")")
                    .execute();
                return null;
            }

        });
        // TODO: Flush the cache of all entities with the given timelineTimesIds.
        // This will require remodularization of the LRUObjectCache, afaict.

        // Now (maybe) dispose of the old ones
        if (config.getDeleteAggregatedChunks()) {
            dbi.inTransaction(new TransactionCallback<Void>() {

                @Override
                public Void inTransaction(final Handle handle, final TransactionStatus status) throws Exception
                {
                    // TODO: Could leave these around rather than deleting them, for testing purposes, since
                    // they are already marked invalid
                    handle
                        .createStatement("delete from timeline_times where timeline_times_id in (" + idString + ")")
                        .execute();
                    // TODO: Could just leave these around for testing purposes, since they are only referenced
                    // by timeline_times_id.
                    handle
                        .createStatement("delete from timeline_chunks where timeline_times_id in (" + idString + ")")
                        .execute();
                    return null;
                }

            });

        }
    }

    private void aggregateSampleChunks(final String idString, final int newTimelineTimesId, final int totalSampleCount) {
        final List<List<TimelineChunk>> orderedHostSampleChunks = getHostSampleTimelineChunks(idString);
        for (List<TimelineChunk> chunkList : orderedHostSampleChunks) {
            final TimelineChunk firstSampleChunk = chunkList.get(0);
            int totalChunkSize = 0;
            for (TimelineChunk chunk : chunkList) {
                totalChunkSize += chunk.getSamples().length;
            }
            final byte[] samples = new byte[totalChunkSize];
            int sampleChunkIndex = 0;
            for (TimelineChunk chunk : chunkList) {
                final int chunkSampleLength = chunk.getSamples().length;
                System.arraycopy(chunk.getSamples(), 0, samples, sampleChunkIndex, chunkSampleLength);
                sampleChunkIndex += chunkSampleLength;
            }
            final TimelineChunk aggregatedChunk = new TimelineChunk(0,
                                                                    firstSampleChunk.getHostId(),
                                                                    firstSampleChunk.getSampleKindId(),
                                                                    newTimelineTimesId,
                                                                    samples,
                                                                    totalSampleCount);
            // No need to remember the TimelineChunkId
            timelineDao.insertTimelineChunk(aggregatedChunk);
        }
    }

    /**
     * This method aggregates candidate timelines
     * @return the count of timeline_times objects aggregated
     */
    private int getAndProcessTimelineAggregationCandidates() {
        final List<TimelineTimes> timelineTimesCandidates = getTimelineTimesAggregationCandidates();
        int aggregatesCreated = 0;
        // The candidates are ordered first by host_id and second by start_time
        // Loop pulling off the candidates for the first host_id
        int lastHostId = 0;
        final List<TimelineTimes> hostTimelineCandidates = new ArrayList<TimelineTimes>();
        for (TimelineTimes candidate : timelineTimesCandidates) {
            final int hostId = candidate.getHostId();
            if (lastHostId == 0) {
                lastHostId = hostId;
            }
            if (lastHostId != hostId) {
                aggregatesCreated += aggregateTimelineCandidates(hostTimelineCandidates);
                hostTimelineCandidates.clear();
                lastHostId = hostId;
            }
            hostTimelineCandidates.add(candidate);
        }
        aggregatesCreated += aggregateTimelineCandidates(hostTimelineCandidates);
        return aggregatesCreated;
    }

    public void runAggregationThread() {
        Executors.newSingleThreadScheduledExecutor("TimelineAggregator").scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                getAndProcessTimelineAggregationCandidates();
            }
        },
        config.getAggregationInterval().getMillis(),
        config.getAggregationInterval().getMillis(),
        TimeUnit.MILLISECONDS);
    }
}
