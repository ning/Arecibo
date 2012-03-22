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

package com.ning.arecibo.collector.persistent;

import com.google.inject.Inject;
import com.mogwee.executors.Executors;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.util.timeline.DefaultTimelineDAO;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineTimes;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.CounterMetric;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.IDBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weakref.jmx.Managed;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class runs a thread that periodically looks for unaggregated timeline_times.
 * When it finds them, it creates a single new timeline_times object representing the
 * full sequence, then searches for all TimelineChunks referring to the original
 * timeline_times_ids and aggregates them
 * TODO: Use string templates rather than open-coding the SQL
 */
public class TimelineAggregator
{
    private static final Logger log = LoggerFactory.getLogger(TimelineAggregator.class);

    private final DefaultTimelineDAO timelineDao;
    private final CollectorConfig config;
    private final TimelineAggregatorDAO aggregatorDao;
    private final ScheduledExecutorService aggregatorThread = Executors.newSingleThreadScheduledExecutor("TimelineAggregator");

    private final AtomicBoolean isAggregating = new AtomicBoolean(false);
    private final CounterMetric aggregatesCreated = Metrics.newCounter(TimelineAggregator.class, "aggregates-created");

    @Inject
    public TimelineAggregator(final IDBI dbi, final DefaultTimelineDAO timelineDao, final CollectorConfig config)
    {
        this.timelineDao = timelineDao;
        this.config = config;
        this.aggregatorDao = dbi.onDemand(TimelineAggregatorDAO.class);
    }

