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
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TestTimelineTimes
{
    @Test(groups = "fast")
    public void testName() throws Exception
    {
        final int timelineTimesId = 12;
        final int hostId = 42;
        final DateTime startTime = new DateTime("2009-01-16T21:23:58.316Z", DateTimeZone.UTC);
        final int sampleCount = 3;
        final List<DateTime> times = new ArrayList<DateTime>();

        for (int i = 0; i < sampleCount; i++) {
            times.add(startTime.plusSeconds(1 + i));
        }

        final TimelineTimes timelineTimes = new TimelineTimes(timelineTimesId, hostId, startTime, times.get(times.size() - 1), times);
        TimeCursor cursor = new TimeCursor(timelineTimes);
        for (int i = 0; i < sampleCount; i++) {
            Assert.assertEquals(cursor.getNextTime(), TimelineTimes.unixSeconds(times.get(i)));
        }

        cursor = new TimeCursor(timelineTimes);
        for (int i = 0; i < sampleCount; i++) {
            Assert.assertEquals(cursor.getNextTime(), TimelineTimes.unixSeconds(times.get(i)));
        }
    }
}
