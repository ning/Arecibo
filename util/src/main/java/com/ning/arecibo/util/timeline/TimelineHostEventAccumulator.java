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

package com.ning.arecibo.util.timeline;

import com.google.common.collect.Sets;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.timeline.persistent.FileBackedBuffer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class represents a collection of timeline chunks, one
 * for each sample kind, each over a specific time period,
 * from a single host.  This class is used to accumulate samples
 * to be written to the database; a separate streaming class with
 * much less overhead is used to "play back" the samples read from
 * the db in response to dashboard queries.
 * <p/>
 * TODO: There is no synchronization in this class, so either prove that
 * the caller is single-threaded or add it.
 * <p/>
 * All subordinate timelines contain the same number of samples,
 * but repeat opcodes may collapse adjacent identical values.
 */
public class TimelineHostEventAccumulator
{
    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();
    private static final DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();
    private static final NullSample nullSample = new NullSample();
    private static final boolean checkEveryAccess = Boolean.parseBoolean(System.getProperty("xn.arecibo.checkEveryAccess"));

    private final FileBackedBuffer backingBuffer;

    private final TimelineDAO dao;
    private final int hostId;
    private DateTime startTime = null;
    private DateTime endTime = null;
    private int sampleCount = 0;

    /**
     * Maps the sample kind id to the accumulator for that sample kind
     */
    private final Map<Integer, TimelineChunkAccumulator> timelines = new HashMap<Integer, TimelineChunkAccumulator>();
    /**
     * Holds the time of the samples
     */
    private final List<DateTime> times = new ArrayList<DateTime>();

    public TimelineHostEventAccumulator(final String spoolDir, final TimelineDAO dao, final int hostId) throws IOException
    {
        this.dao = dao;
        this.hostId = hostId;
        this.backingBuffer = new FileBackedBuffer(spoolDir, String.format("%d", this.hostId));
    }

    @SuppressWarnings("unchecked")
    // TODO - we can probably do better than synchronize the whole method
    public synchronized void addHostSamples(final HostSamplesForTimestamp samples)
    {
        // Start by saving locally the samples
        backingBuffer.append(samples);

        // You can only add samples at the end of a timeline;
        // If sample time is not >= endtime, log a warning
        // TODO: Figure out if this constraint is realistic, or if
        // we need a reordering pipeline to eliminate the constraint.
        // The reasoning is that these samples only apply to this host,
        // so we shouldn't get essentially simultaneous adds
        final DateTime timestamp = samples.getTimestamp();

        if (startTime == null) {
            startTime = timestamp;
        }
        if (endTime == null) {
            endTime = timestamp;
        }

        if (timestamp.isBefore(endTime)) {
            log.warn("Adding samples for host %d, timestamp %s is earlier than the end time %s; ignored",
                hostId, dateFormatter.print(timestamp), dateFormatter.print(endTime));
            return;
        }
        final Set<Integer> currentKinds = Sets.newHashSet(timelines.keySet());
        for (final Map.Entry<Integer, ScalarSample> entry : samples.getSamples().entrySet()) {
            final Integer sampleKindId = entry.getKey();
            currentKinds.remove(sampleKindId);
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
        // Now make sure to advance the timelines we haven't added samples to,
        // since the samples for a given sample kind can come and go
        for (final Integer sampleKindId : currentKinds) {
            final TimelineChunkAccumulator timeline = timelines.get(sampleKindId);
            timeline.addSample(nullSample);
        }
        // Now we can update the state
        endTime = timestamp;
        times.add(timestamp);
        sampleCount++;

        if (checkEveryAccess) {
            checkSampleCounts(sampleCount);
        }
    }

    private void addPlaceholders(final TimelineChunkAccumulator timeline, int countToAdd)
    {
        final int maxRepeatSamples = RepeatedSample.MAX_REPEAT_COUNT;
        while (countToAdd >= maxRepeatSamples) {
            timeline.addPlaceholder((byte) maxRepeatSamples);
            countToAdd -= maxRepeatSamples;
        }
        if (countToAdd > 0) {
            timeline.addPlaceholder((byte) countToAdd);
        }
    }

    /**
     * This method "rotates" the accumulators in this set, creating and saving the
     * db representation of the timelines, and clearing the accumulators so they
     * have no samples in them.
     * TODO: I'm not clear on the synchronization paradigm.  Is it reasonable to
     * have just one thread adding samples, and writing stuff to the db?
     */
    public void extractAndSaveTimelineChunks()
    {
        final TimelineTimes dbTimelineTimes = new TimelineTimes(0, hostId, startTime, endTime, times);
        final int timelineTimesId = dao.insertTimelineTimes(dbTimelineTimes);
        for (final TimelineChunkAccumulator accumulator : timelines.values()) {
            dao.insertTimelineChunk(accumulator.extractTimelineChunkAndReset(timelineTimesId));
        }

        // All the samples have been saved, discard the local buffer
        // Note: it is fine if we end up storing dups at replay time
        backingBuffer.discard();
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
            log.error("For host %d, start time %s, the HostTimeLines sampleCount %d is not equal to the assertedCount %d",
                hostId, dateFormatter.print(startTime), sampleCount, assertedCount);
            success = false;
        }
        for (final Map.Entry<Integer, TimelineChunkAccumulator> entry : timelines.entrySet()) {
            final int sampleKindId = entry.getKey();
            final TimelineChunkAccumulator timeline = entry.getValue();
            final int lineSampleCount = timeline.getSampleCount();
            if (lineSampleCount != assertedCount) {
                log.error("For host %d, start time %s, sample kind id %s, the sampleCount %d is not equal to the assertedCount %d",
                    hostId, dateFormatter.print(startTime), sampleKindId, lineSampleCount, assertedCount);
                success = false;
            }
        }
        return success;
    }

    public int getHostId()
    {
        return hostId;
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

    public FileBackedBuffer getBackingBuffer()
    {
        return backingBuffer;
    }
}
