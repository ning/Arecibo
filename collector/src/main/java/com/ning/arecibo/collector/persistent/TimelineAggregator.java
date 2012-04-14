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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.IDBI;
import org.weakref.jmx.Managed;

import com.google.inject.Inject;
import com.mogwee.executors.Executors;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.timeline.DefaultTimelineDAO;
import com.ning.arecibo.util.timeline.SampleCoder;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineCoder;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.CounterMetric;

/**
 * This class runs a thread that periodically looks for unaggregated timeline_times.
 * When it finds them, it creates a single new timeline_times object representing the
 * full sequence, then searches for all TimelineChunks referring to the original
 * timeline_times_ids and aggregates them
 * TODO: When combining timelines, don't just concat the time and the samples.
 * Instead, run a scanner so we can compress the case of successive identical repeats
 * in adjacent timelines.
 */
public class TimelineAggregator
{
    private static final Logger log = Logger.getLogger(TimelineAggregator.class);

    private final DefaultTimelineDAO timelineDao;
    private final CollectorConfig config;
    private final TimelineAggregatorDAO aggregatorDao;
    private final ScheduledExecutorService aggregatorThread = Executors.newSingleThreadScheduledExecutor("TimelineAggregator");

    private Map<String, CounterMetric> aggregatorCounters = new HashMap<String, CounterMetric>();

    private final AtomicBoolean isAggregating = new AtomicBoolean(false);

    private final CounterMetric aggregatesCreated = makeCounter("aggregatesCreated");

    private final CounterMetric timelineChunksConsidered = makeCounter("timelineChunksConsidered");
    private final CounterMetric timelineChunksCombined = makeCounter("timelineChunksCombined");
    private final CounterMetric timelineChunksQueuedForCreation = makeCounter("timelineChunksQueuedForCreation");
    private final CounterMetric timelineChunksWritten = makeCounter("timelineChunksWritten");
    private final CounterMetric timelineChunksInvalidatedOrDeleted = makeCounter("timelineChunksInvalidatedOrDeleted");
    private final CounterMetric timelineChunksBytesCreated = makeCounter("timelineChunksBytesCreated");

    // These lists support batching of aggregated chunk writes and updates or deletes of the chunks aggregated
    private final List<TimelineChunk> chunksToWrite = new ArrayList<TimelineChunk>();
    private final List<Long> chunkIdsToInvalidateOrDelete = new ArrayList<Long>();

    @Inject
    public TimelineAggregator(final IDBI dbi, final DefaultTimelineDAO timelineDao, final CollectorConfig config)
    {
        this.timelineDao = timelineDao;
        this.config = config;
        this.aggregatorDao = dbi.onDemand(TimelineAggregatorDAO.class);
    }

    private int aggregateTimelineCandidates(final List<TimelineChunk> timelineChunkCandidates, final int aggregationLevel, final int chunksToAggregate)
    {
        final TimelineChunk firstCandidate = timelineChunkCandidates.get(0);
        log.debug("For host_id {}, sampleKindId {}, looking to aggregate {} candidates in {} chunks",
                new Object[]{firstCandidate.getHostId(), firstCandidate.getSampleKindId(), timelineChunkCandidates.size(), chunksToAggregate});

        int aggregatesCreated = 0;
        while (timelineChunkCandidates.size() >= chunksToAggregate) {
            final List<TimelineChunk> chunkCandidates = timelineChunkCandidates.subList(0, chunksToAggregate);
            timelineChunksCombined.inc(chunksToAggregate);
            try {
                aggregateHostSampleChunks(chunkCandidates, aggregationLevel);
            }
            catch (IOException e) {
                log.error(e, "IOException aggregating {} chunks, host_id {}, sampleKindId {}, looking to aggregate {} candidates in {} chunks",
                        new Object[]{firstCandidate.getHostId(), firstCandidate.getSampleKindId(), timelineChunkCandidates.size(), chunksToAggregate});
            }
            aggregatesCreated++;
            chunkCandidates.clear();
        }
        return aggregatesCreated;
    }

