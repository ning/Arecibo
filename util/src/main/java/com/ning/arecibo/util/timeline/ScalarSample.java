package com.ning.arecibo.util.timeline;

/**
 * TODO: sampleValues will be boxed, and that sucks.  But we can replace
 * with 8 opcode-specific classes extending SampleBase.
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
