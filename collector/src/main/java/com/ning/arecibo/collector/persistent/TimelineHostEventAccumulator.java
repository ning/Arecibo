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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.NullSample;
import com.ning.arecibo.util.timeline.RepeatSample;
import com.ning.arecibo.util.timeline.SampleCoder;
import com.ning.arecibo.util.timeline.ScalarSample;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineChunkAccumulator;
import com.ning.arecibo.util.timeline.TimelineCoder;
import com.ning.arecibo.util.timeline.TimelineDAO;

/**
 * This class represents a collection of timeline chunks, one for each sample
 * kind belonging to one event category, each over a specific time period,
 * from a single host.  This class is used to accumulate samples
 * to be written to the database; a separate streaming class with
 * much less overhead is used to "play back" the samples read from
 * the db in response to dashboard queries.
 * <p/>
 * All subordinate timelines contain the same number of samples.
 * <p/>
 * When enough samples have accumulated, typically one hour's worth,
 * in-memory samples are made into TimelineChunks, one chunk for each sampleKindId
 * maintained by the accumulator.
 * <p/>
 * These new chunks are organized as PendingChunkMaps, kept in a local list and also
 * handed off to a PendingChunkMapConsumer to written to the db by a background process.  At some
 * in the future, that background process will call markPendingChunkMapConsumed(),
 * passing the id of a PendingChunkMap.  This causes the PendingChunkMap
 * to be removed from the local list maintained by the TimelineHostEventAccumulator.
 * <p/>
 * Queries that cause the TimelineHostEventAccumulator instance to return memory
 * chunks also return any chunks in PendingChunkMaps in the local list of pending chunks.
 */
public class TimelineHostEventAccumulator
{
    private static final Logger log = LoggerFactory.getLogger(TimelineHostEventAccumulator.class);
    private static final DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();
    private static final NullSample nullSample = new NullSample();
    private static final boolean checkEveryAccess = Boolean.parseBoolean(System.getProperty("xn.arecibo.checkEveryAccess"));
    private static final Random rand = new Random(0);

    private final Map<Integer, SampleSequenceNumber> sampleKindIdCounters = new HashMap<Integer, SampleSequenceNumber>();
    private final List<PendingChunkMap> pendingChunkMaps = new ArrayList<PendingChunkMap>();
    private long pendingChunkMapIdCounter = 1;

    private final BackgroundDBChunkWriter backgroundWriter;
    private final Integer timelineLengthMillis;
    private final int hostId;
    private final int eventCategoryId;
    // This is the time when we want to end the chunk.  Setting the value randomly
    // when the TimelineHostEventAccumulator  is created provides a mechanism to
    // distribute the db writes during the 1 hour when
    private DateTime chunkEndTime = null;
    private DateTime startTime = null;
    private DateTime endTime = null;
    private DateTime latestSampleAddTime;
    private long sampleSequenceNumber = 0;
    private int sampleCount = 0;

    /**
     * Maps the sample kind id to the accumulator for that sample kind
     */
    private final Map<Integer, TimelineChunkAccumulator> timelines = new ConcurrentHashMap<Integer, TimelineChunkAccumulator>();

    /**
     * Holds the sampling times of the samples
     */
    private final List<DateTime> times = new ArrayList<DateTime>();

    public TimelineHostEventAccumulator(final TimelineDAO dao, final BackgroundDBChunkWriter backgroundWriter,
            final int hostId, final int eventCategoryId, final DateTime firstSampleTime, Integer timelineLengthMillis)
    {
        this.timelineLengthMillis = timelineLengthMillis;
        this.backgroundWriter = backgroundWriter;
        this.hostId = hostId;
        this.eventCategoryId = eventCategoryId;
        // Set the end-of-chunk time by tossing a random number, to evenly distribute the db writeback load.
        this.chunkEndTime = timelineLengthMillis != null ? firstSampleTime.plusMillis(rand.nextInt(timelineLengthMillis)) : null;
    }