    /**
     * This returns a list of lists of TimelineChunks.  Each of the lists is
     * a time-ordered sequence of chunks for one host and one sample kind.
     *
     * @param timelineTimesId the timelineTimes ids for the TimelineTimes chunks
     *                 to be aggregated
     * @return the list of lists of host/sample chunks for the supplied timelineTimesIds
     *         TODO: If we ever do multi-level aggregation, ordering by timeline_times_id
     *         isn't the same as ordering by start_time.  We'd really have to add a start_time
     *         column to the timeline_chunks table for that case, though we don't need it with
     *         single-level aggreation.  And we could always sort in Java memory by matching
     *         up with the timeline_times start times.
     */
    private List<List<TimelineChunk>> getHostSampleTimelineChunks(final List<Long> timelineTimesId)
    {
        final List<TimelineChunk> chunks = aggregatorDao.getTimelineChunksForTimelineTimes(timelineTimesId);

        final List<List<TimelineChunk>> orderedHostSampleChunks = new ArrayList<List<TimelineChunk>>();
        int lastHostId = 0;
        int lastSampleId = 0;
        List<TimelineChunk> hostSampleChunks = new ArrayList<TimelineChunk>();
        for (final TimelineChunk chunk : chunks) {
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

    private int aggregateTimelineCandidates(final List<TimelineTimes> timelineTimesCandidates)
    {
        log.info("Looking to aggregate {} candidates in {} chunks", timelineTimesCandidates.size(), config.getChunksToAggregate());

        int aggregatesCreated = 0;
        final int chunksToAggregate = config.getChunksToAggregate();
        while (timelineTimesCandidates.size() >= chunksToAggregate) {
            final List<TimelineTimes> chunkCandidates = timelineTimesCandidates.subList(0, chunksToAggregate);
            aggregateHostSampleChunks(chunkCandidates);
            aggregatesCreated++;
            chunkCandidates.clear();
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
     * <p/>
     *
     * @param timelineTimesChunks the TimlineTime chunks to be aggregated
     */
    private void aggregateHostSampleChunks(final List<TimelineTimes> timelineTimesChunks)
    {
        final TimelineTimes firstTimesChunk = timelineTimesChunks.get(0);
        final TimelineTimes lastTimesChunk = timelineTimesChunks.get(timelineTimesChunks.size() - 1);
        final int hostId = firstTimesChunk.getHostId();
        final DateTime startTime = firstTimesChunk.getStartTime();
        final DateTime endTime = lastTimesChunk.getEndTime();
        // Compute the total size of the aggregated stuff
        int totalTimelineSize = 0;
        int sampleCount = 0;
        final List<Long> timelineTimesIds = new ArrayList<Long>(timelineTimesChunks.size());
        for (final TimelineTimes timelineTimes : timelineTimesChunks) {
            totalTimelineSize += timelineTimes.getCompressedTimes().length;
            sampleCount += timelineTimes.getSampleCount();
            timelineTimesIds.add(timelineTimes.getObjectId());
        }
        log.info("Aggregating {} timelines ({} bytes, {} samples): {}",
            new Object[]{timelineTimesChunks.size(), totalTimelineSize, sampleCount, timelineTimesIds});

        final int totalSampleCount = sampleCount;
        final byte[] aggregatedTimes = new byte[totalTimelineSize];
        int timeChunkIndex = 0;
        for (final TimelineTimes chunk : timelineTimesChunks) {
            final int chunkTimeLength = chunk.getCompressedTimes().length;
            System.arraycopy(chunk.getCompressedTimes(), 0, aggregatedTimes, timeChunkIndex, chunkTimeLength);
            timeChunkIndex += chunkTimeLength;
        }

        aggregatorDao.begin();
        aggregatorDao.insertNewInvalidTimelineTimes(new TimelineTimes(-1, hostId, startTime, endTime, aggregatedTimes, totalSampleCount), 1);
        final int newTimelineTimesId = aggregatorDao.getLastInsertedId();
        aggregatorDao.commit();

        aggregateSampleChunks(timelineTimesIds, newTimelineTimesId, totalSampleCount);

        // This is the atomic operation: set the new aggregated TimelineTimes object valid, and the
        // ones that were aggregated invalid.  This should be very fast.
        aggregatorDao.begin();
        aggregatorDao.makeTimelineTimesValid(newTimelineTimesId);
        aggregatorDao.makeTimelineTimesInvalid(timelineTimesIds);
        aggregatorDao.commit();

        // TODO: Flush the cache of all entities with the given timelineTimesIds.
        // This will require remodularization of the LRUObjectCache, afaict.

        // Now (maybe) dispose of the old ones
        if (config.getDeleteAggregatedChunks()) {
            aggregatorDao.begin();
            // TODO: Could leave these around rather than deleting them, for testing purposes, since
            // they are already marked invalid
            aggregatorDao.deleteTimelineTimes(timelineTimesIds);
            // TODO: Could just leave these around for testing purposes, since they are only referenced
            // by timeline_times_id.
            aggregatorDao.deleteTimelineChunks(timelineTimesIds);
            aggregatorDao.commit();
        }
    }

    private void aggregateSampleChunks(final List<Long> timelineTimesId, final int newTimelineTimesId, final int totalSampleCount)
    {
        final List<List<TimelineChunk>> orderedHostSampleChunks = getHostSampleTimelineChunks(timelineTimesId);
        for (final List<TimelineChunk> chunkList : orderedHostSampleChunks) {
            final TimelineChunk firstSampleChunk = chunkList.get(0);
            int totalChunkSize = 0;
            for (final TimelineChunk chunk : chunkList) {
                totalChunkSize += chunk.getSamples().length;
            }
            final byte[] samples = new byte[totalChunkSize];
            int sampleChunkIndex = 0;
            for (final TimelineChunk chunk : chunkList) {
                final int chunkSampleLength = chunk.getSamples().length;
                System.arraycopy(chunk.getSamples(), 0, samples, sampleChunkIndex, chunkSampleLength);
                sampleChunkIndex += chunkSampleLength;
            }
            final TimelineChunk aggregatedChunk = new TimelineChunk(0,
                firstSampleChunk.getHostId(),
                firstSampleChunk.getSampleKindId(),
                newTimelineTimesId,
                firstSampleChunk.getStartTime(),
                samples,
                totalSampleCount);
            // No need to remember the TimelineChunkId
            timelineDao.insertTimelineChunk(aggregatedChunk);
        }
    }

    /**
     * This method aggregates candidate timelines
     */
    @Managed(description = "Aggregate candidate timelines")
    public void getAndProcessTimelineAggregationCandidates()
    {
        if (!isAggregating.compareAndSet(false, true)) {
            log.info("Asked to aggregate, but we're already aggregating!");
            return;
        }
        else {
            log.info("Starting aggregating");
        }

        final List<TimelineTimes> timelineTimesCandidates = aggregatorDao.getTimelineTimesAggregationCandidates();

        // The candidates are ordered first by host_id and second by start_time
        // Loop pulling off the candidates for the first host_id
        int lastHostId = 0;
        final List<TimelineTimes> hostTimelineCandidates = new ArrayList<TimelineTimes>();
        for (final TimelineTimes candidate : timelineTimesCandidates) {
            final int hostId = candidate.getHostId();
            if (lastHostId == 0) {
                lastHostId = hostId;
            }
            if (lastHostId != hostId) {
                aggregatesCreated.inc(aggregateTimelineCandidates(hostTimelineCandidates));
                hostTimelineCandidates.clear();
                lastHostId = hostId;
            }
            hostTimelineCandidates.add(candidate);
        }
        aggregatesCreated.inc(aggregateTimelineCandidates(hostTimelineCandidates));

        log.info("Aggregation done");
        isAggregating.set(false);
    }

    public void runAggregationThread()
    {
        aggregatorThread.scheduleWithFixedDelay(new Runnable()
        {
            @Override
            public void run()
            {
                getAndProcessTimelineAggregationCandidates();
            }
        },
            config.getAggregationInterval().getMillis(),
            config.getAggregationInterval().getMillis(),
            TimeUnit.MILLISECONDS);
    }

    public void stopAggregationThread()
    {
        aggregatorThread.shutdown();
    }
}
