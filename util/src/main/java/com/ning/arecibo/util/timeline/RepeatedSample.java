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

public class RepeatedSample<T> extends SampleBase {
    public static final int MAX_REPEAT_COUNT = 0x7F; // The maximum byte value

    private final ScalarSample<T> sample;

    private byte repeatCount;

    public RepeatedSample(byte repeatCount, ScalarSample<T> sample) {
        super(SampleOpcode.REPEAT);
        this.repeatCount = repeatCount;
        this.sample = sample;

    }

    public byte getRepeatCount() {
        return repeatCount;
    }

    public void incrementRepeatCount() {
        repeatCount++;
    }

    public ScalarSample<T> getSample() {
        return sample;
    }
}
