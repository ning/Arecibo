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

package com.ning.arecibo.util.timeline.persistent;

import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.SampleOpcode;
import com.ning.arecibo.util.timeline.ScalarSample;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.util.membuf.MemBuffersForBytes;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSimpleReplayer
{
    private static final File basePath = new File(System.getProperty("java.io.tmpdir"), "TestSimpleReplayer-" + System.currentTimeMillis());

    @Test(groups = "slow")
    public void testRoundTrip() throws Exception
    {
        Assert.assertTrue(basePath.mkdir());

        // Setup Jackson
        final ObjectMapper mapper = new ObjectMapper().registerModule(new JodaModule());
        final SmileFactory smileFactory = new SmileFactory();
        smileFactory.setCodec(mapper);

        // Setup some sample data
        final int hostId = 1;
        final String category = "a";
        final DateTime timestamp = new DateTime(DateTimeZone.UTC);
        final HostSamplesForTimestamp samples = new HostSamplesForTimestamp(hostId, category, timestamp, createSamples());

        // Setup the StreamyBytesPersistentOutputStream
        final MemBuffersForBytes bufs = new MemBuffersForBytes(1024, 1, 1);
        final StreamyBytesPersistentOutputStream outputStream = new StreamyBytesPersistentOutputStream(basePath.getAbsolutePath(), "something", bufs.createStreamyBuffer(1, 1));
        outputStream.reset();

        // Send some data
        final SmileGenerator generator = smileFactory.createJsonGenerator(outputStream);
        generator.writeObject(samples);
        generator.flush();
        outputStream.flushUnderlyingBufferAndReset();
        Assert.assertEquals(outputStream.getCreatedFiles().size(), 1);

        // Try to replay
        final Replayer replayer = new Replayer(basePath.getAbsolutePath());
        final List<HostSamplesForTimestamp> samplesFound = replayer.readAll();
        Assert.assertEquals(samplesFound.size(), 1);
        Assert.assertEquals(samplesFound.get(0).getHostId(), samples.getHostId());
        Assert.assertEquals(samplesFound.get(0).getCategory(), samples.getCategory());
        Assert.assertEquals(samplesFound.get(0).getTimestamp(), samples.getTimestamp());
    }

    private Map<Integer, ScalarSample> createSamples()
    {
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

        return eventMap;
    }
}
