package com.ning.arecibo.util.timeline;

public class NullSample extends ScalarSample<Void> {

    public NullSample() {
        super(SampleOpcode.NULL, null);
    }
}
