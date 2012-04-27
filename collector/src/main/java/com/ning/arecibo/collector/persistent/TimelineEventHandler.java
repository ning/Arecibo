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
import com.mogwee.executors.Executors;
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
import com.ning.arecibo.util.timeline.TimelineCoder;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.persistent.FileBackedBuffer;
import com.ning.arecibo.util.timeline.persistent.Replayer;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TimelineEventHandler implements EventHandler
{
    private static final Logger log = LoggerFactory.getLogger(TimelineEventHandler.class);
    private final ScheduledExecutorService purgeThread = Executors.newSingleThreadScheduledExecutor("TimelineEventPurger");
    private final ExecutorService loadGeneratorThread = Executors.newSingleThreadExecutor("LoadGenerator");
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
    private final Map<Integer, HostAccumulatorsAndUpdateDate> accumulators = new ConcurrentHashMap<Integer, HostAccumulatorsAndUpdateDate>();

    private final CollectorConfig config;
    private final TimelineDAO timelineDAO;
    private final BackgroundDBChunkWriter backgroundWriter;
    private final FileBackedBuffer backingBuffer;

    private final ShutdownSaveMode shutdownSaveMode;
    private final AtomicBoolean shuttingDown = new AtomicBoolean();

    private final AtomicLong eventsDiscarded = new AtomicLong(0L);
    private final AtomicLong eventsReceivedAfterShuttingDown = new AtomicLong();
    private final AtomicLong handledEventCount = new AtomicLong();
    private final AtomicLong addedHostEventAccumulatorMapCount = new AtomicLong();
    private final AtomicLong addedHostEventAccumulatorCount = new AtomicLong();
    private final AtomicLong getInMemoryChunksCallCount = new AtomicLong();
    private final AtomicLong accumulatorDeepCopyCount = new AtomicLong();
    private final AtomicLong inMemoryChunksReturnedCount = new AtomicLong();
    private final AtomicLong replayCount = new AtomicLong();
    private final AtomicLong replaySamplesFoundCount = new AtomicLong();
    private final AtomicLong replaySamplesOutsideTimeRangeCount = new AtomicLong();
    private final AtomicLong replaySamplesProcessedCount = new AtomicLong();
    private final AtomicLong forceCommitCallCount = new AtomicLong();


    private EventReplayingLoadGenerator loadGenerator = null;

    @Inject
    public TimelineEventHandler(final CollectorConfig config, final TimelineDAO timelineDAO, final BackgroundDBChunkWriter backgroundWriter, final FileBackedBuffer fileBackedBuffer)
    {
        this.config = config;
        this.timelineDAO = timelineDAO;
        this.backgroundWriter = backgroundWriter;
        this.backingBuffer = fileBackedBuffer;
        this.shutdownSaveMode = ShutdownSaveMode.fromString(config.getShutdownSaveMode());
    }

    private void saveAccumulators()
    {
        for (Map.Entry<Integer, HostAccumulatorsAndUpdateDate> entry : accumulators.entrySet()) {
            final int hostId = entry.getKey();
            final Map<Integer, TimelineHostEventAccumulator> hostAccumulators = entry.getValue().getCategoryAccumulators();
            for (Map.Entry<Integer, TimelineHostEventAccumulator> accumulatorEntry : hostAccumulators.entrySet()) {
                final int categoryId = accumulatorEntry.getKey();
                final TimelineHostEventAccumulator accumulator = accumulatorEntry.getValue();
                log.debug("Saving Timeline for hostId [{}] and categoryId [{}]", hostId, categoryId);
                accumulator.extractAndQueueTimelineChunks();
            }
        }
    }

    private void saveStartTimes(final StartTimes startTimes)
    {
        for (Map.Entry<Integer, HostAccumulatorsAndUpdateDate> entry : accumulators.entrySet()) {
            final int hostId = entry.getKey();
            final Map<Integer, TimelineHostEventAccumulator> hostAccumulators = entry.getValue().getCategoryAccumulators();
            for (Map.Entry<Integer, TimelineHostEventAccumulator> accumulatorEntry : hostAccumulators.entrySet()) {
                final int categoryId = accumulatorEntry.getKey();
                final TimelineHostEventAccumulator accumulator = accumulatorEntry.getValue();
                log.debug("Saving Timeline start time for hostId [{}] and category [{}]", hostId, categoryId);
                startTimes.addTime(hostId, categoryId, accumulator.getStartTime());
            }
        }
    }

    public synchronized void purgeOldHostsAndAccumulators(final DateTime purgeIfBeforeDate)
    {
        final List<Integer> oldHostIds = new ArrayList<Integer>();
        for (final Map.Entry<Integer, HostAccumulatorsAndUpdateDate> entry : accumulators.entrySet()) {
            final int hostId = entry.getKey();
            final HostAccumulatorsAndUpdateDate accumulatorsAndDate = entry.getValue();
            final DateTime lastUpdatedDate = accumulatorsAndDate.getLastUpdateDate();
            if (lastUpdatedDate.isBefore(purgeIfBeforeDate)) {
                oldHostIds.add(hostId);
                for (TimelineHostEventAccumulator categoryAccumulator : accumulatorsAndDate.getCategoryAccumulators().values()) {
                    categoryAccumulator.extractAndQueueTimelineChunks();
                }
            }
            else {
                final List<Integer> categoryIdsToPurge = new ArrayList<Integer>();
                final Map<Integer, TimelineHostEventAccumulator> categoryMap = accumulatorsAndDate.getCategoryAccumulators();
                for (final Map.Entry<Integer, TimelineHostEventAccumulator> eventEntry : categoryMap.entrySet()) {
                    final int categoryId = eventEntry.getKey();
                    final TimelineHostEventAccumulator categoryAccumulator = eventEntry.getValue();
                    final DateTime latestTime = categoryAccumulator.getLatestSampleAddTime();
                    if (latestTime != null && latestTime.isBefore(purgeIfBeforeDate)) {
                        categoryAccumulator.extractAndQueueTimelineChunks();
                        categoryIdsToPurge.add(categoryId);
                    }
                }
                for (final int categoryId : categoryIdsToPurge) {
                    categoryMap.remove(categoryId);
                }
            }
        }
        for (final int hostIdToPurge : oldHostIds) {
            accumulators.remove(hostIdToPurge);
        }
    }

    @Override
    public void handle(final Event event)
    {
        if (shuttingDown.get()) {
            eventsReceivedAfterShuttingDown.incrementAndGet();
            return;
        }
        try {
            handledEventCount.incrementAndGet();
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

    public TimelineHostEventAccumulator getOrAddHostEventAccumulator(final int hostId, final int categoryId, final DateTime firstSampleTime)
    {
        return this.getOrAddHostEventAccumulator(hostId, categoryId, firstSampleTime, (int)config.getTimelineLength().getMillis());
    }

    public synchronized TimelineHostEventAccumulator getOrAddHostEventAccumulator(final int hostId, final int categoryId, final DateTime firstSampleTime, final int timelineLengthMillis)
    {
        HostAccumulatorsAndUpdateDate hostAccumulatorsAndUpdateDate = accumulators.get(hostId);
        if (hostAccumulatorsAndUpdateDate == null) {
            addedHostEventAccumulatorMapCount.incrementAndGet();
            hostAccumulatorsAndUpdateDate = new HostAccumulatorsAndUpdateDate(new HashMap<Integer, TimelineHostEventAccumulator>(), new DateTime());
            accumulators.put(hostId, hostAccumulatorsAndUpdateDate);
        }
        hostAccumulatorsAndUpdateDate.markUpdated();
        final Map<Integer, TimelineHostEventAccumulator> hostCategoryAccumulators = hostAccumulatorsAndUpdateDate.getCategoryAccumulators();
        TimelineHostEventAccumulator accumulator = hostCategoryAccumulators.get(categoryId);
        if (accumulator == null) {
            addedHostEventAccumulatorCount.incrementAndGet();
            accumulator = new TimelineHostEventAccumulator(timelineDAO, backgroundWriter, hostId, categoryId, firstSampleTime, timelineLengthMillis);
            hostCategoryAccumulators.put(categoryId, accumulator);
            log.debug("Created new Timeline for hostId [{}] and category [{}]", hostId, categoryId);
        }
        return accumulator;
    }

    @VisibleForTesting
    public void processSamples(final HostSamplesForTimestamp hostSamples) throws ExecutionException, IOException
    {
        final int hostId = hostSamples.getHostId();
        final String category = hostSamples.getCategory();
        final int categoryId = timelineDAO.getEventCategoryId(category);
        final DateTime timestamp = hostSamples.getTimestamp();
        final TimelineHostEventAccumulator accumulator = getOrAddHostEventAccumulator(hostId, categoryId, timestamp);
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

    public synchronized Collection<? extends TimelineChunk> getInMemoryTimelineChunks(final Integer hostId, final List<Integer> sampleKindIds, @Nullable final DateTime filterStartTime, @Nullable final DateTime filterEndTime) throws IOException, ExecutionException
    {
        getInMemoryChunksCallCount.incrementAndGet();
        // Check first if there is an in-memory accumulator for this host
        final HostAccumulatorsAndUpdateDate hostAccumulatorsAndDate = accumulators.get(hostId);
        if (hostAccumulatorsAndDate == null) {
            return ImmutableList.of();
        }

        // Now, filter each accumulator for this host
        final List<TimelineChunk> samplesByHostName = new ArrayList<TimelineChunk>();
        for (final TimelineHostEventAccumulator accumulator : hostAccumulatorsAndDate.getCategoryAccumulators().values()) {
            for (TimelineChunk chunk : accumulator.getPendingTimelineChunks()) {
                if ((filterStartTime != null && chunk.getEndTime().isBefore(filterStartTime)) ||
                    (filterEndTime != null && chunk.getStartTime().isAfter(filterEndTime)) ||
                    !sampleKindIds.contains(chunk.getSampleKindId())) {
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
            if ((filterStartTime != null && accumulatorEndTime.isBefore(filterStartTime)) || (filterEndTime != null && accumulatorStartTime.isAfter(filterEndTime))) {
                // Ignore this accumulator
                continue;
            }

            // This accumulator is in the right time range, now return only the sample kinds specified
            final byte[] timeBytes = TimelineCoder.compressDateTimes(accumulatorTimes);
            for (final TimelineChunkAccumulator chunkAccumulator : accumulator.getTimelines().values()) {
                if (sampleKindIds.contains(chunkAccumulator.getSampleKindId())) {
                    // Extract the timeline for this chunk by copying it and reading encoded bytes
                    accumulatorDeepCopyCount.incrementAndGet();
                    final TimelineChunkAccumulator chunkAccumulatorCopy = chunkAccumulator.deepCopy();
                    final TimelineChunk timelineChunk = chunkAccumulatorCopy.extractTimelineChunkAndReset(accumulatorStartTime, accumulatorEndTime, timeBytes);
                    samplesByHostName.add(timelineChunk);
                }
            }
        }
        inMemoryChunksReturnedCount.addAndGet(samplesByHostName.size());
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
        replayCount.incrementAndGet();
        log.info("Starting replay of files in {}", spoolDir);
        final Replayer replayer = new Replayer(spoolDir);
        StartTimes lastStartTimes = null;
        if (shutdownSaveMode == ShutdownSaveMode.SAVE_START_TIMES) {
            lastStartTimes = timelineDAO.getLastStartTimes();
            if (lastStartTimes == null) {
                log.info("Did not find startTimes");
            }
            else {
                log.info("Retrieved startTimes from the db");
            }
        }
        final StartTimes startTimes = lastStartTimes;
        final DateTime minStartTime = lastStartTimes == null ? null : startTimes.getMinStartTime();
        final long found = replaySamplesFoundCount.get();
        final long outsideTimeRange = replaySamplesOutsideTimeRangeCount.get();
        final long processed = replaySamplesProcessedCount.get();

        try {
            // Read all files in the spool directory and delete them after process, if
            // startTimes  is null.
            replayer.readAll(startTimes == null, minStartTime, new Function<HostSamplesForTimestamp, Void>()
            {
                @Override
                public Void apply(@Nullable final HostSamplesForTimestamp hostSamples)
                {
                    if (hostSamples != null) {
                        replaySamplesFoundCount.incrementAndGet();
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
                                    replaySamplesOutsideTimeRangeCount.incrementAndGet();
                                    useSamples = false;
                                }
                            }
                            if (useSamples) {
                                replaySamplesProcessedCount.incrementAndGet();
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
                log.info("Deleted old startTimes");
            }
            log.info(String.format("Replay completed samples read %d, samples outside time range %d, samples used %d",
                    replaySamplesFoundCount.get() - found, replaySamplesOutsideTimeRangeCount.get() - outsideTimeRange, replaySamplesProcessedCount.get() - processed));
        }
        catch (RuntimeException e) {
            // Catch the exception to make the collector start properly
            log.error("Ignoring error when replaying the data", e);
        }
    }

    @Managed
    public void forceCommit()
    {
        forceCommitCallCount.incrementAndGet();
        saveAccumulators();
        backingBuffer.discard();
        log.info("Timelines committed");
    }

    @Managed
    public void commitAndShutdown()
    {
        shuttingDown.set(true);
        final boolean doingFastShutdown = shutdownSaveMode == ShutdownSaveMode.SAVE_START_TIMES;
        if (doingFastShutdown) {
            final StartTimes startTimes = new StartTimes();
            saveStartTimes(startTimes);
            timelineDAO.insertLastStartTimes(startTimes);
            log.info("During shutdown, saved timeline start times in the db");
        }
        else {
            saveAccumulators();
            log.info("During shutdown, saved timeline accumulators");
        }
        performShutdown();
        backingBuffer.discard();
    }

    private void performShutdown()
    {
        if (config.getRunLoadGenerator()) {
            loadGenerator.initiateShutdown();
            loadGeneratorThread.shutdown();
        }
        backgroundWriter.initiateShutdown();
        while (!backgroundWriter.getShutdownFinished()) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        purgeThread.shutdown();
    }

    private synchronized void purgeFilesAndAccumulators()
    {
        this.purgeFilesAndAccumulators(new DateTime().minus(config.getTimelineLength().getMillis()), new DateTime().minus(2 * config.getTimelineLength().getMillis()));
    }

    // TODO: We have a bad interaction between startTimes and purging: If the system is down
    // for two hours, we may not want it to purge everything.  Figure out what to do about this.
    private synchronized void purgeFilesAndAccumulators(final DateTime purgeAccumulatorsIfBefore, final DateTime purgeFilesIfBefore)
    {
        purgeOldHostsAndAccumulators(purgeAccumulatorsIfBefore);
        final Replayer replayer = new Replayer(config.getSpoolDir());
        replayer.purgeOldFiles(purgeFilesIfBefore);
    }

    public void startHandlerThreads() {
        purgeThread.scheduleWithFixedDelay(new Runnable()
        {
            @Override
            public void run()
            {
                purgeFilesAndAccumulators();
            }
        },
        config.getTimelineLength().getMillis(),
        config.getTimelineLength().getMillis(),
        TimeUnit.MILLISECONDS);

        if (config.getRunLoadGenerator()) {
            loadGenerator = new EventReplayingLoadGenerator(this, timelineDAO);
            final EventReplayingLoadGenerator myLoadGenerator = loadGenerator;
            loadGeneratorThread.execute(new Runnable() {

                @Override
                public void run() {
                    myLoadGenerator.generateEventStream();
                }
            });
        }
    }

    // We use the lastUpdateDate to purge hosts and their accumulators from the map
    private static class HostAccumulatorsAndUpdateDate
    {
        private final Map<Integer, TimelineHostEventAccumulator> categoryAccumulators;
        private DateTime lastUpdateDate;

        public HostAccumulatorsAndUpdateDate(Map<Integer, TimelineHostEventAccumulator> categoryAccumulators, DateTime lastUpdateDate)
        {
            super();
            this.categoryAccumulators = categoryAccumulators;
            this.lastUpdateDate = lastUpdateDate;
        }

        public Map<Integer, TimelineHostEventAccumulator> getCategoryAccumulators()
        {
            return categoryAccumulators;
        }

        public DateTime getLastUpdateDate()
        {
            return lastUpdateDate;
        }

        public void markUpdated()
        {
            lastUpdateDate = new DateTime();
        }
    }

    @VisibleForTesting
    public Collection<TimelineHostEventAccumulator> getAccumulators()
    {
        final List<TimelineHostEventAccumulator> inMemoryAccumulator = new ArrayList<TimelineHostEventAccumulator>();
        for (final HostAccumulatorsAndUpdateDate hostEventAccumulatorMap : accumulators.values()) {
            inMemoryAccumulator.addAll(hostEventAccumulatorMap.getCategoryAccumulators().values());
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

    @Managed
    public long getHostEventAccumulatorCount()
    {
        return accumulators.size();
    }

    @Managed
    public long getEventsReceivedAfterShuttingDown()
    {
        return eventsReceivedAfterShuttingDown.get();
    }

    @Managed
    public long getHandledEventCount()
    {
        return handledEventCount.get();
    }

    @Managed
    public long getAddedHostEventAccumulatorMapCount()
    {
        return addedHostEventAccumulatorMapCount.get();
    }

    @Managed
    public long getAddedHostEventAccumulatorCount()
    {
        return addedHostEventAccumulatorCount.get();
    }

    @Managed
    public long getGetInMemoryChunksCallCount()
    {
        return getInMemoryChunksCallCount.get();
    }

    @Managed
    public long getAccumulatorDeepCopyCount()
    {
        return accumulatorDeepCopyCount.get();
    }

    @Managed
    public long getInMemoryChunksReturnedCount()
    {
        return inMemoryChunksReturnedCount.get();
    }

    @Managed
    public long getReplayCount()
    {
        return replayCount.get();
    }

    @Managed
    public long getReplaySamplesFoundCount()
    {
        return replaySamplesFoundCount.get();
    }

    @Managed
    public long getReplaySamplesOutsideTimeRangeCount()
    {
        return replaySamplesOutsideTimeRangeCount.get();
    }

    @Managed
    public long getReplaySamplesProcessedCount()
    {
        return replaySamplesProcessedCount.get();
    }

    @Managed
    public long getForceCommitCallCount()
    {
        return forceCommitCallCount.get();
    }
}

