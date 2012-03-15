package com.ning.arecibo.util.timeline;

import org.joda.time.DateTime;

public abstract class TimeRangeSampleProcessor implements SampleProcessor {
    private final DateTime startTime;  // Inclusive
    private final DateTime endTime;    // Inclusive

    public TimeRangeSampleProcessor(DateTime startTime, DateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Process sampleCount sequential samples with identical values.  sampleCount will usually be 1,
     * but may be larger than 1.  Implementors may just loop processing identical values, but some
     * implementations may optimize adding a bunch of repeated values
     *
     * @param timestamps   a TimelineTimestamps instance, indexed by sample number to get the time at which the sample was captured.
     * @param sampleNumber the number of the sample within the timeline, used to index timestamps
     * @param sampleCount  the count of sequential, identical values
     * @param opcode       the opcode of the sample value, which may not be a REPEAT opcode
     * @param value        the value of this kind of sample over the count of samples starting at the time
     *                     given by the sampleNumber indexing the TimelineTimestamps.
     */
    @Override
    public void processSamples(final TimelineTimes timestamps, final int sampleNumber, final int sampleCount, final SampleOpcode opcode, final Object value) {
        for (int i = 0; i < sampleCount; i++) {
            final DateTime sampleTimestamp = timestamps.getSampleTimestamp(sampleNumber + i);
            if (sampleTimestamp == null) {
                // Invalid?
                continue;
            }

            // Check if the sample is in the right time range
            long sampleMillis = sampleTimestamp.getMillis();
            if ((startTime == null || (sampleMillis >= startTime.getMillis())) && (endTime == null || (sampleMillis <= endTime.getMillis()))) {
                processOneSample(sampleTimestamp, sampleNumber, opcode, value);
            }
        }
    }

    public abstract void processOneSample(final DateTime time, final int sampleNumber, final SampleOpcode opcode, final Object value);
}
