/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.util.timeline;

import com.ning.arecibo.util.Logger;



public enum SampleOpcode {
    BYTE((byte)1, 1),
    SHORT((byte)2, 2),
    INT((byte)3, 4),
    LONG((byte)4, 8),
    FLOAT((byte)5, 4),
    DOUBLE((byte)6, 8),
    STRING((byte)7, 0),
    NULL((byte)8, 0, true),
    FLOAT_FOR_DOUBLE((byte)10, 4, DOUBLE),
    HALF_FLOAT_FOR_DOUBLE((byte)11, 2, DOUBLE),
    BYTE_FOR_DOUBLE((byte)12, 1, DOUBLE),
    SHORT_FOR_DOUBLE((byte)13, 2, DOUBLE),
    BIGINT((byte)14, 0),
    DOUBLE_ZERO((byte)15, 0, true),
    INT_ZERO((byte)16, 0, true),
    REPEAT((byte)0x7f, 1, true);

    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();

    private byte opcodeIndex;
    private final int byteSize;
    private final boolean repeater;
    private final boolean noArgs;
    private final SampleOpcode replacement;

    private SampleOpcode(byte opcodeIndex, int byteSize) {
        this(opcodeIndex, byteSize, false);
    }

    private SampleOpcode(byte opcodeIndex, int byteSize, SampleOpcode replacement) {
        this.opcodeIndex = opcodeIndex;
        this.byteSize = byteSize;
        this.repeater = false;
        this.noArgs = false;
        this.replacement = replacement;
    }

    private SampleOpcode(byte opcodeIndex, int byteSize, boolean noArgs) {
        this(opcodeIndex, byteSize, noArgs, false);
    }

    private SampleOpcode(byte opcodeIndex, int byteSize, boolean noArgs, boolean repeater) {
        this.opcodeIndex = opcodeIndex;
        this.byteSize = byteSize;
        this.repeater = repeater;
        this.noArgs = noArgs;
        this.replacement = this;
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

    public boolean getNoArgs() {
        return noArgs;
    }

    public boolean getRepeater() {
        return repeater;
    }

    public SampleOpcode getReplacement() {
        return replacement;
    }
}
