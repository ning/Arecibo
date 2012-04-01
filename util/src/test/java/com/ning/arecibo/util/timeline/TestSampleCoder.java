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

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class TestSampleCoder
{
    @Test(groups = "fast")
    public void testScan() throws Exception
    {
        final DateTime startTime = new DateTime(DateTimeZone.UTC);
        final DateTime endTime = startTime.plusSeconds(5);
        final TimelineTimes times = new TimelineTimes(-1, -1, 123, startTime, endTime,
            ImmutableList.<DateTime>of(startTime.plusSeconds(1), startTime.plusSeconds(2), startTime.plusSeconds(3), startTime.plusSeconds(4)));

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        final ScalarSample<Short> sample = new ScalarSample<Short>(SampleOpcode.SHORT, (short) 4);
        SampleCoder.encodeSample(dataOutputStream, sample);
        SampleCoder.encodeSample(dataOutputStream, new RepeatSample<Short>((byte) 3, sample));
        dataOutputStream.close();

        SampleCoder.scan(outputStream.toByteArray(), times, new TimeRangeSampleProcessor(startTime, endTime)
        {
            @Override
            public void processOneSample(final DateTime time, final SampleOpcode opcode, final Object value)
            {
                Assert.assertTrue(time.isAfter(startTime));
                Assert.assertTrue(time.isBefore(endTime));
                Assert.assertEquals(Short.valueOf(value.toString()), sample.getSampleValue());
            }
        });
    }

    @Test(groups = "fast")
    public void testTimeRangeSampleProcessor() throws Exception
    {
        final DateTime startTime = new DateTime("2012-03-23T17:35:11.707Z");
        final DateTime endTime = new DateTime("2012-03-23T17:35:17.924Z");
        final int sampleCount = 2;

        final TimelineTimes times = new TimelineTimes(0, -1, 123, startTime, endTime, ImmutableList.<DateTime>of(startTime, endTime));
        final TimeCursor cursor = times.getTimeCursor();
        Assert.assertEquals(cursor.getNextTime(), TimelineTimes.unixSeconds(startTime));
        Assert.assertEquals(cursor.getNextTime(), TimelineTimes.unixSeconds(endTime));

        // 2 x the value 12: REPEAT, SHORT, 2, SHORT, 12 (2 bytes)
        final byte[] samples = new byte[]{127, 2, 2, 0, 12};

        final AtomicInteger samplesCount = new AtomicInteger(0);
        SampleCoder.scan(samples, times, new TimeRangeSampleProcessor(startTime, endTime)
        {
            @Override
            public void processOneSample(final DateTime time, final SampleOpcode opcode, final Object value)
            {
                if (samplesCount.get() == 0) {
                    Assert.assertEquals(TimelineTimes.unixSeconds(time), TimelineTimes.unixSeconds(startTime));
                }
                else {
                    Assert.assertEquals(TimelineTimes.unixSeconds(time), TimelineTimes.unixSeconds(endTime));
                }
                samplesCount.incrementAndGet();
            }
        });
        Assert.assertEquals(samplesCount.get(), sampleCount);
    }
}