    /*
     * This constructor is used for testing; it writes chunks as soon as they are
     * created, but because the chunkEndTime is way in the future, doesn't initiate
     * chunk writes.
     */
    public TimelineHostEventAccumulator(TimelineDAO timelineDAO, Integer hostId, int eventTypeId, DateTime firstSampleTime)
    {
        this(timelineDAO, new BackgroundDBChunkWriter(timelineDAO, null, true), hostId, eventTypeId, firstSampleTime, Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    // TODO - we can probably do better than synchronize the whole method
    public synchronized void addHostSamples(final HostSamplesForTimestamp samples)
    {
        final DateTime timestamp = samples.getTimestamp();

        if (chunkEndTime != null && chunkEndTime.isBefore(timestamp)) {
            extractAndQueueTimelineChunks();
            startTime = timestamp;
            chunkEndTime = timestamp.plusMillis(timelineLengthMillis);
        }

        if (startTime == null) {
            startTime = timestamp;
        }
        if (endTime == null) {
            endTime = timestamp;
        }
        else if (!timestamp.isAfter(endTime)) {
            log.warn("Adding samples for host {}, timestamp {} is not after the end time {}; ignored",
                new Object[]{hostId, dateFormatter.print(timestamp), dateFormatter.print(endTime)});
            return;
        }
        sampleSequenceNumber++;
        latestSampleAddTime = new DateTime();
        for (final Map.Entry<Integer, ScalarSample> entry : samples.getSamples().entrySet()) {
            final Integer sampleKindId = entry.getKey();
            final SampleSequenceNumber counter = sampleKindIdCounters.get(sampleKindId);
            if (counter != null) {
                counter.setSequenceNumber(sampleSequenceNumber);
            }
            else {
                sampleKindIdCounters.put(sampleKindId, new SampleSequenceNumber(sampleSequenceNumber));
            }
            final ScalarSample sample = entry.getValue();
            TimelineChunkAccumulator timeline = timelines.get(sampleKindId);
            if (timeline == null) {
                timeline = new TimelineChunkAccumulator(hostId, sampleKindId);
                if (sampleCount > 0) {
                    addPlaceholders(timeline, sampleCount);
                }
                timelines.put(sampleKindId, timeline);
            }
            final ScalarSample compressedSample = SampleCoder.compressSample(sample);
            timeline.addSample(compressedSample);
        }
        for (Map.Entry<Integer, SampleSequenceNumber> entry : sampleKindIdCounters.entrySet()) {
            final SampleSequenceNumber counter = entry.getValue();
            if (counter.getSequenceNumber() < sampleSequenceNumber) {
                counter.setSequenceNumber(sampleSequenceNumber);
                final int sampleKindId = entry.getKey();
                final TimelineChunkAccumulator timeline = timelines.get(sampleKindId);
                timeline.addSample(nullSample);
            }
        }
        // Now we can update the state
        endTime = timestamp;
        sampleCount++;
        times.add(timestamp);

        if (checkEveryAccess) {
            checkSampleCounts(sampleCount);
        }
    }

    private void addPlaceholders(final TimelineChunkAccumulator timeline, int countToAdd)
    {
        final int maxRepeatSamples = RepeatSample.MAX_SHORT_REPEAT_COUNT;
        while (countToAdd >= maxRepeatSamples) {
            timeline.addPlaceholder((byte) maxRepeatSamples);
            countToAdd -= maxRepeatSamples;
        }
        if (countToAdd > 0) {
            timeline.addPlaceholder((byte) countToAdd);
        }
    }

    /**
     * This method queues a map of TimelineChunks extracted from the TimelineChunkAccumulators
     * to be written to the db.  When memory chunks are requested, any queued chunk will be included
     * in the list.
     */
    public synchronized void extractAndQueueTimelineChunks()
    {
        if (times.size() > 0) {
            final Map<Integer, TimelineChunk> chunkMap = new HashMap<Integer, TimelineChunk>();
            final byte[] timeBytes = TimelineCoder.compressDateTimes(times);
            for (final Map.Entry<Integer, TimelineChunkAccumulator> entry : timelines.entrySet()) {
                final int sampleKindId = entry.getKey();
                final TimelineChunkAccumulator accumulator = entry.getValue();
                final TimelineChunk chunk = accumulator.extractTimelineChunkAndReset(startTime, endTime, timeBytes);
                chunkMap.put(sampleKindId, chunk);
            }
            times.clear();
            sampleCount = 0;
            final long counter = pendingChunkMapIdCounter++;
            final PendingChunkMap newChunkMap = new PendingChunkMap(this, counter, chunkMap);
            pendingChunkMaps.add(newChunkMap);
            backgroundWriter.addPendingChunkMap(newChunkMap);
        }
    }

    public synchronized void markPendingChunkMapConsumed(final long pendingChunkMapId)
    {
        final PendingChunkMap pendingChunkMap = pendingChunkMaps.size() > 0 ? pendingChunkMaps.get(0) : null;
        if (pendingChunkMap == null) {
            log.error("In TimelineHostEventAccumulator.markPendingChunkMapConsumed(), could not find the map for {}", pendingChunkMapId);
        }
        else if (pendingChunkMapId != pendingChunkMap.getPendingChunkMapId()) {
            log.error("In TimelineHostEventAccumulator.markPendingChunkMapConsumed(), the next map has id {}, but we're consuming id {}",
                    pendingChunkMap.getPendingChunkMapId(), pendingChunkMapId);
        }
        else {
            pendingChunkMaps.remove(0);
        }
    }

    public synchronized List<TimelineChunk> getPendingTimelineChunks()
    {
        final List<TimelineChunk> timelineChunks = new ArrayList<TimelineChunk>();
        for (PendingChunkMap pendingChunkMap : pendingChunkMaps) {
            timelineChunks.addAll(pendingChunkMap.getChunkMap().values());
        }
        return timelineChunks;
    }

    /**
     * Make sure all timelines have the sample count passed in; otherwise log
     * discrepancies and return false
     *
     * @param assertedCount The sample count that all timelines are supposed to have
     * @return true if all timelines have the right count; false otherwise
     */
    public boolean checkSampleCounts(final int assertedCount)
    {
        boolean success = true;
        if (assertedCount != sampleCount) {
            log.error("For host {}, start time {}, the HostTimeLines sampleCount {} is not equal to the assertedCount {}",
                new Object[]{hostId, dateFormatter.print(startTime), sampleCount, assertedCount});
            success = false;
        }
        for (final Map.Entry<Integer, TimelineChunkAccumulator> entry : timelines.entrySet()) {
            final int sampleKindId = entry.getKey();
            final TimelineChunkAccumulator timeline = entry.getValue();
            final int lineSampleCount = timeline.getSampleCount();
            if (lineSampleCount != assertedCount) {
                log.error("For host {}, start time {}, sample kind id {}, the sampleCount {} is not equal to the assertedCount {}",
                    new Object[]{hostId, dateFormatter.print(startTime), sampleKindId, lineSampleCount, assertedCount});
                success = false;
            }
        }
        return success;
    }

    public int getHostId()
    {
        return hostId;
    }

    public int getEventCategoryId()
    {
        return eventCategoryId;
    }

    public DateTime getStartTime()
    {
        return startTime;
    }

    public DateTime getEndTime()
    {
        return endTime;
    }

    public Map<Integer, TimelineChunkAccumulator> getTimelines()
    {
        return timelines;
    }

    public List<DateTime> getTimes()
    {
        return times;
    }

    public DateTime getLatestSampleAddTime()
    {
        return latestSampleAddTime;
    }

    private static class SampleSequenceNumber
    {
        private long sequenceNumber;

        public SampleSequenceNumber(long sequenceNumber)
        {
            this.sequenceNumber = sequenceNumber;
        }

        public long getSequenceNumber()
        {
            return sequenceNumber;
        }

        public void setSequenceNumber(long sequenceNumber)
        {
            this.sequenceNumber = sequenceNumber;
        }
    }
}
