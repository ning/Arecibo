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

package com.ning.arecibo.collector.process;

import com.google.common.collect.ImmutableMap;
import com.ning.arecibo.collector.MockTimelineDAO;
import com.ning.arecibo.collector.persistent.TimelineHostEventAccumulator;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.SampleOpcode;
import com.ning.arecibo.util.timeline.ScalarSample;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.persistent.FileBackedBuffer;
import com.ning.arecibo.util.timeline.persistent.Replayer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Lightweight version of TestFileBackedBuffer
public class TestSamplesReplayer
{
    // Total space: 255 * 3 = 765 bytes
    private static final int NB_EVENTS = 3;
    // One will still be in memory after the flush
    private static final int EVENTS_ON_DISK = NB_EVENTS - 1;
    private static final int HOST_ID = 1;
    private static final int EVENT_CATEGORY_ID = 123;
    private static final File basePath = new File(System.getProperty("java.io.tmpdir"), "TestSamplesReplayer-" + System.currentTimeMillis());

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        Assert.assertTrue(basePath.mkdir());
    }

    @Test(groups = "slow")
    public void testIdentityFilter() throws Exception
    {
        // Need less than 765 + 1 (metadata) bytes
        final FileBackedBuffer fileBackedBuffer = new FileBackedBuffer(basePath.toString(), "test", 765, 1);

        // Create the host samples - this will take 255 bytes
        final Map<Integer, ScalarSample> eventMap = new HashMap<Integer, ScalarSample>();
        eventMap.putAll(ImmutableMap.<Integer, ScalarSample>of(
            1, new ScalarSample(SampleOpcode.BYTE, (byte) 0),
            2, new ScalarSample(SampleOpcode.SHORT, (short) 1),
            3, new ScalarSample(SampleOpcode.INT, 1000),
            4, new ScalarSample(SampleOpcode.LONG, 12345678901L),
            5, new ScalarSample(SampleOpcode.DOUBLE, Double.MAX_VALUE)
        ));
        eventMap.putAll(ImmutableMap.<Integer, ScalarSample>of(
            6, new ScalarSample(SampleOpcode.FLOAT, Float.NEGATIVE_INFINITY),
            7, new ScalarSample(SampleOpcode.STRING, "pwet")
        ));
        final DateTime firstTime = new DateTime(DateTimeZone.UTC).minusSeconds(NB_EVENTS * 30);

        // Write the samples to disk
        for (int i = 0; i < NB_EVENTS; i++) {
            final HostSamplesForTimestamp samples = new HostSamplesForTimestamp(HOST_ID, "something", firstTime.plusSeconds(30 * i), eventMap);
            fileBackedBuffer.append(samples);
        }

        // Try the replayer
        final Replayer replayer = new Replayer(new File(basePath.toString()).getAbsolutePath());
        final List<HostSamplesForTimestamp> hostSamples = replayer.readAll();
        Assert.assertEquals(hostSamples.size(), EVENTS_ON_DISK);

        // Try to encode them again
        final MockTimelineDAO dao = new MockTimelineDAO();
        final TimelineHostEventAccumulator accumulator = new TimelineHostEventAccumulator(dao, HOST_ID, EVENT_CATEGORY_ID, hostSamples.get(0).getTimestamp());
        for (final HostSamplesForTimestamp samplesFound : hostSamples) {
            accumulator.addHostSamples(samplesFound);
        }
        Assert.assertTrue(accumulator.checkSampleCounts(EVENTS_ON_DISK));

        // This will check the SampleCode can encode value correctly
        accumulator.extractAndQueueTimelineChunks();
        Assert.assertEquals(dao.getTimelineChunks().keySet().size(), 7);
        for (final TimelineChunk chunk : dao.getTimelineChunks().values()) {
            Assert.assertEquals(chunk.getHostId(), HOST_ID);
            Assert.assertEquals(chunk.getSampleCount(), EVENTS_ON_DISK);
        }
    }
}
