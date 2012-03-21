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
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ning.arecibo.util.Logger;

public class TimelineCoder {
    public static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();
    public static final int MAX_DELTA_TIME = 0x7F;
    public static final int MAX_REPEAT_COUNT = 0xFF;

    public static byte[] compressTimes(final int[] times) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(times.length / 3);
        final DataOutputStream dataStream = new DataOutputStream(outputStream);
        try {
            int lastTime = times[0];
            int lastDelta = 0;
            int repeatCount = 0;
            writeTime(0, lastTime, dataStream);
            for (int i=1; i<times.length; i++) {
                final int newTime = times[i];
                if (newTime <= lastTime) {
                    log.warn("In TimelineCoder.compressTimes(), newTime %d is <= lastTime %d; ignored", newTime, lastTime);
                    continue;
                }
                final int delta = newTime - lastTime;
                final boolean deltaWorks = delta <= MAX_DELTA_TIME;
                final boolean sameDelta = repeatCount > 0 && delta == lastDelta;
                if (deltaWorks) {
                    if (sameDelta) {
                        repeatCount++;
                        if (repeatCount == MAX_REPEAT_COUNT) {
                            writeRepeatedDelta(delta, repeatCount, dataStream);
                            repeatCount = 0;
                        }
                    }
                    else {
                        if (repeatCount > 0) {
                            writeRepeatedDelta(lastDelta, repeatCount, dataStream);
                        }
                        repeatCount = 1;
                    }
                    lastDelta = delta;
                }
                else {
                    if (repeatCount > 0) {
                        writeRepeatedDelta(lastDelta, repeatCount, dataStream);
                    }
                    writeTime(0, newTime, dataStream);
                    repeatCount = 0;
                    lastDelta = 0;
                }
                lastTime = newTime;
            }
            if (repeatCount > 0) {
                writeRepeatedDelta(lastDelta, repeatCount, dataStream);
            }
            outputStream.flush();
            return outputStream.toByteArray();
        }
        catch (IOException e) {
            log.error(e, "Exception compressing times array of length %d", times.length);
            return null;
        }
    }

    public static int[] decompressTimes(final byte[] compressedTimes) {
        final List<Integer> intList = new ArrayList<Integer>(compressedTimes.length * 4);
        final ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedTimes);
        final DataInputStream byteDataStream = new DataInputStream(byteStream);
        int opcode = 0;
        int lastTime = 0;
        try {
            while (true) {
                try {
                    opcode = byteDataStream.readUnsignedByte();
                }
                catch (EOFException e) {
                }

                if (opcode == TimelineOpcode.FULL_TIME.getOpcodeIndex()) {
                    lastTime = byteDataStream.readInt();
                    intList.add(lastTime);
                }
                else if (opcode == TimelineOpcode.REPEATED_DELTA_TIME.getOpcodeIndex()) {
                    final int repeatCount = byteDataStream.readUnsignedByte();
                    final int delta = byteDataStream.readUnsignedByte();
                    for (int i=0; i<repeatCount; i++) {
                        lastTime = lastTime + delta;
                        intList.add(lastTime);
                    }
                }
                else {
                    // The opcode is itself a singleton delta
                    lastTime = lastTime + opcode;
                    intList.add(lastTime);
                }
            }
        }
        catch (IOException e) {

        }
        final int[] intArray = new int[intList.size()];
        for (int i=0; i<intList.size(); i++) {
            intArray[i] = intList.get(i);
        }
        return intArray;
    }

    private static void writeRepeatedDelta(final int delta, final int repeatCount, final DataOutputStream dataStream) throws IOException {
        if (repeatCount > 1) {
            dataStream.writeByte(TimelineOpcode.REPEATED_DELTA_TIME.getOpcodeIndex());
            dataStream.writeByte(repeatCount);
        }
        dataStream.writeByte(delta);
    }

    private static void writeTime(final int lastTime, final int newTime, final DataOutputStream dataStream) throws IOException {
        if (newTime > lastTime) {
            final int delta = (newTime - lastTime);
            if (delta <= MAX_DELTA_TIME) {
                dataStream.writeByte(delta);
            }
            else {
                dataStream.writeByte(TimelineOpcode.FULL_TIME.getOpcodeIndex());
                dataStream.writeInt(newTime);
            }
        }
    }
}
