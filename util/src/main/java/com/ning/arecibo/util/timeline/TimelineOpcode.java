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
/**
 * Opcodes are 1-byte entities.  Any "opcode" whose value is 127 or less is treated
 * as a time delta to be added to the previous time value.
 */

enum TimelineOpcode {
    FULL_TIME(0x7f),                 // Followed by 4 bytes of int value
    REPEATED_DELTA_TIME(0x7e);       // Followed by a repeat count byte, 1-255, and then by a 1-byte delta whose value is 1-127

    private int opcodeIndex;

    private TimelineOpcode(int opcodeIndex) {
        this.opcodeIndex = opcodeIndex;
    }

    public int getOpcodeIndex() {
        return opcodeIndex;
    }
}
