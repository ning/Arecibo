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
import org.weakref.jmx.Managed;

import com.google.common.base.Function;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.persistent.Replayer;
import com.ning.arecibo.util.timeline.persistent.TimelineDAO;

public class EventReplayingLoadGenerator {
    private static final Logger log = Logger.getLogger(EventReplayingLoadGenerator.class);
    private static final String REPLAY_FILE_DIRECTORY = System.getProperty("arecibo.collector.timelines.replayFileDirectory");
    private static final int REPLAY_REPEAT_COUNT = Integer.parseInt(System.getProperty("arecibo.collector.timelines.replayRepeatCount"));
    private static final int EVENTS_PER_SECOND = Integer.parseInt(System.getProperty("arecibo.collector.timelines.eventsPerSecond", "100"));
    private static final int SIMULATED_HOSTS_PER_REAL_HOST = Integer.parseInt(System.getProperty("arecibo.collector.timelines.simulatedHostsPerRealHost", "20"));
    private static final int LOGGING_INTERVAL = Integer.parseInt(System.getProperty("arecibo.collector.timelines.loggingInterval", "10000"));
    private static final boolean PROCESS_SAMPLES = Boolean.parseBoolean(System.getProperty("arecibo.collector.timelines.processSamples", "true"));


    private final TimelineEventHandler eventHandler;
    private final TimelineDAO timelineDAO;
    private final Map<Integer, DateTime> latestHostTimes = new HashMap<Integer, DateTime>();
    private final AtomicBoolean shuttingDown = new AtomicBoolean();
    private final AtomicLong eventsSent = new AtomicLong();
    private final AtomicLong lateEvents = new AtomicLong();
    private final AtomicLong samplesAdded = new AtomicLong();

    private final AtomicLong timeWorking = new AtomicLong();
    private final AtomicLong timeSleeping = new AtomicLong();
    private final Replayer replayer = new Replayer(REPLAY_FILE_DIRECTORY);

    private Map<Integer, List<Integer>> simulatedHostIdsForRealHostId = new HashMap<Integer, List<Integer>>();
    private long cycleStartTime = 0;
    private int eventsPerSecondCount = 0;
    // This is a timestamp read from the replayed event
    private DateTime firstReplayEventTimestamp;
    // This is the corresponding real time timestamp
    private DateTime replayIterationStartTime;
    private DateTime lastEndTime = null;

    public EventReplayingLoadGenerator(TimelineEventHandler eventHandler, TimelineDAO timelineDAO)
    {
        this.eventHandler = eventHandler;
        this.timelineDAO = timelineDAO;
    }

    private void resetSecondCounter()
    {
        cycleStartTime = System.currentTimeMillis();
        eventsPerSecondCount = 0;
    }

    public void generateEventStream()
    {
        resetSecondCounter();
        replayIterationStartTime = new DateTime();
        for (int i=0; i<REPLAY_REPEAT_COUNT; i++) {
            final Collection<File> files = FileUtils.listFiles(new File(REPLAY_FILE_DIRECTORY), new String[]{"bin"}, false);
            firstReplayEventTimestamp = null;
            resetSecondCounter();
            for (final File file : Replayer.FILE_ORDERING.sortedCopy(files)) {
                try {
                    log.info("About to read file %s", file.getAbsolutePath());
                    replayer.read(file, new Function<HostSamplesForTimestamp, Void>() {

                        @Override
                        public Void apply(HostSamplesForTimestamp hostSamples) {
                            if (shuttingDown.get()) {
                                return null;
                            }
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
            replayIterationStartTime = lastEndTime.plusSeconds(30);
        }
    }

    private DateTime getAdjustedSampleTime(final DateTime timestamp)
    {
        if (firstReplayEventTimestamp == null) {
            firstReplayEventTimestamp = timestamp;
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
        lastEndTime = adjustedTime;
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
                    if (PROCESS_SAMPLES) {
                        eventHandler.processSamples(newSamples);
                    }
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
        if (sentCount % LOGGING_INTERVAL == 0) {
            log.info("%d events sent, %d late and ignored, %d samples added, ms working %d, ms sleeping %d",
                    sentCount, lateEvents.get(), samplesAdded.get(), timeWorking.get(), timeSleeping.get());
        }
        if (eventsPerSecondCount >= EVENTS_PER_SECOND) {
            final long elapsed = System.currentTimeMillis() - cycleStartTime;
            timeWorking.addAndGet(elapsed);
            if (elapsed < 1000) {
                final long sleepTime = 1000 - elapsed;
                timeSleeping.addAndGet(sleepTime);
                try {
                    Thread.sleep(sleepTime);
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

    @Managed
    public void initiateShutdown()
    {
        shuttingDown.set(true);
        replayer.initiateShutdown();
    }
}
