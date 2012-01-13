package com.ning.arecibo.collector.process;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Inject;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.event.BatchedEvent;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.MonitoringEvent;
import com.ning.arecibo.event.receiver.EventProcessor;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.SampleOpcode;
import com.ning.arecibo.util.timeline.ScalarSample;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.TimelineHostEventAccumulator;
import com.ning.arecibo.util.timeline.TimelineRegistry;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CollectorEventProcessor implements EventProcessor
{
    private final static Logger log = Logger.getLogger(CollectorEventProcessor.class);

    private final Cache<Integer, TimelineHostEventAccumulator> accumulators;
    private final TimelineDAO timelineDAO;
    private TimelineRegistry timelineRegistry = null;

    private final AtomicLong eventsReceived = new AtomicLong(0L);
    private final AtomicLong eventsDiscarded = new AtomicLong(0L);

    @Inject
    public CollectorEventProcessor(final CollectorConfig config, final TimelineDAO timelineDAO)
    {
        this.timelineDAO = timelineDAO;

        accumulators = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .maximumSize(config.getMaxHosts())
            .expireAfterWrite(config.getTimelineLength().getMillis(), TimeUnit.MILLISECONDS)
            .removalListener(new RemovalListener<Integer, TimelineHostEventAccumulator>()
            {
                @Override
                public void onRemoval(RemovalNotification<Integer, TimelineHostEventAccumulator> removedObjectNotification)
                {
                    TimelineHostEventAccumulator accumulator = removedObjectNotification.getValue();
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
                public TimelineHostEventAccumulator load(Integer hostId) throws Exception
                {
                    log.info("Creating new Timeline for hostId: " + hostId);
                    return new TimelineHostEventAccumulator(timelineDAO, hostId);
                }
            });
    }

    public void processEvent(Event evt)
    {
        if (timelineRegistry == null) {
            timelineRegistry = new TimelineRegistry(timelineDAO);
        }

        try {
            final List<Event> events = new ArrayList<Event>();
            if (evt instanceof BatchedEvent) {
                events.addAll(((BatchedEvent) evt).getEvents());
            }
            else {
                events.add(evt);
            }
            // Update stats
            eventsReceived.getAndAdd(events.size());

            // Lookup the host id
            final int hostId = getHostIdFromEvent(evt);

            // Extract input samples
            final Map<Integer, ScalarSample> scalarSamples = new LinkedHashMap<Integer, ScalarSample>();
            for (final Event event : events) {
                if (event instanceof MapEvent) {
                    final Map<String, Object> samplesMap = ((MapEvent) evt).getMap();
                    convertSamplesToScalarSamples(samplesMap, scalarSamples);
                }
                else if (event instanceof MonitoringEvent) {
                    final Map<String, Object> samplesMap = ((MonitoringEvent) evt).getMap();
                    convertSamplesToScalarSamples(samplesMap, scalarSamples);
                }
                else {
                    log.warn("I don't understand event: " + event);
                    eventsDiscarded.getAndIncrement();
                }
            }

            // In case of batched events, use the timestmap of the first event
            final HostSamplesForTimestamp hostSamples = new HostSamplesForTimestamp(hostId, evt.getEventType(), new DateTime(events.get(0).getTimestamp(), DateTimeZone.UTC), scalarSamples);
            final TimelineHostEventAccumulator accumulator = accumulators.get(hostId);
            accumulator.addHostSamples(hostSamples);
        }
        catch (RuntimeException ruEx) {
            log.warn(ruEx);
        }
        catch (ExecutionException e) {
            log.warn(e);
        }
    }

    private int getHostIdFromEvent(Event evt)
    {
        String hostUUID = evt.getSourceUUID().toString();
        if (evt instanceof MonitoringEvent) {
            hostUUID = ((MonitoringEvent) evt).getHostName();
        }
        return timelineRegistry.getOrAddHost(hostUUID);
    }

    private void convertSamplesToScalarSamples(Map<String, Object> inputSamples, Map<Integer, ScalarSample> outputSamples)
    {
        if (inputSamples == null) {
            return;
        }

        for (final String sampleKind : inputSamples.keySet()) {
            final int sampleKindId = timelineRegistry.getOrAddSampleKind(sampleKind);
            final Object sample = inputSamples.get(sampleKind);

            if (sample == null) {
                outputSamples.put(sampleKindId, new ScalarSample(SampleOpcode.NULL, sample));
            }
            else if (sample instanceof Byte) {
                outputSamples.put(sampleKindId, new ScalarSample(SampleOpcode.BYTE, sample));
            }
            else if (sample instanceof Short) {
                outputSamples.put(sampleKindId, new ScalarSample(SampleOpcode.SHORT, sample));
            }
            else if (sample instanceof Integer) {
                outputSamples.put(sampleKindId, new ScalarSample(SampleOpcode.INT, sample));
            }
            else if (sample instanceof Long) {
                outputSamples.put(sampleKindId, new ScalarSample(SampleOpcode.LONG, sample));
            }
            else if (sample instanceof Float) {
                outputSamples.put(sampleKindId, new ScalarSample(SampleOpcode.FLOAT, sample));
            }
            else if (sample instanceof Double) {
                outputSamples.put(sampleKindId, new ScalarSample(SampleOpcode.DOUBLE, sample));
            }
            else {
                outputSamples.put(sampleKindId, new ScalarSample(SampleOpcode.STRING, sample.toString()));
            }
        }
    }

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventsReceived()
    {
        return eventsReceived.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventsDiscarded()
    {
        return eventsDiscarded.get();
    }
}
