package com.ning.arecibo.util.timeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.collect.Sets;
import com.ning.arecibo.util.Logger;


/**
 * This class represent collection of timelines, one
 * for each sample kind, each over a specific time period,
 * from a single host.  This class is used to accumulate samples
 * to be written to the database; a separate streaming class with
 * much less overhead is used to "play back" the samples read from
 * the db in response to dashboard queries.
 * <p>
 * All subordinate timelines contain the same number of samples,
 * but repeat opcodes may collapse adjacent identical values.
 */
public class SampleSetTimelineChunk {
    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();
    private static final DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();
    private static final NullSample nullSample = new NullSample();
    private static final boolean checkEveryAccess = Boolean.parseBoolean(System.getProperty("xn.arecibo.checkEveryAccess"));

    private final String hostName;
    private final DateTime startTime;
    private final Map<String, SampleTimelineChunk> timelines;

    private int sampleCount;

    private DateTime endTime;

    /**
     * A SampleSetTimelineChunk object is born the first time the manager receives set of samples from a host
     * @param samples a set of samples representing on transmission from the host.
     */
    public SampleSetTimelineChunk(HostSamplesForTimestamp samples) {
        this.hostName = samples.getHostName();
        final DateTime timestamp = samples.getTimestamp();
        this.startTime = timestamp;
        this.endTime = timestamp;
        this.sampleCount = 0;
        this.timelines = new HashMap<String, SampleTimelineChunk>();
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
            log.warn("Adding samples for host %s, timestamp %s is earlier than the end time %s; ignored",
                    hostName, dateFormatter.print(timestamp), dateFormatter.print(endTime));
            return;
        }
        final Set<String> currentKinds = Sets.newHashSet(timelines.keySet());
        for (Map.Entry<String, ScalarSample> entry : samples.getSamples().entrySet()) {
            final String sampleKind = entry.getKey();
            currentKinds.remove(sampleKind);
            final ScalarSample sample = entry.getValue();
            SampleTimelineChunk timeline = timelines.get(samples.getHostName());
            if (timeline == null) {
                timeline = new SampleTimelineChunk(this, sampleKind);
                if (sampleCount > 0) {
                    addPlaceholders(timeline, sampleCount);
                }
                timelines.put(sampleKind, timeline);
            }
            timeline.addSample(sample);
        }
        // Now make sure to advance the timelines we haven't added samples to,
        // since the samples for a given sample kind can come and go
        for (String sampleKind : currentKinds) {
            final SampleTimelineChunk timeline = timelines.get(sampleKind);
            timeline.addSample(nullSample);
        }
        // Now we can update the state
        endTime = timestamp;
        sampleCount++;

        if (checkEveryAccess) {
            checkSampleCounts(sampleCount);
        }
    }

    private void addPlaceholders(final SampleTimelineChunk timeline, int countToAdd) {
        final int maxRepeatSamples = RepeatedSample.MAX_REPEAT_COUNT;
        while (countToAdd >= maxRepeatSamples) {
            timeline.addPlaceholder((byte)maxRepeatSamples);
            countToAdd -= maxRepeatSamples;
        }
        if (countToAdd > 0) {
            timeline.addPlaceholder((byte)countToAdd);
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
                    hostName, dateFormatter.print(startTime), sampleCount, assertedCount);
            success = false;
        }
        for (Map.Entry<String, SampleTimelineChunk> entry : timelines.entrySet()) {
            final String sampleKind = entry.getKey();
            final SampleTimelineChunk timeline = entry.getValue();
            final int lineSampleCount = timeline.getSampleCount();
            if (lineSampleCount != assertedCount) {
                log.error("For host %d, start time %s, timeline %s, the sampleCount %d is not equal to the assertedCount %d",
                        hostName, dateFormatter.print(startTime), sampleKind, lineSampleCount, assertedCount);
                success = false;
            }
        }
        return success;
    }

    public String getHostName() {
        return hostName;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public Map<String, SampleTimelineChunk> getTimelines() {
        return timelines;
    }
}
