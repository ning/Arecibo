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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.collector.process.EventHandler;
import com.ning.arecibo.collector.process.EventsUtils;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.ScalarSample;
import com.ning.arecibo.util.timeline.ShutdownSaveMode;
import com.ning.arecibo.util.timeline.StartTimes;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineChunkAccumulator;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.persistent.FileBackedBuffer;
import com.ning.arecibo.util.timeline.persistent.Replayer;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.CounterMetric;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weakref.jmx.Managed;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TimelineEventHandler implements EventHandler
{
    private static final Logger log = LoggerFactory.getLogger(TimelineEventHandler.class);
    private static Comparator<TimelineChunk> CHUNK_COMPARATOR = new Comparator<TimelineChunk>() {

        @Override
        public int compare(TimelineChunk o1, TimelineChunk o2) {
            final int hostDiff = o1.getHostId() - o1.getHostId();
            if (hostDiff < 0) {
                return -1;
            }
            else if (hostDiff > 0) {
                return 1;
            }
            else {
                final int sampleKindIdDiff = o1.getSampleKindId() - o2.getSampleKindId();
                if (sampleKindIdDiff < 0) {
                    return -1;
                }
                else if (sampleKindIdDiff > 0) {
                    return 1;
                }
                else {
                    final long startTimeDiff = o1.getStartTime().getMillis() - o2.getStartTime().getMillis();
                    if (startTimeDiff < 0) {
                        return -1;
                    }
                    else if (startTimeDiff > 0) {
                        return 1;
                    }
                    else {
                        return 0;
                    }
                }
            }
        }
    };

    // A TimelineHostEventAccumulator records attributes for a specific host and event type.
    // This cache maps hostId -> categoryId -> accumulator
    //
    // TODO: There are still timing windows in the use of accumulators.  Enumerate them and
    // either fix them or prove they are benign
    private final Map<Integer, Map<Integer, TimelineHostEventAccumulator>> accumulators = new ConcurrentHashMap<Integer, Map<Integer, TimelineHostEventAccumulator>>();

    private final CollectorConfig config;
    private final TimelineDAO timelineDAO;
    private final BackgroundDBChunkWriter backgroundWriter;
    private final FileBackedBuffer backingBuffer;

    private final ShutdownSaveMode shutdownSaveMode;
    private final AtomicBoolean shuttingDown = new AtomicBoolean();
    private final AtomicBoolean fastShutdown = new AtomicBoolean();

    private final AtomicLong eventsDiscarded = new AtomicLong(0L);
    private final CounterMetric eventsReceivedAfterShuttingDown = makeCounter("eventsReceivedAfterShuttingDown");
    private final CounterMetric handledEventCount = makeCounter("handledEventCount");
    private final CounterMetric addedHostEventAccumulatorMapCount = makeCounter("addedHostEventAccumulatorMapCount");
    private final CounterMetric addedHostEventAccumulatorCount = makeCounter("addedHostEventAccumulatorCount");
    private final CounterMetric getInMemoryChunksCallCount = makeCounter("getInMemoryChunksCallCount");
    private final CounterMetric accumulatorDeepCopyCount = makeCounter("accumulatorDeepCopyCount");
    private final CounterMetric inMemoryChunksReturnedCount = makeCounter("inMemoryChunksReturnedCount");
    private final CounterMetric replayCount = makeCounter("replayCount");
    private final CounterMetric replaySamplesFoundCount = makeCounter("replaySamplesFoundCount");
    private final CounterMetric replaySamplesOutsideTimeRangeCount = makeCounter("replaySamplesOutsideTimeRangeCount");
    private final CounterMetric replaySamplesProcessedCount = makeCounter("replaySamplesProcessedCount");
    private final CounterMetric forceCommitCallCount = makeCounter("forceCommitCallCount");

    private StartTimes startTimes = null;

    @Inject
    public TimelineEventHandler(final CollectorConfig config, final TimelineDAO timelineDAO, final BackgroundDBChunkWriter backgroundWriter, final FileBackedBuffer fileBackedBuffer)
    {
        this.config = config;
        this.timelineDAO = timelineDAO;
        this.backgroundWriter = backgroundWriter;
        this.backingBuffer = fileBackedBuffer;
        this.shutdownSaveMode = ShutdownSaveMode.fromString(config.getShutdownSaveMode());
    }

    private CounterMetric makeCounter(final String counterName)
    {
        return Metrics.newCounter(TimelineEventHandler.class, counterName);
    }

    private void saveAccumulatorsOrStartTimes()
    {
        for (Map.Entry<Integer, Map<Integer, TimelineHostEventAccumulator>> entry : accumulators.entrySet()) {
            final int hostId = entry.getKey();
            final Map<Integer, TimelineHostEventAccumulator> hostAccumulators = entry.getValue();
            for (Map.Entry<Integer, TimelineHostEventAccumulator> accumulatorEntry : hostAccumulators.entrySet()) {
                final int categoryId = accumulatorEntry.getKey();
                final TimelineHostEventAccumulator accumulator = accumulatorEntry.getValue();
                if (fastShutdown.get()) {
                    log.debug("Saving Timeline start time for hostId [{}] and category [{}]", hostId, categoryId);
                    startTimes.addTime(hostId, categoryId, accumulator.getStartTime());
                }
                else {
                    log.debug("Saving Timeline for hostId [{}] and categoryId [{}]", hostId, categoryId);
                    accumulator.extractAndQueueTimelineChunks();
                }
            }
        }
    }

    @Override
    public void handle(final Event event)
    {
        if (shuttingDown.get()) {
            eventsReceivedAfterShuttingDown.inc();
            return;
        }
        try {
            handledEventCount.inc();
            // Lookup the host id
            final String hostName = EventsUtils.getHostNameFromEvent(event);
            final Integer hostId = timelineDAO.getOrAddHost(hostName);

            // Extract and parse samples
            final Map<String, Object> samples = EventsUtils.getSamplesFromEvent(event);
            final Map<Integer, ScalarSample> scalarSamples = new LinkedHashMap<Integer, ScalarSample>();
            convertSamplesToScalarSamples(hostId, event.getEventType(), samples, scalarSamples);

            if (scalarSamples.isEmpty()) {
                eventsDiscarded.incrementAndGet();
                log.warn("Invalid event: " + event);
                return;
            }

            final HostSamplesForTimestamp hostSamples = new HostSamplesForTimestamp(hostId, event.getEventType(), new DateTime(event.getTimestamp(), DateTimeZone.UTC), scalarSamples);
            // Start by saving locally the samples
            backingBuffer.append(hostSamples);
            // Then add them to the in-memory accumulator
            processSamples(hostSamples);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized TimelineHostEventAccumulator getOrAddHostEventAccumulator(final int hostId, final int categoryId)
    {
        Map<Integer, TimelineHostEventAccumulator> hostAccumulators = accumulators.get(hostId);
        if (hostAccumulators == null) {
            addedHostEventAccumulatorMapCount.inc();
            hostAccumulators = new HashMap<Integer, TimelineHostEventAccumulator>();
            accumulators.put(hostId, hostAccumulators);
        }
        TimelineHostEventAccumulator accumulator = hostAccumulators.get(categoryId);
        if (accumulator == null) {
            addedHostEventAccumulatorCount.inc();
            accumulator = new TimelineHostEventAccumulator(timelineDAO, backgroundWriter, hostId, categoryId,
                    config.getTimelinesVerboseStats(), (int)config.getTimelineLength().getMillis());
            hostAccumulators.put(categoryId, accumulator);
            log.debug("Created new Timeline for hostId [{}] and category [{}]", hostId, categoryId);
        }
        return accumulator;
    }

    private void processSamples(final HostSamplesForTimestamp hostSamples) throws ExecutionException, IOException
    {
        final int hostId = hostSamples.getHostId();
        final String category = hostSamples.getCategory();
        final int categoryId = timelineDAO.getEventCategoryId(category);
        final TimelineHostEventAccumulator accumulator = getOrAddHostEventAccumulator(hostId, categoryId);
        accumulator.addHostSamples(hostSamples);
    }

    public Collection<? extends TimelineChunk> getInMemoryTimelineChunks(final Integer hostId, @Nullable final DateTime filterStartTime, @Nullable final DateTime filterEndTime) throws IOException, ExecutionException
    {
        return getInMemoryTimelineChunks(hostId, ImmutableList.copyOf(timelineDAO.getSampleKindIdsByHostId(hostId)), filterStartTime, filterEndTime);
    }

    public Collection<? extends TimelineChunk> getInMemoryTimelineChunks(final Integer hostId, final Integer sampleKindId, @Nullable final DateTime filterStartTime, @Nullable final DateTime filterEndTime) throws IOException, ExecutionException
    {
        return getInMemoryTimelineChunks(hostId, ImmutableList.<Integer>of(sampleKindId), filterStartTime, filterEndTime);
    }

    public Collection<? extends TimelineChunk> getInMemoryTimelineChunks(final Integer hostId, final List<Integer> sampleKindIds, @Nullable final DateTime filterStartTime, @Nullable final DateTime filterEndTime) throws IOException, ExecutionException
    {
        getInMemoryChunksCallCount.inc();
        // Check first if there is an in-memory accumulator for this host
        final Map<Integer, TimelineHostEventAccumulator> hostAccumulators = accumulators.get(hostId);
        if (hostAccumulators == null) {
            return ImmutableList.of();
        }

        // Now, filter each accumulator for this host
        final List<TimelineChunk> samplesByHostName = new ArrayList<TimelineChunk>();
        for (final TimelineHostEventAccumulator accumulator : hostAccumulators.values()) {
            for (TimelineChunk chunk : accumulator.getPendingTimelineChunks()) {
                if ((filterStartTime != null && chunk.getEndTime().isBefore(filterStartTime)) || (filterStartTime != null && chunk.getStartTime().isAfter(filterEndTime))) {
                    continue;
                }
                else {
                    samplesByHostName.add(chunk);
                }
            }
            final List<DateTime> accumulatorTimes = accumulator.getTimes();
            if (accumulatorTimes.size() == 0) {
                continue;
            }
            final DateTime accumulatorStartTime = accumulator.getStartTime();
            final DateTime accumulatorEndTime = accumulator.getEndTime();

            // Check if the time filters apply
            if ((filterStartTime != null && accumulatorEndTime.isBefore(filterStartTime)) || (filterStartTime != null && accumulatorStartTime.isAfter(filterEndTime))) {
                // Ignore this accumulator
                continue;
            }


            // This accumulator is in the right time range, now return only the sample kinds specified
            for (final TimelineChunkAccumulator chunkAccumulator : accumulator.getTimelines().values()) {
                // Extract the timeline for this chunk by copying it and reading encoded bytes
                accumulatorDeepCopyCount.inc();
                final TimelineChunkAccumulator chunkAccumulatorCopy = chunkAccumulator.deepCopy();
                final TimelineChunk timelineChunk = chunkAccumulatorCopy.extractTimelineChunkAndReset(accumulatorStartTime, accumulatorEndTime, accumulatorTimes);

                if (!sampleKindIds.contains(timelineChunk.getSampleKindId())) {
                    // We don't care about this sample kind
                    continue;
                }

                samplesByHostName.add(timelineChunk);
            }
        }
        inMemoryChunksReturnedCount.inc(samplesByHostName.size());
        Collections.sort(samplesByHostName, CHUNK_COMPARATOR);
        return samplesByHostName;
    }

    @VisibleForTesting
    void convertSamplesToScalarSamples(final Integer hostId, final String eventType, final Map<String, Object> inputSamples, final Map<Integer, ScalarSample> outputSamples)
    {
        if (inputSamples == null) {
            return;
        }
        final Integer eventCategoryId = timelineDAO.getOrAddEventCategory(eventType);

        for (final String attributeName : inputSamples.keySet()) {
            final Integer sampleKindId = timelineDAO.getOrAddSampleKind(hostId, eventCategoryId, attributeName);
            final Object sample = inputSamples.get(attributeName);

            outputSamples.put(sampleKindId, ScalarSample.fromObject(sample));
        }
    }

    public void replay(final String spoolDir)
    {
        replayCount.inc();
        log.info("Starting replay of files in {}", spoolDir);
        final Replayer replayer = new Replayer(spoolDir);
        final StartTimes startTimes = shutdownSaveMode == ShutdownSaveMode.SAVE_START_TIMES ? timelineDAO.getLastStartTimes() : null;

        try {
            // Read all files in the spool directory and delete them after process
            // NOTE: The replay process creates new replay files, so we can restart if
            // we crash before the accumulators are sent to the db.
            replayer.readAll(new Function<HostSamplesForTimestamp, Void>()
            {
                @Override
                public Void apply(@Nullable final HostSamplesForTimestamp hostSamples)
                {
                    if (hostSamples != null) {
                        replaySamplesFoundCount.inc();
                        boolean useSamples = true;
                        try {
                            final int hostId = hostSamples.getHostId();
                            final String category = hostSamples.getCategory();
                            final int categoryId = timelineDAO.getEventCategoryId(category);
                            // If startTimes is non-null and the samples come from before the first time for
                            // the given host and event category, ignore the samples
                            if (startTimes != null) {
                                final DateTime timestamp = hostSamples.getTimestamp();
                                final DateTime categoryStartTime = startTimes.getStartTimeForHostIdAndCategoryId(hostId, categoryId);
                                if (timestamp == null ||
                                        timestamp.isBefore(startTimes.getMinStartTime()) ||
                                        (categoryStartTime != null && timestamp.isBefore(categoryStartTime))) {
                                    replaySamplesOutsideTimeRangeCount.inc();
                                    useSamples = false;
                                }
                            }
                            if (useSamples) {
                                replaySamplesProcessedCount.inc();
                                processSamples(hostSamples);
                            }
                        }
                        catch (Exception e) {
                            log.warn("Got exception replaying sample, data potentially lost! {}", hostSamples.toString());
                        }
                    }

                    return null;
                }
            });
            if (shutdownSaveMode == ShutdownSaveMode.SAVE_START_TIMES) {
                timelineDAO.deleteLastStartTimes();
            }
            log.info("Replay completed");
        }
        catch (RuntimeException e) {
            // Catch the exception to make the collector start properly
            log.error("Ignoring error when replaying the data", e);
        }
    }

    @Managed
    public void forceCommit(final boolean shutdown)
    {
        if (shutdown) {
            shuttingDown.set(true);
        }
        forceCommitCallCount.inc();
        final boolean doingFastShutdown = shutdown && shutdownSaveMode == ShutdownSaveMode.SAVE_START_TIMES;
        fastShutdown.set(doingFastShutdown);
        if (doingFastShutdown) {
            startTimes = shutdownSaveMode == ShutdownSaveMode.SAVE_START_TIMES ? new StartTimes() : null;
            saveAccumulatorsOrStartTimes();
            timelineDAO.insertLastStartTimes(startTimes);
        }
        else {
            startTimes = null;
            saveAccumulatorsOrStartTimes();
        }
        if (shutdown) {
            backgroundWriter.initiateShutdown();
            while (!backgroundWriter.getShutdownFinished()) {
                try {
                    Thread.currentThread().sleep(100);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        // All the samples have been saved, or else the start times have been saved.  Discard the local buffer
        backingBuffer.discard();

        log.info(doingFastShutdown ? "Timeline start times committed" : "Timelines committed");
    }

    @VisibleForTesting
    public Collection<TimelineHostEventAccumulator> getAccumulators()
    {
        final List<TimelineHostEventAccumulator> inMemoryAccumulator = new ArrayList<TimelineHostEventAccumulator>();
        for (final Map<Integer, TimelineHostEventAccumulator> hostEventAccumulatorMap : accumulators.values()) {
            inMemoryAccumulator.addAll(hostEventAccumulatorMap.values());
        }

        return inMemoryAccumulator;
    }

    @VisibleForTesting
    public FileBackedBuffer getBackingBuffer()
    {
        return backingBuffer;
    }

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventsDiscarded()
    {
        return eventsDiscarded.get();
    }

    public long getHostEventAccumulatorCount()
    {
        return accumulators.size();
    }
}
