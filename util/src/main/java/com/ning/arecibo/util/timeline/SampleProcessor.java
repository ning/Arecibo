package com.ning.arecibo.util.timeline;

public interface SampleProcessor {

    /**
     * Process sampleCount sequential samples with identical values.  sampleCount will usually be 1,
     * but may be larger than 1.  Implementors may just loop processing identical values, but some
     * implementations may optimize add a bunch of values
     * @param timestamps a TimelineTimestamps instance, indexed by sample number to get the time at which the sample was captured.
     * @param sampleNumber the number of the sample within the timeline, used to index timestamps
     * @param sampleCount the count of sequential, identical values
     * @param opcode the opcode of the sample value, which may not be a REPEAT opcode
     * @param value the value to be matched
     */
    public void processSamples(final TimelineTimestamps timestamps, final int sampleNumber, final int sampleCount,
                               final SampleOpcode opcode, final Object value);

}
