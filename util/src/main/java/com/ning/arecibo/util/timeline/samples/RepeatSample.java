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

package com.ning.arecibo.util.timeline.samples;


public class RepeatSample<T> extends SampleBase {
    public static final int MAX_BYTE_REPEAT_COUNT = 0xFF; // The maximum byte value
    public static final int MAX_SHORT_REPEAT_COUNT = 0xFFFF; // The maximum byte value

    private final ScalarSample<T> sampleRepeated;

    private int repeatCount;

    public RepeatSample(int repeatCount, ScalarSample<T> sampleRepeated) {
        super(SampleOpcode.REPEAT_BYTE);
        this.repeatCount = repeatCount;
        this.sampleRepeated = sampleRepeated;

    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void incrementRepeatCount() {
        repeatCount++;
    }

    public void incrementRepeatCount(final int addend) {
        repeatCount += addend;
    }

    public ScalarSample<T> getSampleRepeated() {
        return sampleRepeated;
    }

    @Override
    public SampleOpcode getOpcode()
    {
        return repeatCount > MAX_BYTE_REPEAT_COUNT ? SampleOpcode.REPEAT_SHORT : SampleOpcode.REPEAT_BYTE;
    }
}
