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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
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
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.TimelineHostEventAccumulator;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class TimelineEventHandler implements EventHandler
{
    private static final Logger log = LoggerFactory.getLogger(TimelineEventHandler.class);

    private final AtomicLong eventsDiscarded = new AtomicLong(0L);
    // A TimelineHostEventAccumulator records attributes for a specific host and event type.
    // This cache maps hostId -> eventType -> accumulator
    private final LoadingCache<Integer, Map<String, TimelineHostEventAccumulator>> accumulators;
    private final Object accumulatorsMonitor = new Object();

    private final CollectorConfig config;
    private final ShutdownSaveMode shutdownSaveMode;
    private final TimelineDAO timelineDAO;
    private final FileBackedBuffer backingBuffer;
    private final AtomicBoolean fastShutdown;
    private final AtomicReference<StartTimes> startTimesReference = new AtomicReference<StartTimes>();

    @Inject
    public TimelineEventHandler(final CollectorConfig config, final TimelineDAO timelineDAO, final FileBackedBuffer fileBackedBuffer) throws IOException
    {
        this.config = config;
        this.shutdownSaveMode = ShutdownSaveMode.fromString(config.getShutdownSaveMode());
        this.fastShutdown = new AtomicBoolean(false);
        this.timelineDAO = timelineDAO;
        this.backingBuffer = fileBackedBuffer;

        accumulators = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(config.getMaxHosts())
            .removalListener(new RemovalListener<Integer, Map<String, TimelineHostEventAccumulator>>()
            {
                @Override
                public void onRemoval(final RemovalNotification<Integer, Map<String, TimelineHostEventAccumulator>> removedObjectNotification)
                {
                    // Save all accumulators for this host
                    final Map<String, TimelineHostEventAccumulator> accumulators = removedObjectNotification.getValue();
                    if (accumulators == null) {
                        // TODO Shouldn't happen without weak keys/values?
                        log.error("Accumulator already GCed - data lost!");
                    }
                    else {
                        for (final String eventType : accumulators.keySet()) {
                            final Integer hostId = removedObjectNotification.getKey();
                            final TimelineHostEventAccumulator accumulator = accumulators.get(eventType);
                            if (fastShutdown.get()) {
                                log.debug("Saving Timeline start time for hostId [{}] and category [{}]", hostId, eventType);
                                final int categoryId = timelineDAO.getEventCategoryId(eventType);
                                startTimesReference.get().addTime(hostId, categoryId, accumulator.getStartTime());
                            }
                            else {
                                log.debug("Saving Timeline for hostId [{}] and category [{}]", hostId, eventType);
                                accumulator.extractAndSaveTimelineChunks();
                            }
                        }
                    }
                }
            })
            .build(new CacheLoader<Integer, Map<String, TimelineHostEventAccumulator>>()
            {
                @Override
                public Map<String, TimelineHostEventAccumulator> load(final Integer hostId) throws Exception
                {
                    return new HashMap<String, TimelineHostEventAccumulator>();
                }
            });

        Executors.newSingleThreadScheduledExecutor("TimelinesCommiter").scheduleWithFixedDelay(new Runnable()
        {
            @Override
            public void run()
            {
                // Ideally we would use the CachBuilder and do:
                //  .expireAfterWrite(config.getTimelineLength().getMillis(), TimeUnit.MILLISECONDS)
                // Unfortunately, this is won't work as eviction only occurs at access time.
                forceCommit(false);
            }
        }, config.getTimelineLength().getMillis(), config.getTimelineLength().getMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void handle(final Event event)
    {
        try {
            // Lookup the host id
            final String hostName = EventsUtils.getHostNameFromEvent(event);
            final Integer hostId = timelineDAO.getOrAddHost(hostName);

            // Extract and parse samples
            final Map<String, Object> samples = EventsUtils.getSamplesFromEvent(event);
            final Map<Integer, ScalarSample> scalarSamples = new LinkedHashMap<Integer, ScalarSample>();
            convertSamplesToScalarSamples(hostId, event.getEventType(), samples, scalarSamples);

            if (scalarSamples.isEmpty()) {
                log.warn("Invalid event: " + event);
                eventsDiscarded.getAndIncrement();
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

    private void processSamples(final HostSamplesForTimestamp hostSamples) throws ExecutionException, IOException
    {
        final int hostId = hostSamples.getHostId();
        final String category = hostSamples.getCategory();
        final int categoryId = timelineDAO.getEventCategoryId(category);

        Map<String, TimelineHostEventAccumulator> hostAccumulators = accumulators.get(hostId);
        TimelineHostEventAccumulator accumulator = hostAccumulators.get(category);

        if (accumulator == null) {
            // TODO: It seems to me that this synchronization either doesn't work or isn't
            // necessary, because there is no synchronization on getting accumulators.
            // But I think we'll need a different data structure anyway to support
            synchronized (accumulatorsMonitor) {
                hostAccumulators = accumulators.get(hostId);
                accumulator = hostAccumulators.get(category);
                if (accumulator == null) {
                    accumulator = new TimelineHostEventAccumulator(timelineDAO, hostId, categoryId, config.getTimelinesVerboseStats());
                    hostAccumulators.put(category, accumulator);
                    log.debug("Created new Timeline for hostId [{}] and category [{}]", hostId, category);
                }
            }
        }

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
        // Check first if there is an in-memory accumulator for this host
        final Map<String, TimelineHostEventAccumulator> hostAccumulators = accumulators.getIfPresent(hostId);
        if (hostAccumulators == null) {
            return ImmutableList.of();
        }

        // Now, filter each accumulator for this host
        final List<TimelineChunk> samplesByHostName = new ArrayList<TimelineChunk>();
        for (final TimelineHostEventAccumulator accumulator : hostAccumulators.values()) {
            final List<DateTime> accumulatorTimes = accumulator.getTimes();
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
                final TimelineChunkAccumulator chunkAccumulatorCopy = chunkAccumulator.deepCopy();
                final TimelineChunk timelineChunk = chunkAccumulatorCopy.extractTimelineChunkAndReset(accumulatorStartTime, accumulatorEndTime, accumulatorTimes);

                if (!sampleKindIds.contains(timelineChunk.getSampleKindId())) {
                    // We don't care about this sample kind
                    continue;
                }

                samplesByHostName.add(timelineChunk);
            }
        }

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
                                    useSamples = false;
                                }
                            }
                            if (useSamples) {
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

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventsDiscarded()
    {
        return eventsDiscarded.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getInMemoryTimelines()
    {
        return accumulators.size();
    }

    @Managed
    public void forceCommit(final boolean shutdown)
    {
        final boolean doingFastShutdown = shutdown && shutdownSaveMode == ShutdownSaveMode.SAVE_START_TIMES;
        fastShutdown.set(doingFastShutdown);
        StartTimes startTimes = null;
        if (doingFastShutdown) {
            startTimes = shutdownSaveMode == ShutdownSaveMode.SAVE_START_TIMES ? new StartTimes() : null;
            startTimesReference.set(startTimes);
            accumulators.invalidateAll();
            timelineDAO.insertLastStartTimes(startTimes);
        }
        else {
            startTimesReference.set(null);
            accumulators.invalidateAll();
        }
        // All the samples have been saved, or else the start times have been saved.  Discard the local buffer
        backingBuffer.discard();

        log.info(doingFastShutdown ? "Timeline start times committed" : "Timelines committed");
    }

    @MonitorableManaged(description = "Returns the number of times a host accumulator lookup methods have returned a cached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getAccumulatorsHitCount()
    {
        return accumulators.stats().hitCount();
    }

    @MonitorableManaged(description = "Returns the ratio of host accumulator requests which were hits", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getAccumulatorsHitRate()
    {
        return accumulators.stats().hitRate();
    }

    @MonitorableManaged(description = "Returns the number of times a new host accumulator was created", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getAccumulatorsMissCount()
    {
        return accumulators.stats().missCount();
    }

    @MonitorableManaged(description = "Returns the ratio of requests resulting in creating a new host accumulator", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getAccumulatorsMissRate()
    {
        return accumulators.stats().missRate();
    }

    @MonitorableManaged(description = "Returns the number of times a new host accumulator was successfully created", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getAccumulatorsLoadSuccessCount()
    {
        return accumulators.stats().loadSuccessCount();
    }

    @MonitorableManaged(description = "Returns the number of times an exception was thrown while creating a new host accumulator", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getAccumulatorsLoadExceptionCount()
    {
        return accumulators.stats().loadExceptionCount();
    }

    @MonitorableManaged(description = "Returns the ratio of host accumulator creation attempts which threw exceptions", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getAccumulatorsLoadExceptionRate()
    {
        return accumulators.stats().loadExceptionRate();
    }

    @MonitorableManaged(description = "Returns the total number of nanoseconds spent creating new host accumulators", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getAccumulatorsTotalLoadTime()
    {
        return accumulators.stats().totalLoadTime();
    }

    @MonitorableManaged(description = "Returns the average time spent creating new host accumulators", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getAccumulatorsAverageLoadPenalty()
    {
        return accumulators.stats().averageLoadPenalty();
    }

    @MonitorableManaged(description = "Returns the number of times a host accumulator was stored in the database", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getAccumulatorsEvictionCount()
    {
        return accumulators.stats().evictionCount();
    }

    @VisibleForTesting
    public Collection<TimelineHostEventAccumulator> getAccumulators()
    {
        final List<TimelineHostEventAccumulator> inMemoryAccumulator = new ArrayList<TimelineHostEventAccumulator>();
        for (final Map<String, TimelineHostEventAccumulator> hostEventAccumulatorMap : accumulators.asMap().values()) {
            inMemoryAccumulator.addAll(hostEventAccumulatorMap.values());
        }

        return inMemoryAccumulator;
    }

    @VisibleForTesting
    public FileBackedBuffer getBackingBuffer()
    {
        return backingBuffer;
    }
}
