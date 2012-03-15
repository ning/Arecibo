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
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.MonitoringEvent;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.SampleOpcode;
import com.ning.arecibo.util.timeline.ScalarSample;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineChunkAccumulator;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.TimelineHostEventAccumulator;
import com.ning.arecibo.util.timeline.TimelineRegistry;
import com.ning.arecibo.util.timeline.TimelineTimes;
import com.ning.arecibo.util.timeline.persistent.Replayer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.weakref.jmx.Managed;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TimelineEventHandler implements EventHandler
{
    private static final Logger log = Logger.getLogger(TimelineEventHandler.class);

    private final AtomicLong eventsDiscarded = new AtomicLong(0L);
    private final LoadingCache<Integer, TimelineHostEventAccumulator> accumulators;
    private final TimelineDAO timelineDAO;

    private TimelineRegistry timelineRegistry = null;

    @Inject
    public TimelineEventHandler(final CollectorConfig config, final TimelineDAO timelineDAO)
    {
        this.timelineDAO = timelineDAO;

        accumulators = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(config.getMaxHosts())
            .removalListener(new RemovalListener<Integer, TimelineHostEventAccumulator>()
            {
                @Override
                public void onRemoval(final RemovalNotification<Integer, TimelineHostEventAccumulator> removedObjectNotification)
                {
                    final TimelineHostEventAccumulator accumulator = removedObjectNotification.getValue();
                    if (accumulator == null) {
                        // TODO How often will that happen?
                        log.error("Accumulator already GCed - data lost!");
                    }
                    else {
                        final Integer hostId = removedObjectNotification.getKey();
                        if (hostId == null) {
                            log.info("Saving Timeline");
                        }
                        else {
                            log.info("Saving Timeline for hostId: " + hostId);
                        }
                        accumulator.extractAndSaveTimelineChunks();
                    }
                }
            })
            .build(new CacheLoader<Integer, TimelineHostEventAccumulator>()
            {
                @Override
                public TimelineHostEventAccumulator load(final Integer hostId) throws Exception
                {
                    log.info("Creating new Timeline for hostId: " + hostId);
                    return new TimelineHostEventAccumulator(config.getSpoolDir(), timelineDAO, hostId);
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
                forceCommit();
            }
        }, config.getTimelineLength().getMillis(), config.getTimelineLength().getMillis(), TimeUnit.MILLISECONDS);

        replay(config.getSpoolDir());
    }

    @Override
    public void handle(final Event event)
    {
        if (timelineRegistry == null) {
            timelineRegistry = new TimelineRegistry(timelineDAO);
        }

        try {
            final Map<Integer, ScalarSample> scalarSamples = new LinkedHashMap<Integer, ScalarSample>();

            // Lookup the host id
            final int hostId = getHostIdFromEvent(event);

            // Extract samples
            if (event instanceof MapEvent) {
                final Map<String, Object> samplesMap = ((MapEvent) event).getMap();
                convertSamplesToScalarSamples(hostId, samplesMap, scalarSamples);
            }
            else if (event instanceof MonitoringEvent) {
                final Map<String, Object> samplesMap = ((MonitoringEvent) event).getMap();
                convertSamplesToScalarSamples(hostId, samplesMap, scalarSamples);
            }
            else {
                log.warn("I don't understand event: " + event);
                eventsDiscarded.getAndIncrement();
            }

            final HostSamplesForTimestamp hostSamples = new HostSamplesForTimestamp(hostId, event.getEventType(), new DateTime(event.getTimestamp(), DateTimeZone.UTC), scalarSamples);
            processSamples(hostSamples);
        }
        catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void processSamples(final HostSamplesForTimestamp hostSamples) throws ExecutionException
    {
        final TimelineHostEventAccumulator accumulator = accumulators.get(hostSamples.getHostId());
        accumulator.addHostSamples(hostSamples);
    }

    public Collection<? extends TimelineChunkAndTimes> getInMemoryTimelineChunkAndTimes(final String filterHostName, @Nullable final DateTime filterStartTime, @Nullable final DateTime filterEndTime) throws IOException, ExecutionException
    {
        final ImmutableList<String> sampleKinds = ImmutableList.copyOf(timelineRegistry.getSampleKindsForHost(filterHostName));
        return getInMemoryTimelineChunkAndTimes(filterHostName, sampleKinds, filterStartTime, filterEndTime);
    }

    public Collection<? extends TimelineChunkAndTimes> getInMemoryTimelineChunkAndTimes(final String filterHostName, final String filterSampleKind, @Nullable final DateTime filterStartTime, @Nullable final DateTime filterEndTime) throws IOException, ExecutionException
    {
        return getInMemoryTimelineChunkAndTimes(filterHostName, ImmutableList.of(filterSampleKind), filterStartTime, filterEndTime);
    }

    public Collection<? extends TimelineChunkAndTimes> getInMemoryTimelineChunkAndTimes(final String filterHostName, final List<String> filterSampleKinds, @Nullable final DateTime filterStartTime, @Nullable final DateTime filterEndTime) throws IOException, ExecutionException
    {
        if (timelineRegistry == null) {
            return ImmutableList.of();
        }

        // Check first if there is an in-memory accumulator for this host
        final Integer hostId = timelineRegistry.getHostId(filterHostName);
        if (hostId == null) {
            return ImmutableList.of();
        }
        final TimelineHostEventAccumulator hostEventAccumulator = accumulators.getIfPresent(hostId);
        if (hostEventAccumulator == null) {
            return ImmutableList.of();
        }

        // Yup, there is. Check now if the filters apply
        final List<DateTime> accumulatorTimes = hostEventAccumulator.getTimes();
        final DateTime accumulatorStartTime = hostEventAccumulator.getStartTime();
        final DateTime accumulatorEndTime = hostEventAccumulator.getEndTime();

        if ((filterStartTime != null && accumulatorEndTime.isBefore(filterStartTime)) || (filterStartTime != null && accumulatorStartTime.isAfter(filterEndTime))) {
            // Ignore this accumulator
            return ImmutableList.of();
        }

        // We have a timeline match, return the samples matching the sample kinds
        final List<TimelineChunkAndTimes> samplesByHostName = new ArrayList<TimelineChunkAndTimes>();
        for (final TimelineChunkAccumulator chunkAccumulator : hostEventAccumulator.getTimelines().values()) {
            // Extract the timeline for this chunk by copying it and reading encoded bytes
            final TimelineChunkAccumulator accumulator = chunkAccumulator.deepCopy();
            final TimelineChunk timelineChunk = accumulator.extractTimelineChunkAndReset(-1);
            // NOTE! Further filtering needs to be done in the processing function
            final TimelineTimes timelineTimes = new TimelineTimes(-1, hostId, accumulatorStartTime, accumulatorEndTime, accumulatorTimes);

            final String sampleKind = timelineRegistry.getSampleKindById(timelineChunk.getSampleKindId());
            if (!filterSampleKinds.contains(sampleKind)) {
                // We don't care about this sample kind
                continue;
            }

            samplesByHostName.add(new TimelineChunkAndTimes(filterHostName, sampleKind, timelineChunk, timelineTimes));
        }

        return samplesByHostName;
    }

    private int getHostIdFromEvent(final Event event)
    {
        String hostUUID = event.getSourceUUID().toString();
        if (event instanceof MonitoringEvent) {
            hostUUID = ((MonitoringEvent) event).getHostName();
        }
        return timelineRegistry.getOrAddHost(hostUUID);
    }

    private void convertSamplesToScalarSamples(final int hostId, final Map<String, Object> inputSamples, final Map<Integer, ScalarSample> outputSamples)
    {
        if (inputSamples == null) {
            return;
        }

        for (final String sampleKind : inputSamples.keySet()) {
            final int sampleKindId = timelineRegistry.getOrAddSampleKind(hostId, sampleKind);
            final Object sample = inputSamples.get(sampleKind);

            if (sample == null) {
                outputSamples.put(sampleKindId, new ScalarSample<Void>(SampleOpcode.NULL, null));
            }
            else if (sample instanceof Byte) {
                outputSamples.put(sampleKindId, new ScalarSample<Byte>(SampleOpcode.BYTE, (Byte) sample));
            }
            else if (sample instanceof Short) {
                outputSamples.put(sampleKindId, new ScalarSample<Short>(SampleOpcode.SHORT, (Short) sample));
            }
            else if (sample instanceof Integer) {
                outputSamples.put(sampleKindId, new ScalarSample<Integer>(SampleOpcode.INT, (Integer) sample));
            }
            else if (sample instanceof Long) {
                outputSamples.put(sampleKindId, new ScalarSample<Long>(SampleOpcode.LONG, (Long) sample));
            }
            else if (sample instanceof Float) {
                outputSamples.put(sampleKindId, new ScalarSample<Float>(SampleOpcode.FLOAT, (Float) sample));
            }
            else if (sample instanceof Double) {
                outputSamples.put(sampleKindId, new ScalarSample<Double>(SampleOpcode.DOUBLE, (Double) sample));
            }
            else {
                outputSamples.put(sampleKindId, new ScalarSample<String>(SampleOpcode.STRING, sample.toString()));
            }
        }
    }

    private void replay(final String spoolDir)
    {
        log.info("Starting replay of files in {}", spoolDir);
        final Replayer replayer = new Replayer(spoolDir);

        // Read all files in the spool directory and delete them after process
        replayer.readAll(new Function<HostSamplesForTimestamp, Void>()
        {
            @Override
            public Void apply(@Nullable final HostSamplesForTimestamp input)
            {
                if (input != null) {
                    try {
                        processSamples(input);
                    }
                    catch (ExecutionException e) {
                        log.warn("Got exception replaying sample, data potentially lost! {}", input.toString());
                    }
                }

                return null;
            }
        });

        log.info("Replay completed");
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
    public void forceCommit()
    {
        accumulators.invalidateAll();
    }

    @VisibleForTesting
    public Collection<TimelineHostEventAccumulator> getAccumulators()
    {
        return accumulators.asMap().values();
    }
}
