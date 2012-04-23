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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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

/**
 * This class runs a thread that periodically looks for unaggregated timelines.
 * When it finds them, it combines them intelligently as if they were originally
 * a single sequence of times.
 */
public class TimelineAggregator
{
    private static final Logger log = Logger.getLogger(TimelineAggregator.class);

    private final DefaultTimelineDAO timelineDao;
    private final CollectorConfig config;
    private final TimelineAggregatorDAO aggregatorDao;
    private final ScheduledExecutorService aggregatorThread = Executors.newSingleThreadScheduledExecutor("TimelineAggregator");

    private Map<String, AtomicLong> aggregatorCounters = new LinkedHashMap<String, AtomicLong>();

    private final AtomicBoolean isAggregating = new AtomicBoolean(false);

    private final AtomicLong aggregationRuns = makeCounter("runs");
    private final AtomicLong foundNothingRuns = makeCounter("found nothing");
    private final AtomicLong aggregatesCreated = makeCounter("aggsCreated");
    private final AtomicLong timelineChunksConsidered = makeCounter("chunksConsidered");
    private final AtomicLong timelineChunkBatchesProcessed = makeCounter("batchesProcessed");
    private final AtomicLong timelineChunksCombined = makeCounter("chunksCombined");
    private final AtomicLong timelineChunksQueuedForCreation = makeCounter("chunksQueued");
    private final AtomicLong timelineChunksWritten = makeCounter("chunksWritten");
    private final AtomicLong timelineChunksInvalidatedOrDeleted = makeCounter("chunksInvalidatedOrDeleted");
    private final AtomicLong timelineChunksBytesCreated = makeCounter("bytesCreated");
    private final AtomicLong msSpentAggregating = makeCounter("msSpentAggregating");
    private final AtomicLong msSpentSleeping = makeCounter("msSpentSleeping");

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
        final int hostId = firstCandidate.getHostId();
        final int sampleKindId = firstCandidate.getSampleKindId();
        log.debug("For host_id {}, sampleKindId {}, looking to aggregate {} candidates in {} chunks",
                new Object[]{hostId, sampleKindId, timelineChunkCandidates.size(), chunksToAggregate});
        int aggregatesCreated = 0;
        while (timelineChunkCandidates.size() >= chunksToAggregate) {
            final List<TimelineChunk> chunkCandidates = timelineChunkCandidates.subList(0, chunksToAggregate);
            timelineChunksCombined.addAndGet(chunksToAggregate);
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
        final byte[] combinedTimeBytes = TimelineCoder.combineTimelines(timeParts, sampleCount);
        final byte[] combinedSampleBytes = SampleCoder.combineSampleBytes(sampleParts);
        final int timeBytesLength = combinedTimeBytes.length;
        final int totalSize = 4 + timeBytesLength + combinedSampleBytes.length;
        log.debug("For hostId {}, aggregationLevel {}, aggregating {} timelines ({} bytes, {} samples): {}",
            new Object[]{firstTimesChunk.getHostId(), firstTimesChunk.getAggregationLevel(), timelineChunks.size(), totalSize, sampleCount});
        timelineChunksBytesCreated.addAndGet(totalSize);
        final int totalSampleCount = sampleCount;
        final TimelineChunk chunk = new TimelineChunk(0, hostId, firstTimesChunk.getSampleKindId(), startTime, endTime,
                combinedTimeBytes, combinedSampleBytes, totalSampleCount, aggregationLevel + 1, false, false);
        chunksToWrite.add(chunk);
        chunkIdsToInvalidateOrDelete.addAll(timelineChunkIds);
        timelineChunksQueuedForCreation.incrementAndGet();

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
        timelineChunksWritten.addAndGet(chunksToWrite.size());
        timelineChunksInvalidatedOrDeleted.addAndGet(chunkIdsToInvalidateOrDelete.size());
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
            log.debug("Starting aggregating");
        }

        aggregationRuns.incrementAndGet();
        final String[] chunkCountsToAggregate = config.getChunksToAggregate().split(",");
        for (int aggregationLevel=0; aggregationLevel<config.getMaxAggregationLevel(); aggregationLevel++) {
            final long startingAggregatesCreated = aggregatesCreated.get();
            final Map<String, Long> initialCounters = captureAggregatorCounters();
            final int chunkCountIndex = aggregationLevel >= chunkCountsToAggregate.length ? chunkCountsToAggregate.length - 1 : aggregationLevel;
            final int chunksToAggregate = Integer.parseInt(chunkCountsToAggregate[chunkCountIndex]);
            aggregateLevel(aggregationLevel, chunksToAggregate);
            final Map<String, Long> counterDeltas = subtractFromAggregatorCounters(initialCounters);
            final long netAggregatesCreated = aggregatesCreated.get() - startingAggregatesCreated;
            if (netAggregatesCreated == 0) {
                foundNothingRuns.incrementAndGet();
                log.debug("Created no new aggregates, so skipping higher-level aggregations");
                break;
            }
            else {
                final StringBuilder builder = new StringBuilder();
                builder.append("For aggregation level ").append(aggregationLevel);
                for (Map.Entry<String, Long> entry : counterDeltas.entrySet()) {
                    builder.append(", ").append(entry.getKey()).append(": ").append(entry.getValue());
                }
                log.info(builder.toString());
            }
        }

