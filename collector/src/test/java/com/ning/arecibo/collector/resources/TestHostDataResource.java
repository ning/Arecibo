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

package com.ning.arecibo.collector.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ning.arecibo.collector.MockFileBackedBuffer;
import com.ning.arecibo.collector.MockTimelineDAO;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.collector.process.EventsUtils;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.SampleOpcode;
import com.ning.arecibo.util.timeline.ScalarSample;
import com.ning.arecibo.util.timeline.TimelineHostEventAccumulator;
import com.ning.jaxrs.DateTimeParameter;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TestHostDataResource
{
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final UUID HOST_1 = UUID.randomUUID();
    private static final UUID HOST_2 = UUID.randomUUID();
    private static final UUID HOST_3 = UUID.randomUUID();
    private static final String HOST_NAME_1 = HOST_1.toString();
    private static final String HOST_NAME_2 = HOST_2.toString();
    private static final String HOST_NAME_3 = HOST_3.toString();
    private static final String EVENT_TYPE = "myType";
    private static final String ATTRIBUTE_1 = "min_heapUsed";
    private static final String ATTRIBUTE_2 = "max_heapUsed";
    private static final String SAMPLE_KIND_1 = EventsUtils.getSampleKindFromEventAttribute(EVENT_TYPE, ATTRIBUTE_1);
    private static final String SAMPLE_KIND_2 = EventsUtils.getSampleKindFromEventAttribute(EVENT_TYPE, ATTRIBUTE_2);

    private final MockTimelineDAO dao = new MockTimelineDAO();

    private TimelineEventHandler handler;
    private HostDataResource resource;
    private Integer hostId1 = null;
    private Integer hostId2 = null;
    private Integer hostId3 = null;
    private Integer sampleKindId1 = null;
    private Integer sampleKindId2 = null;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        handler = new TimelineEventHandler(config, dao, new MockFileBackedBuffer());
        resource = new HostDataResource(dao, handler);

        // Create the hosts. host1 and host2 are used in testGetHostSamplesParsing, host3 in testWriteJsonForInMemoryChunks
        hostId1 = dao.getOrAddHost(HOST_NAME_1);
        Assert.assertNotNull(hostId1);
        hostId2 = dao.getOrAddHost(HOST_NAME_2);
        Assert.assertNotNull(hostId2);
        hostId3 = dao.getOrAddHost(HOST_NAME_3);
        Assert.assertNotNull(hostId3);

        // Create the sample kinds
        sampleKindId1 = dao.getOrAddSampleKind(hostId1, SAMPLE_KIND_1);
        Assert.assertNotNull(sampleKindId1);
        sampleKindId2 = dao.getOrAddSampleKind(hostId1, SAMPLE_KIND_2);
        Assert.assertNotNull(sampleKindId2);

        // Check the sample kinds for this host
        Set<String> sampleKinds = resource.findSampleKindsForHosts(ImmutableList.<String>of(HOST_NAME_1));
        Assert.assertEquals(sampleKinds.size(), 2);
        Assert.assertTrue(sampleKinds.contains(SAMPLE_KIND_1));
        Assert.assertTrue(sampleKinds.contains(SAMPLE_KIND_2));
        sampleKinds = resource.findSampleKindsForHosts(ImmutableList.<String>of(HOST_NAME_2));
        Assert.assertEquals(sampleKinds.size(), 0);
    }

    @Test(groups = "fast")
    public void testWriteJsonForInMemoryChunks() throws Exception
    {
        final DateTime startTime = new DateTime(DateTimeZone.UTC).minusHours(2);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final JsonGenerator generator = mapper.getJsonFactory().createJsonGenerator(output);

        // Check nothing is in memory
        resource.writeJsonForInMemoryChunks(generator, mapper.writer(), ImmutableList.<Integer>of(hostId3), ImmutableList.<Integer>of(sampleKindId1, sampleKindId2), startTime, null, false);
        Assert.assertEquals(output.size(), 0);

        // The test is fast enough, the event won't be committed
        handler.handle(new MapEvent(System.currentTimeMillis(), EVENT_TYPE, UUID.randomUUID(), ImmutableMap.<String, Object>of("hostName", HOST_NAME_3, ATTRIBUTE_1, 12, ATTRIBUTE_2, 42)));
        resource.writeJsonForInMemoryChunks(generator, mapper.writer(), ImmutableList.<Integer>of(hostId3), ImmutableList.<Integer>of(sampleKindId1, sampleKindId2), startTime, null, false);
        Assert.assertTrue(output.size() > 0);

        // Check the sample kinds for this host
        final Set<String> sampleKinds = resource.findSampleKindsForHosts(ImmutableList.<String>of(HOST_NAME_3));
        Assert.assertEquals(sampleKinds.size(), 3);
        Assert.assertTrue(sampleKinds.contains(SAMPLE_KIND_1));
        Assert.assertTrue(sampleKinds.contains(SAMPLE_KIND_2));
        Assert.assertTrue(sampleKinds.contains(EventsUtils.getSampleKindFromEventAttribute(EVENT_TYPE, "hostName")));
    }

    @Test(groups = "fast")
    public void testGetHostSamplesParsing() throws Exception
    {
        final DateTime startTime = new DateTime(DateTimeZone.UTC).minusHours(2);

        List<Map<String, Object>> timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1, SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 0);

        // Send one sample first
        sendSamples(hostId1, sampleKindId1, startTime);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(SAMPLE_KIND_1), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("hostId"), hostId1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("sampleKindId"), sampleKindId1);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 0);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(SAMPLE_KIND_1, SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("hostId"), hostId1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("sampleKindId"), sampleKindId1);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 0);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 0);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1, SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 0);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("hostId"), hostId1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("sampleKindId"), sampleKindId1);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 0);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1, SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("hostId"), hostId1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("sampleKindId"), sampleKindId1);

        // Send the second sample to the same host
        sendSamples(hostId1, sampleKindId2, startTime);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(SAMPLE_KIND_1), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("hostId"), hostId1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("sampleKindId"), sampleKindId1);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("hostId"), hostId1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("sampleKindId"), sampleKindId2);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(SAMPLE_KIND_1, SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 2);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 0);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 0);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1, SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 0);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1, SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 2);

        // Send the first sample to the second host
        sendSamples(hostId2, sampleKindId1, startTime);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(SAMPLE_KIND_1), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("hostId"), hostId1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("sampleKindId"), sampleKindId1);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("hostId"), hostId1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("sampleKindId"), sampleKindId2);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(SAMPLE_KIND_1, SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 2);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("hostId"), hostId2);
        Assert.assertEquals(timlineChunkAndTimes.get(0).get("sampleKindId"), sampleKindId1);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 0);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1, SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 2);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 1);
        timlineChunkAndTimes = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(SAMPLE_KIND_1, SAMPLE_KIND_2), startTime);
        Assert.assertEquals(timlineChunkAndTimes.size(), 3);
    }

    private void sendSamples(final Integer hostId, final Integer sampleKindId, final DateTime startTime) throws IOException
    {
        final TimelineHostEventAccumulator accumulator = new TimelineHostEventAccumulator(dao, hostId, EVENT_TYPE, false);
        // 120 samples per hour
        for (int i = 0; i < 120; i++) {
            final DateTime eventDateTime = startTime.plusSeconds(i * 30);
            final Map<Integer, ScalarSample> event = createEvent(sampleKindId, eventDateTime.getMillis());
            final HostSamplesForTimestamp samples = new HostSamplesForTimestamp(hostId, EVENT_TYPE, eventDateTime, event);
            accumulator.addHostSamples(samples);
        }
        accumulator.extractAndSaveTimelineChunks();
    }

    private Map<Integer, ScalarSample> createEvent(final Integer sampleKindId, final long ts)
    {
        return ImmutableMap.<Integer, ScalarSample>of(
            sampleKindId, new ScalarSample(SampleOpcode.LONG, Long.MIN_VALUE + ts)
        );
    }

    private List<Map<String, Object>> getSamplesSinceDateTime(final List<String> hosts, final List<String> sampleKinds, final DateTime startTime) throws IOException
    {
        final StreamingOutput output = resource.getHostSamples(
            new DateTimeParameter(startTime.toString()),
            new DateTimeParameter(null),
            true,
            false,
            false,
            hosts,
            sampleKinds
        );
        return parseOutput(output);
    }

    private List<Map<String, Object>> parseOutput(final StreamingOutput output) throws IOException
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        output.write(out);
        return mapper.readValue(out.toByteArray(), new TypeReference<List<Map<String, Object>>>()
        {
        });
    }
}
