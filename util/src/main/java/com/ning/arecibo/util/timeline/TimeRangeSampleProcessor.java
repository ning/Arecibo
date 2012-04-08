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

import org.joda.time.DateTime;

public abstract class TimeRangeSampleProcessor implements SampleProcessor {
    private final DateTime startTime;  // Inclusive
    private final DateTime endTime;    // Inclusive

    public TimeRangeSampleProcessor(DateTime startTime, DateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Process sampleCount sequential samples with identical values.  sampleCount will usually be 1,
     * but may be larger than 1.  Implementors may just loop processing identical values, but some
     * implementations may optimize adding a bunch of repeated values
     *
     * @param timeCursor   a TimeCursor instance, which supplies successive int UNIX times
     * @param sampleCount  the count of sequential, identical values
     * @param opcode       the opcode of the sample value, which may not be a REPEAT opcode
     * @param value        the value of this kind of sample over the sampleCount samples
     */
    @Override
    public void processSamples(final TimeCursor timeCursor, final int sampleCount, final SampleOpcode opcode, final Object value)
    {
        final int unixStartTime = startTime == null ? Integer.MIN_VALUE : DateTimeUtils.unixSeconds(startTime);
        final int unixEndTime = endTime == null ? Integer.MAX_VALUE : DateTimeUtils.unixSeconds(endTime);
        for (int i = 0; i < sampleCount; i++) {
            // Check if the sample is in the right time range
            final int unixSampleTime = timeCursor.getNextTime();
            if (unixSampleTime >= unixStartTime && unixSampleTime <= unixEndTime) {
                processOneSample(DateTimeUtils.dateTimeFromUnixSeconds(unixSampleTime), opcode, value);
            }
        }
    }

    public abstract void processOneSample(final DateTime time, final SampleOpcode opcode, final Object value);
}