    /**
     * The sequence of events is:
     * <ul>
     * <li>Build the aggregated TimelineChunk object, and save it, setting not_valid to true, and
     * aggregation_level to 1.  This means that it won't be noticed by any of the dashboard
     * queries.  The save operation returns the new timeline_times_id</li>
     * <li>Then, in a single transaction, update the aggregated TimelineChunk object to have not_valid = 0,
     * and also delete the TimelineChunk objects that were the basis of the aggregation, and flush
     * any TimelineChunks that happen to be in the cache.</li>
     * <p/>
     *
     * @param timelineChunks the TimelineChunks to be aggregated
     */
    private void aggregateHostSampleChunks(final List<TimelineChunk> timelineChunks, final int aggregationLevel) throws IOException
    {
        final TimelineChunk firstTimesChunk = timelineChunks.get(0);
        final TimelineChunk lastTimesChunk = timelineChunks.get(timelineChunks.size() - 1);
        final int chunkCount = timelineChunks.size();
        final int hostId = firstTimesChunk.getHostId();
        final DateTime startTime = firstTimesChunk.getStartTime();
        final DateTime endTime = lastTimesChunk.getEndTime();
        final List<byte[]> timeParts = new ArrayList<byte[]>(chunkCount);
        final List<byte[]> sampleParts = new ArrayList<byte[]>(chunkCount);
        final List<Long> timelineChunkIds = new ArrayList<Long>(chunkCount);
        int sampleCount = 0;
        for (final TimelineChunk timelineChunk : timelineChunks) {
            timeParts.add(timelineChunk.getTimes());
            sampleParts.add(timelineChunk.getSamples());
            sampleCount += timelineChunk.getSampleCount();
            timelineChunkIds.add(timelineChunk.getObjectId());
        }
        final byte[] combinedTimeBytes = TimelineCoder.combineTimelines(timeParts);
        final byte[] combinedSampleBytes = SampleCoder.combineSampleBytes(sampleParts);
        final int timeBytesLength = combinedTimeBytes.length;
        final int totalSize = 4 + timeBytesLength + combinedSampleBytes.length;
        log.debug("For hostId {}, aggregationLevel {}, aggregating {} timelines ({} bytes, {} samples): {}",
            new Object[]{firstTimesChunk.getHostId(), firstTimesChunk.getAggregationLevel(), timelineChunks.size(), totalSize, sampleCount});
        timelineChunksBytesCreated.inc(totalSize);
        final int totalSampleCount = sampleCount;
        final TimelineChunk chunk = new TimelineChunk(0, hostId, firstTimesChunk.getSampleKindId(), startTime, endTime,
                combinedTimeBytes, combinedSampleBytes, totalSampleCount, aggregationLevel + 1, false);
        chunksToWrite.add(chunk);
        chunkIdsToInvalidateOrDelete.addAll(timelineChunkIds);
        timelineChunksQueuedForCreation.inc();

        if (chunkIdsToInvalidateOrDelete.size() >= config.getMaxChunkIdsToInvalidateOrDelete()) {
            performWrites();
        }
    }

