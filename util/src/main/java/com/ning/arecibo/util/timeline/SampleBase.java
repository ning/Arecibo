package com.ning.arecibo.util.timeline;

public class SampleBase {
    private final SampleOpcode opcode;

    public SampleBase(SampleOpcode opcode) {
        this.opcode = opcode;
    }

    public SampleOpcode getOpcode() {
        return opcode;
    }
}
