package com.ning.arecibo.util.timeline;

import com.ning.arecibo.util.Logger;



public enum SampleOpcode {
    BYTE((byte)1, 1, false),
    SHORT((byte)2, 2, false),
    INT((byte)3, 4, false),
    LONG((byte)4, 8, false),
    FLOAT((byte)5, 4, false),
    DOUBLE((byte)6, 8, false),
    STRING((byte)7, 0, false),
    NULL((byte)8, 0, false),
    REPEAT((byte)9, 0, true);

    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();

    private byte opcodeIndex;
    private final int byteSize;
    private final boolean repeater;

    private SampleOpcode(byte opcodeIndex, int byteSize, boolean repeater) {
        this.opcodeIndex = opcodeIndex;
        this.byteSize = byteSize;
        this.repeater = repeater;
    }

    public byte getOpcodeIndex() {
        return opcodeIndex;
    }

    public static SampleOpcode getOpcodeFromIndex(final byte index) {
        for (SampleOpcode opcode : values()) {
            if (opcode.getOpcodeIndex() == index) {
                return opcode;
            }
        }
        final String s = String.format("In SampleOpcode.getOpcodefromIndex(), could not find opcode for index %d", index);
        log.error(s);
        throw new IllegalArgumentException(s);
    }

    public int getByteSize() {
        return byteSize;
    }

    public boolean getRepeater() {
        return repeater;
    }
}
