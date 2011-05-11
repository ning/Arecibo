package com.ning.arecibo.util.timeline;

public class RepeatedSample<T> extends SampleBase {
    public static final int MAX_REPEAT_COUNT = 255;

    private final SampleBase sample;

    private byte repeatCount;

    public RepeatedSample(byte repeatCount, SampleBase sample) {
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

    public SampleBase getSample() {
        return sample;
    }
}
