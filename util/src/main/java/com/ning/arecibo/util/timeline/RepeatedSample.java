package com.ning.arecibo.util.timeline;

public class RepeatedSample<T> extends SampleBase {
    public static final int MAX_REPEAT_COUNT = 255;

    private final ScalarSample<T> sample;

    private byte repeatCount;

    public RepeatedSample(byte repeatCount, ScalarSample<T> sample) {
        super(SampleOpcode.REPEAT);
        this.repeatCount = 0;
        this.sample = sample;

    }

    public byte getRepeatCount() {
        return repeatCount;
    }

    public void incrementRepeatCount() {
        repeatCount++;
    }

    public ScalarSample<T> getSample() {
        return sample;
    }
}