    private void performWrites()
    {
        // This is the atomic operation: set the new aggregated TimelineChunk object valid, and the
        // ones that were aggregated invalid.  This should be very fast.
        aggregatorDao.begin();
        timelineDao.bulkInsertTimelineChunks(chunksToWrite);
        if (config.getDeleteAggregatedChunks()) {
            aggregatorDao.deleteTimelineChunks(chunkIdsToInvalidateOrDelete);
        }
        else {
            aggregatorDao.makeTimelineChunksInvalid(chunkIdsToInvalidateOrDelete);
        }
        aggregatorDao.commit();
        timelineChunksWritten.inc(chunksToWrite.size());
        timelineChunksInvalidatedOrDeleted.inc(chunkIdsToInvalidateOrDelete.size());
        chunksToWrite.clear();
        chunkIdsToInvalidateOrDelete.clear();
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

        final String[] chunkCountsToAggregate = config.getChunksToAggregate().split(",");
        for (int aggregationLevel=0; aggregationLevel<config.getMaxAggregationLevel(); aggregationLevel++) {
            final Map<String, Long> initialCounters = captureAggregatorCounters();
            final int chunkCountIndex = aggregationLevel >= chunkCountsToAggregate.length ? chunkCountsToAggregate.length - 1 : aggregationLevel;
            final int chunksToAggregate = Integer.parseInt(chunkCountsToAggregate[chunkCountIndex]);
            final List<TimelineChunk> timelineChunkCandidates = aggregatorDao.getTimelineAggregationCandidates(aggregationLevel, chunksToAggregate);

            // The candidates are ordered first by host_id, then by event_category, and finally by start_time
            // Loop pulling off the candidates for the first hostId and eventCategory
            int lastHostId = 0;
            int lastSampleKindId = 0;
            final long startingAggregatesCreated = aggregatesCreated.count();
            final List<TimelineChunk> hostTimelineCandidates = new ArrayList<TimelineChunk>();
            for (final TimelineChunk candidate : timelineChunkCandidates) {
                timelineChunksConsidered.inc();
                final int hostId = candidate.getHostId();
                final int sampleKindId = candidate.getSampleKindId();
                if (lastHostId == 0) {
                    lastHostId = hostId;
                    lastSampleKindId = sampleKindId;
                }
                if (lastHostId != hostId || lastSampleKindId != sampleKindId) {
                    aggregatesCreated.inc(aggregateTimelineCandidates(hostTimelineCandidates, aggregationLevel, chunksToAggregate));
                    hostTimelineCandidates.clear();
                    lastHostId = hostId;
                    lastSampleKindId = sampleKindId;
                }
                hostTimelineCandidates.add(candidate);
            }
            if (hostTimelineCandidates.size() > 0) {
                aggregatesCreated.inc(aggregateTimelineCandidates(hostTimelineCandidates, aggregationLevel, chunksToAggregate));
            }
            if (chunkIdsToInvalidateOrDelete.size() > 0) {
                performWrites();
            }
            final Map<String, Long> counterDeltas = subtractFromAggregatorCounters(initialCounters);
            final StringBuilder builder = new StringBuilder();
            builder.append("For aggregation level ").append(aggregationLevel);
            for (Map.Entry<String, Long> entry : counterDeltas.entrySet()) {
                builder.append(", ").append(entry.getKey()).append(": ").append(entry.getValue());
            }
            log.info(builder.toString());
            final long netAggregatesCreated = aggregatesCreated.count() - startingAggregatesCreated;
            if (netAggregatesCreated == 0) {
                log.info("Created no new aggregates, so skipping higher-level aggregations");
                break;
            }
        }

        log.info("Aggregation done");
        isAggregating.set(false);
    }

    private CounterMetric makeCounter(final String counterName)
    {
        final CounterMetric counter = Metrics.newCounter(TimelineAggregator.class, counterName);
        aggregatorCounters.put(counterName, counter);
        return counter;
    }

    private Map<String, Long> captureAggregatorCounters()
    {
        final Map<String, Long> counterValues = new HashMap<String, Long>();
        for (Map.Entry<String, CounterMetric> entry : aggregatorCounters.entrySet()) {
            counterValues.put(entry.getKey(), entry.getValue().count());
        }
        return counterValues;
    }

    private Map<String, Long> subtractFromAggregatorCounters(final Map<String, Long> initialCounters)
    {
        final Map<String, Long> counterValues = new HashMap<String, Long>();
        for (Map.Entry<String, CounterMetric> entry : aggregatorCounters.entrySet()) {
            final String key = entry.getKey();
            counterValues.put(key, entry.getValue().count() - initialCounters.get(key));
        }
        return counterValues;
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
