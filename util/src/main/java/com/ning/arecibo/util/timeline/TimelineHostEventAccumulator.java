package com.ning.arecibo.util.timeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.collect.Sets;
import com.ning.arecibo.util.Logger;


/**
 * This class represents a collection of timeline chunks, one
 * for each sample kind, each over a specific time period,
 * from a single host.  This class is used to accumulate samples
 * to be written to the database; a separate streaming class with
 * much less overhead is used to "play back" the samples read from
 * the db in response to dashboard queries.
 * <p>
 * TODO: There is no synchronization in this class, so either prove that
 * the caller is single-threaded or add it.
 * <p>
 * All subordinate timelines contain the same number of samples,
 * but repeat opcodes may collapse adjacent identical values.
 */
public class TimelineHostEventAccumulator {
    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();
    private static final DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();
    private static final NullSample nullSample = new NullSample();
    private static final boolean checkEveryAccess = Boolean.parseBoolean(System.getProperty("xn.arecibo.checkEveryAccess"));

    private final TimelineDAO dao;
    private final int hostId;
    private final String category;
    private final DateTime startTime;
    private int sampleCount;
    private DateTime endTime;

    /**
     * Maps the sample kind id to the accumulator for that sample kind
     */
    private final Map<Integer, TimelineChunkAccumulator> timelines;
    /**
     * Holds the time of the samples
     */
    private final List<DateTime> times;

    /**
     * A TimelineHostEventAccumulator object is born the first time the manager receives set of samples from a host
     * @param samples a set of samples representing on transmission from the host.
     */
    public TimelineHostEventAccumulator(TimelineDAO dao, HostSamplesForTimestamp samples) {
        this.dao = dao;
        this.hostId = samples.getHostId();
        this.category = samples.getCategory();
        final DateTime timestamp = samples.getTimestamp();
        this.startTime = timestamp;
        this.endTime = timestamp;
        this.sampleCount = 0;
        this.timelines = new HashMap<Integer, TimelineChunkAccumulator>();
        this.times = new ArrayList<DateTime>();
        addHostSamples(samples);
    }

    @SuppressWarnings("unchecked")
    public void addHostSamples(final HostSamplesForTimestamp samples) {
        // You can only add samples at the end of a timeline;
        // If sample time is not >= endtime, log a warning
        // TODO: Figure out if this constraint is realistic, or if
        // we need a reordering pipeline to eliminate the constraint.
        // The reasoning is that these samples only apply to this host,
        // so we shouldn't get essentially simultaneous adds
        final DateTime timestamp = samples.getTimestamp();
        if (timestamp.isBefore(endTime)) {
            log.warn("Adding samples for host %d, timestamp %s is earlier than the end time %s; ignored",
                    hostId, dateFormatter.print(timestamp), dateFormatter.print(endTime));
            return;
        }
        final Set<Integer> currentKinds = Sets.newHashSet(timelines.keySet());
        for (Map.Entry<Integer, ScalarSample> entry : samples.getSamples().entrySet()) {
            final Integer sampleKindId = entry.getKey();
            currentKinds.remove(sampleKindId);
            final ScalarSample sample = entry.getValue();
            TimelineChunkAccumulator timeline = timelines.get(samples.getHostId());
            if (timeline == null) {
                timeline = new TimelineChunkAccumulator(hostId, sampleKindId);
                if (sampleCount > 0) {
                    addPlaceholders(timeline, sampleCount);
                }
                timelines.put(sampleKindId, timeline);
            }
            timeline.addSample(sample);
        }
        // Now make sure to advance the timelines we haven't added samples to,
        // since the samples for a given sample kind can come and go
        for (Integer sampleKindId : currentKinds) {
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

    private void addPlaceholders(final TimelineChunkAccumulator timeline, int countToAdd) {
        final int maxRepeatSamples = RepeatedSample.MAX_REPEAT_COUNT;
        while (countToAdd >= maxRepeatSamples) {
            timeline.addPlaceholder((byte)maxRepeatSamples);
            countToAdd -= maxRepeatSamples;
        }
        if (countToAdd > 0) {
            timeline.addPlaceholder((byte)countToAdd);
        }
    }

    public static class TimesAndTimelineChunks {
        private final TimelineTimes times;
        private final List<TimelineChunk> timelines;

        public TimesAndTimelineChunks(TimelineTimes times, List<TimelineChunk> timelines) {
            this.times = times;
            this.timelines = timelines;
        }

        public TimelineTimes getTimes() {
            return times;
        }

        public List<TimelineChunk> getTimelines() {
            return timelines;
        }
    }

    /**
     * This method "rotates" the accumulators in this set, creating and saving the
     * db representation of the timelines, and clearing the accumulators so they
     * have no samples in them.
     * TODO: I'm not clear on the synchronization paradigm.  Is it reasonable to
     * have just one thread adding samples, and writing stuff to the db?
     */
    public void extractAndSaveTimelineChunks() {
        final TimelineTimes dbTimelineTimes = new TimelineTimes(0, hostId, startTime, endTime, times);
        final int timelineTimesId = dao.insertTimelineTimes(dbTimelineTimes);
        for (TimelineChunkAccumulator accumulator : timelines.values()) {
            dao.insertTimelineChunk(accumulator.extractTimelineChunkAndReset(timelineTimesId));
        }
    }

    /**
     * Make sure all timelines have the sample count passed in; otherwise log
     * discrepancies and return false
     * @param assertedCount The sample count that all timelines are supposed to have
     * @return true if all timelines have the right count; false otherwise
     */
    public boolean checkSampleCounts(final int assertedCount) {
        boolean success = true;
        if (assertedCount != sampleCount) {
            log.error("For host %d, start time %s, the HostTimeLines sampleCount %d is not equal to the assertedCount %d",
                    hostId, dateFormatter.print(startTime), sampleCount, assertedCount);
            success = false;
        }
        for (Map.Entry<Integer, TimelineChunkAccumulator> entry : timelines.entrySet()) {
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

    public int getHostId() {
        return hostId;
    }


    public String getCategory() {
        return category;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public Map<Integer, TimelineChunkAccumulator> getTimelines() {
        return timelines;
    }
}
