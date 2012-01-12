package com.ning.arecibo.util.timeline;

/**
 * @param <T> A value consistent with the opcode
 */
public class ScalarSample<T> extends SampleBase {

    private final T sampleValue;

    public ScalarSample(SampleOpcode opcode, T sampleValue) {
        super(opcode);
        this.sampleValue = sampleValue;
    }

    public T getSampleValue() {
        return sampleValue;
    }
}
