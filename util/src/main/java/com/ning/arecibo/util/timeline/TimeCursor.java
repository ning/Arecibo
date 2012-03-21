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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.ning.arecibo.util.Logger;

public class TimeCursor
{
    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();

    private final DataInputStream timelineDataStream;;
    private int byteCursor;
    private int lastValue;
    private int delta;
    private int repeatCount;

    public TimeCursor(TimelineTimes timelineTimes)
    {
        this.timelineDataStream = new DataInputStream(new ByteArrayInputStream(timelineTimes.getCompressedTimes()));
        this.byteCursor = 0;
        this.lastValue = 0;
        this.delta = 0;
        this.repeatCount = 0;
    }

    private int getNextTimeInternal()
    {
        try {
        if (repeatCount > 0) {
            repeatCount--;
            lastValue += delta;
        }
        else {
            final int nextOpcode = timelineDataStream.read();
            byteCursor++;
            if (nextOpcode == -1) {
                return nextOpcode;
            }
            if (nextOpcode == TimelineOpcode.FULL_TIME.getOpcodeIndex()) {
                lastValue = timelineDataStream.readInt();
                byteCursor += 4;
                return lastValue;
            }
            else if (nextOpcode == TimelineOpcode.REPEATED_DELTA_TIME.getOpcodeIndex()) {
                repeatCount =  timelineDataStream.read() - 1;
                delta = timelineDataStream.read();
                byteCursor += 2;
                lastValue += delta;
                return lastValue;
            }
            else if (nextOpcode <= TimelineCoder.MAX_DELTA_TIME) {
                byteCursor++;
                lastValue += timelineDataStream.read();
            }
            else {
                throw new IllegalStateException(String.format("In TimeIterator.getNextTime(), unknown opcode %x at offset %d", nextOpcode, byteCursor));
            }
        }
        return lastValue;
        }
        catch (IOException e) {
            log.error(e, "IOException in TimeIterator.getNextTime()");
            return -1;
        }
    }

    public void consumeRepeat() {
        lastValue += repeatCount * delta;
    }

    public int getNextTime()
    {
        final int nextTime = getNextTimeInternal();
        if (nextTime == -1) {
            throw new IllegalStateException(String.format("In DecodedSampleOutputProcessor.getNextTime(), got -1 from timeCursor.getNextTime()"));
        }
        else {
            return nextTime;
        }
    }
}