        log.debug("Aggregation done");
        isAggregating.set(false);
    }

    private void aggregateLevel(final int aggregationLevel, final int chunksToAggregate)
    {
        final int aggregationBatchSize = config.getAggregationBatchSize();
        while (true) {
            final long startTime = System.currentTimeMillis();
            try {
                final List<TimelineChunk> candidates = aggregatorDao.getTimelineAggregationCandidates(aggregationLevel, chunksToAggregate, aggregationBatchSize);
                if (candidates.size() == 0) {
                    break;
                }
                // The candidates are ordered first by host_id, then by sampleKindId, and finally by start_time
                // Loop pulling off the candidates for the first hostId and sampleKindId
                int lastHostId = 0;
                int lastSampleKindId = 0;
                final List<TimelineChunk> hostTimelineCandidates = new ArrayList<TimelineChunk>();
                for (final TimelineChunk candidate : candidates) {
                    timelineChunksConsidered.incrementAndGet();
                    final int hostId = candidate.getHostId();
                    final int sampleKindId = candidate.getSampleKindId();
                    if (lastHostId == 0) {
                        lastHostId = hostId;
                        lastSampleKindId = sampleKindId;
                    }
                    if (lastHostId != hostId || lastSampleKindId != sampleKindId) {
                        aggregatesCreated.addAndGet(aggregateTimelineCandidates(hostTimelineCandidates, aggregationLevel, chunksToAggregate));
                        hostTimelineCandidates.clear();
                        lastHostId = hostId;
                        lastSampleKindId = sampleKindId;
                    }
                    hostTimelineCandidates.add(candidate);
                }
                if (hostTimelineCandidates.size() > 0) {
                    aggregatesCreated.addAndGet(aggregateTimelineCandidates(hostTimelineCandidates, aggregationLevel, chunksToAggregate));
                }
                if (chunkIdsToInvalidateOrDelete.size() > 0) {
                    performWrites();
                }
            }
            finally {
                msSpentAggregating.addAndGet(System.currentTimeMillis() - startTime);
            }
            final long sleepTime = config.getAggregationSleepBetweenBatches().getMillis();
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                msSpentSleeping.addAndGet(sleepTime);
            }
            timelineChunkBatchesProcessed.incrementAndGet();
        }
    }

    private AtomicLong makeCounter(final String counterName)
    {
        final AtomicLong counter = new AtomicLong();
        aggregatorCounters.put(counterName, counter);
        return counter;
    }

    private Map<String, Long> captureAggregatorCounters()
    {
        final Map<String, Long> counterValues = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, AtomicLong> entry : aggregatorCounters.entrySet()) {
            counterValues.put(entry.getKey(), entry.getValue().get());
        }
        return counterValues;
    }

    private Map<String, Long> subtractFromAggregatorCounters(final Map<String, Long> initialCounters)
    {
        final Map<String, Long> counterValues = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, AtomicLong> entry : aggregatorCounters.entrySet()) {
            final String key = entry.getKey();
            counterValues.put(key, entry.getValue().get() - initialCounters.get(key));
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

    @Managed
    public long getAggregationRuns()
    {
        return aggregationRuns.get();
    }

    @Managed
    public long getFoundNothingRuns()
    {
        return foundNothingRuns.get();
    }

    @Managed
    public long getTimelineChunksConsidered()
    {
    return timelineChunksConsidered.get();
    }

    @Managed
    public long getTimelineChunkBatchesProcessed()
    {
    return timelineChunkBatchesProcessed.get();
    }

    @Managed
    public long getTimelineChunksCombined()
    {
    return timelineChunksCombined.get();
    }

    @Managed
    public long getTimelineChunksQueuedForCreation()
    {
    return timelineChunksQueuedForCreation.get();
    }

    @Managed
    public long getTimelineChunksWritten()
    {
    return timelineChunksWritten.get();
    }

    @Managed
    public long getTimelineChunksInvalidatedOrDeleted()
    {
    return timelineChunksInvalidatedOrDeleted.get();
    }

    @Managed
    public long getTimelineChunksBytesCreated()
    {
    return timelineChunksBytesCreated.get();
    }
}
