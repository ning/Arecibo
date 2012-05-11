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

package com.ning.arecibo.collector.persistent;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.ning.arecibo.collector.MockFileBackedBuffer;
import com.ning.arecibo.collector.MockTimelineDAO;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.persistent.TimelineDAO;
import com.ning.arecibo.util.timeline.samples.ScalarSample;
import com.ning.arecibo.util.timeline.times.TimelineCoder;
import com.ning.arecibo.util.timeline.times.TimelineCoderImpl;

public class TestTimelineEventHandler
{
    private static final File basePath = new File(System.getProperty("java.io.tmpdir"), "TestTimelineEventHandler-" + System.currentTimeMillis());
    private static final String EVENT_TYPE = "eventType";
    private static final TimelineCoder timelineCoder = new TimelineCoderImpl();

    private final TimelineDAO dao = new MockTimelineDAO();

    @Test(groups = "fast")
    public void testDownsizingValues() throws Exception
    {
        Assert.assertTrue(basePath.mkdir());
        System.setProperty("arecibo.collector.timelines.spoolDir", basePath.getAbsolutePath());
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        final int eventTypeId = dao.getOrAddEventCategory(EVENT_TYPE);
        final int int2shortId = dao.getOrAddSampleKind(1, eventTypeId, "int2short");
        final int long2intId = dao.getOrAddSampleKind(1, eventTypeId, "long2int");
        final int long2shortId = dao.getOrAddSampleKind(1, eventTypeId, "long2short");
        final int int2intId = dao.getOrAddSampleKind(1, eventTypeId, "int2int");
        final int long2longId = dao.getOrAddSampleKind(1, eventTypeId, "long2long");
        final int hostId = 1;
        final TimelineEventHandler handler = new TimelineEventHandler(config, dao, timelineCoder, new BackgroundDBChunkWriter(dao, config, true), new MockFileBackedBuffer());

        // Test downsizing of values
        final Map<String, Object> event = ImmutableMap.<String, Object>of(
            "int2short", new Integer(1),
            "long2int", new Long(Integer.MAX_VALUE),
            "long2short", new Long(2),
            "int2int", Integer.MAX_VALUE,
            "long2long", Long.MAX_VALUE);
        final Map<Integer, ScalarSample> output = convertEventToSamples(handler, event, hostId, EVENT_TYPE);

        Assert.assertEquals(output.get(int2shortId).getSampleValue(), (short) 1);
        Assert.assertEquals(output.get(int2shortId).getSampleValue().getClass(), Short.class);
        Assert.assertEquals(output.get(long2intId).getSampleValue(), Integer.MAX_VALUE);
        Assert.assertEquals(output.get(long2intId).getSampleValue().getClass(), Integer.class);
        Assert.assertEquals(output.get(long2shortId).getSampleValue(), (short) 2);
        Assert.assertEquals(output.get(long2shortId).getSampleValue().getClass(), Short.class);
        Assert.assertEquals(output.get(int2intId).getSampleValue(), Integer.MAX_VALUE);
        Assert.assertEquals(output.get(int2intId).getSampleValue().getClass(), Integer.class);
        Assert.assertEquals(output.get(long2longId).getSampleValue(), Long.MAX_VALUE);
        Assert.assertEquals(output.get(long2longId).getSampleValue().getClass(), Long.class);
    }

    private Map<Integer, ScalarSample> convertEventToSamples(final TimelineEventHandler handler, final Map<String, Object> event, final int hostId, final String eventType)
    {
        final Map<Integer, ScalarSample> output = new HashMap<Integer, ScalarSample>();
        handler.convertSamplesToScalarSamples(1, eventType, event, output);
        return output;
    }

    private void processOneEvent(final TimelineEventHandler handler, final int hostId, final String eventType, final String sampleKind, final DateTime timestamp) throws Exception
    {
        final Map<String, Object> rawEvent = ImmutableMap.<String, Object>of(sampleKind, new Integer(1));
        final Map<Integer, ScalarSample> convertedEvent = convertEventToSamples(handler, rawEvent, hostId, eventType);
        handler.processSamples(new HostSamplesForTimestamp(hostId, eventType, timestamp, convertedEvent));
    }

    private void sleep(int millis)
    {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test(groups = "fast")
    public void testPurgeAccumulators() throws Exception
    {
        System.setProperty("arecibo.collector.timelines.spoolDir", basePath.getAbsolutePath());
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        final TimelineEventHandler handler = new TimelineEventHandler(config, dao, timelineCoder, new BackgroundDBChunkWriter(dao, config, true), new MockFileBackedBuffer());
        Assert.assertEquals(handler.getAccumulators().size(), 0);
        processOneEvent(handler, 1, "eventType1", "sampleKind1", new DateTime());
        sleep(20);
        final DateTime purgeBeforeTime = new DateTime();
        sleep(20);
        processOneEvent(handler, 1, "eventType2", "sampleKind2", new DateTime());
        Assert.assertEquals(handler.getAccumulators().size(), 2);
        handler.purgeOldHostsAndAccumulators(purgeBeforeTime);
        final Collection<TimelineHostEventAccumulator> accumulators = handler.getAccumulators();
        Assert.assertEquals(accumulators.size(), 1);
    }
}
