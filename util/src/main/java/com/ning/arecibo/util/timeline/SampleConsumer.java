package com.ning.arecibo.util.timeline;

import org.joda.time.DateTime;

public interface SampleConsumer {
    public void consumeSample(int sampleNumber, SampleOpcode opcode, Object value, DateTime time);
}

