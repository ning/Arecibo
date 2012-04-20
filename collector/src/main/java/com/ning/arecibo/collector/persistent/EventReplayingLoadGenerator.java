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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.weakref.jmx.Managed;

import com.google.common.base.Function;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.persistent.Replayer;

public class EventReplayingLoadGenerator {
    private static final Logger log = Logger.getLogger(EventReplayingLoadGenerator.class);
    private static final DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();
    private static final String REPLAY_FILE_DIRECTORY = System.getProperty("arecibo.collector.timeline.replayFileDirectory");
    private static final int REPLAY_REPEAT_COUNT = Integer.parseInt(System.getProperty("arecibo.collector.timeline.replayRepeatCount"));
    private static final int EVENTS_PER_SECOND = Integer.parseInt(System.getProperty("arecibo.collector.timeline.eventsPerSecond", "100"));
    private static final int SIMULATED_HOSTS_PER_REAL_HOST = Integer.parseInt(System.getProperty("arecibo.collector.timeline.simulatedHostsPerRealHost", "20"));

    private final TimelineEventHandler eventHandler;
    private final TimelineDAO timelineDAO;
    private final Map<Integer, DateTime> latestHostTimes = new HashMap<Integer, DateTime>();
    private final AtomicBoolean shuttingDown = new AtomicBoolean();
    private final AtomicLong eventsSent = new AtomicLong();
    private final AtomicLong lateEvents = new AtomicLong();
    private final AtomicLong samplesAdded = new AtomicLong();

    private Map<Integer, List<Integer>> simulatedHostIdsForRealHostId = new HashMap<Integer, List<Integer>>();
    private long nextFlushTime = 0;
    private int eventsPerSecondCount = 0;
    // This is a timestamp read from the replayed event
    private DateTime firstReplayEventTimestamp;
    // This is the corresponding real time timestamp
    private DateTime replayIterationStartTime;
    // This is the amount to add to a read timestamp to get
    // the real time timestamp
    private long millisecondsTimeShift;

    public EventReplayingLoadGenerator(TimelineEventHandler eventHandler, TimelineDAO timelineDAO)
    {
        this.eventHandler = eventHandler;
        this.timelineDAO = timelineDAO;
    }

    private void resetSecondCounter()
    {
        nextFlushTime = System.currentTimeMillis() + 1000;
        eventsPerSecondCount = 0;
    }

    public void generateEventStream()
    {
        final Replayer replayer = new Replayer(REPLAY_FILE_DIRECTORY);
        resetSecondCounter();
        for (int i=0; i<REPLAY_REPEAT_COUNT; i++) {
            replayIterationStartTime = new DateTime();
            final Collection<File> files = FileUtils.listFiles(new File(REPLAY_FILE_DIRECTORY), new String[]{"bin"}, false);
            firstReplayEventTimestamp = null;
            for (final File file : Replayer.FILE_ORDERING.sortedCopy(files)) {
                try {
                    log.info("About to read file %s", file.getAbsolutePath());
                    replayer.read(file, new Function<HostSamplesForTimestamp, Void>() {

                        @Override
                        public Void apply(HostSamplesForTimestamp hostSamples) {
                            processSamples(hostSamples);
                            return null;
                        }
                    });

                }
                catch (IOException e) {
                    log.warn(e, "Exception replaying file: %s", file.getAbsolutePath());
                }
                if (shuttingDown.get()) {
                    log.info("Exiting generateEventStream() because shutdown is true");
                }
            }
            latestHostTimes.clear();
        }
    }

    public void initiateShutdown()
    {
        shuttingDown.set(true);
    }

    private DateTime getAdjustedSampleTime(final DateTime timestamp)
    {
        if (firstReplayEventTimestamp == null) {
            firstReplayEventTimestamp = timestamp;
            millisecondsTimeShift = replayIterationStartTime.getMillis() - timestamp.getMillis();
        }
        final int addend = (int)(timestamp.getMillis() - firstReplayEventTimestamp.getMillis());
        //log.info("In processSamples(), timestamp %s, replayIterationStartTime %s, firstReplayEventTimestamp %s, addend %d",
        //        timestamp.toString(), replayIterationStartTime.toString(), firstReplayEventTimestamp.toString(), addend);
        return replayIterationStartTime.plusMillis(addend);
    }

    private void processSamples(final HostSamplesForTimestamp hostSamples)
    {
        final DateTime timestamp = hostSamples.getTimestamp();
        final DateTime adjustedTime = getAdjustedSampleTime(timestamp);
        final int hostId = hostSamples.getHostId();
        List<Integer> simulatedHostIds = simulatedHostIdsForRealHostId.get(hostId);
        if (simulatedHostIds == null) {
            simulatedHostIds = createSimulatedHostIds(hostId, SIMULATED_HOSTS_PER_REAL_HOST);
            simulatedHostIdsForRealHostId.put(hostId, simulatedHostIds);
        }
        DateTime latestHostTime = latestHostTimes.get(hostId);
        if (latestHostTime == null) {
            latestHostTime = adjustedTime;
            latestHostTimes.put(hostId, latestHostTime);
        }
        // log.info("In processSamples(), timestamp %s, adjustedTime %s, latestTime %s", timestamp.toString(), adjustedTime.toString(), latestHostTime.toString());
        if (adjustedTime.isBefore(latestHostTime)) {
            lateEvents.incrementAndGet();
        }
        else {
            timelineDAO.getOrAddEventCategory(hostSamples.getCategory());
            for (int simulatedHostId : simulatedHostIds) {
                final HostSamplesForTimestamp newSamples = new HostSamplesForTimestamp(simulatedHostId, hostSamples.getCategory(), adjustedTime, hostSamples.getSamples());
                try {
                    samplesAdded.addAndGet(hostSamples.getSamples().size());
                    eventHandler.processSamples(newSamples);
                    eventsPerSecondCount++;
                    sendAndLog();
                }
                catch (Exception e) {
                    log.error(e, "Exception processing samples for hostid %d", simulatedHostId);
                }
            }
            latestHostTimes.put(hostId, adjustedTime);
        }
    }

    private void sendAndLog()
    {
        final long sentCount = eventsSent.incrementAndGet();
        if (sentCount % 1000 == 0) {
            log.info("%d events sent, %d late and ignored, %d samples added", sentCount, lateEvents.get(), samplesAdded.get());
        }
        if (eventsPerSecondCount >= EVENTS_PER_SECOND) {
            final long elapsed = System.currentTimeMillis() - nextFlushTime;
            if (elapsed < 1000) {
                try {
                    Thread.sleep(1000 - elapsed);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            resetSecondCounter();
        }
    }

    private List<Integer> createSimulatedHostIds(final int hostId, final int simulatedCount)
    {
        final List<Integer> simulatedHostIds = new ArrayList<Integer>();
        for (int i=1; i<=simulatedCount; i++) {
            simulatedHostIds.add(timelineDAO.getOrAddHost(String.format("simhost%d_%d", hostId, i)));
        }
        return simulatedHostIds;
    }

    @Managed
    public long getEventsSent()
    {
        return eventsSent.get();
    }

    @Managed
    public long getLateEvents()
    {
        return lateEvents.get();
    }

    @Managed
    public long getSamplesAdded()
    {
        return samplesAdded.get();
    }

}
