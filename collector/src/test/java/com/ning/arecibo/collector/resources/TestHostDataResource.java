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

import com.ning.arecibo.collector.MockFileBackedBuffer;
import com.ning.arecibo.collector.MockTimelineDAO;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.collector.persistent.BackgroundDBChunkWriter;
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.collector.persistent.TimelineHostEventAccumulator;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.util.timeline.CategoryIdAndSampleKind;
import com.ning.arecibo.util.timeline.DecimatingSampleFilter;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.SampleOpcode;
import com.ning.arecibo.util.timeline.ScalarSample;
import com.ning.jaxrs.DateTimeParameter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
    private static final String SAMPLE_KIND_1 = "min_heapUsed";
    private static final String SAMPLE_KIND_2 = "max_heapUsed";
    private static final String CATEGORY_AND_SAMPLE_KIND_1 = EVENT_TYPE + "," + SAMPLE_KIND_1;
    private static final String CATEGORY_AND_SAMPLE_KIND_2 = EVENT_TYPE + "," + SAMPLE_KIND_2;

    private MockTimelineDAO dao = null;
    private TimelineEventHandler handler;
    private HostDataResource resource;
    private Integer hostId1 = null;
    private Integer hostId2 = null;
    private Integer hostId3 = null;
    private Integer eventTypeId = 0;
    private Integer sampleKindId1 = null;
    private Integer sampleKindId2 = null;
    private CollectorConfig config = null;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        dao = new MockTimelineDAO();
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        handler = new TimelineEventHandler(config, dao, new BackgroundDBChunkWriter(dao, config, true), new MockFileBackedBuffer());
        resource = new HostDataResource(dao, handler);

        // Create the hosts. host1 and host2 are used in testGetHostSamplesParsing, host3 in testWriteJsonForInMemoryChunks
        hostId1 = dao.getOrAddHost(HOST_NAME_1);
        Assert.assertNotNull(hostId1);
        hostId2 = dao.getOrAddHost(HOST_NAME_2);
        Assert.assertNotNull(hostId2);
        hostId3 = dao.getOrAddHost(HOST_NAME_3);
        Assert.assertNotNull(hostId3);

        // Create the sample kinds
        eventTypeId = dao.getOrAddEventCategory(EVENT_TYPE);
        sampleKindId1 = dao.getOrAddSampleKind(hostId1, eventTypeId, SAMPLE_KIND_1);
        Assert.assertNotNull(sampleKindId1);
        sampleKindId2 = dao.getOrAddSampleKind(hostId1, eventTypeId, SAMPLE_KIND_2);
        Assert.assertNotNull(sampleKindId2);

        // Check the sample kinds for this host
        Set<Integer> sampleKindIds = resource.findSampleKindIdsForHosts(ImmutableList.<String>of(HOST_NAME_1));
        Assert.assertEquals(sampleKindIds.size(), 2);
        Assert.assertTrue(sampleKindIds.contains(sampleKindId1));
        Assert.assertTrue(sampleKindIds.contains(sampleKindId2));
        sampleKindIds = resource.findSampleKindIdsForHosts(ImmutableList.<String>of(HOST_NAME_2));
        Assert.assertEquals(sampleKindIds.size(), 0);
    }

    @Test(groups = "fast")
    public void testWriteJsonForInMemoryChunks() throws Exception
    {
        final DateTime startTime = new DateTime(DateTimeZone.UTC).minusHours(2);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final JsonGenerator generator = mapper.getJsonFactory().createJsonGenerator(output);

        // Check nothing is in memory
        resource.writeJsonForInMemoryChunks(generator, mapper.writer(), null, ImmutableList.<Integer>of(hostId3), ImmutableList.<Integer>of(sampleKindId1, sampleKindId2), startTime, null, false);
        Assert.assertEquals(output.size(), 0);

        // The test is fast enough, the event won't be committed
        handler.handle(new MapEvent(System.currentTimeMillis(), EVENT_TYPE, UUID.randomUUID(), ImmutableMap.<String, Object>of("hostName", HOST_NAME_3, SAMPLE_KIND_1, 12, SAMPLE_KIND_2, 42)));
        final Map<Integer, Map<Integer, DecimatingSampleFilter>> filters = resource.createDecimatingSampleFilters(ImmutableList.<Integer>of(hostId3), ImmutableList.<Integer>of(sampleKindId1, sampleKindId2), startTime, null, null);
        resource.writeJsonForInMemoryChunks(generator, mapper.writer(), filters, ImmutableList.<Integer>of(hostId3), ImmutableList.<Integer>of(sampleKindId1, sampleKindId2), startTime, null, false);
        Assert.assertTrue(output.size() > 0);

        // Check the sample kinds for this host
        final Set<CategoryIdAndSampleKind> categoryIdsAndSampleKinds = resource.findCategoryIdsAndSampleKindsForHosts(ImmutableList.<String>of(HOST_NAME_3));
        Assert.assertEquals(categoryIdsAndSampleKinds.size(), 3);
        final List<String> sampleKinds = CategoryIdAndSampleKind.extractSampleKinds(categoryIdsAndSampleKinds);
        Assert.assertTrue(sampleKinds.contains(SAMPLE_KIND_1));
        Assert.assertTrue(sampleKinds.contains(SAMPLE_KIND_2));
        Assert.assertTrue(sampleKinds.contains("hostName"));
    }

    @Test(groups = "fast")
    public void testGetHostSamplesParsing() throws Exception
    {
        final DateTime startTime = new DateTime(DateTimeZone.UTC).minusHours(2);

        List<Map<String, Object>> samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1, CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 0);

        // Send one sample first
        sendSamples(hostId1, sampleKindId1, startTime);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("hostName"), HOST_NAME_1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("eventCategory"), EVENT_TYPE);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("sampleKind"), SAMPLE_KIND_1);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 0);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1, CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("hostName"), HOST_NAME_1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("eventCategory"), EVENT_TYPE);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("sampleKind"), SAMPLE_KIND_1);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 0);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 0);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1, CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 0);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("hostName"), HOST_NAME_1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("eventCategory"), EVENT_TYPE);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("sampleKind"), SAMPLE_KIND_1);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 0);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1, CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("hostName"), HOST_NAME_1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("eventCategory"), EVENT_TYPE);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("sampleKind"), SAMPLE_KIND_1);

        // Send the second sample to the same host
        final DateTime nextStartTime = startTime.plusSeconds(121 * 30);
        sendSamples(hostId1, sampleKindId2, nextStartTime);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1), nextStartTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("hostName"), HOST_NAME_1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("eventCategory"), EVENT_TYPE);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("sampleKind"), SAMPLE_KIND_1);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("hostName"), HOST_NAME_1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("eventCategory"), EVENT_TYPE);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("sampleKind"), SAMPLE_KIND_2);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1, CATEGORY_AND_SAMPLE_KIND_2), nextStartTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 2);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 0);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 0);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1, CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 0);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1), nextStartTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1, CATEGORY_AND_SAMPLE_KIND_2), nextStartTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 2);

        // Send the first sample to the second host
        sendSamples(hostId2, sampleKindId1, nextStartTime);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1), nextStartTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("hostName"), HOST_NAME_1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("eventCategory"), EVENT_TYPE);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("sampleKind"), SAMPLE_KIND_1);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("hostName"), HOST_NAME_1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("eventCategory"), EVENT_TYPE);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("sampleKind"), SAMPLE_KIND_2);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1, CATEGORY_AND_SAMPLE_KIND_2), nextStartTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 2);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("hostName"), HOST_NAME_2);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("eventCategory"), EVENT_TYPE);
        Assert.assertEquals(samplesForSampleKindAndHost.get(0).get("sampleKind"), SAMPLE_KIND_1);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 0);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1, CATEGORY_AND_SAMPLE_KIND_2), nextStartTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1), nextStartTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 2);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_2), startTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 1);
        samplesForSampleKindAndHost = getSamplesSinceDateTime(ImmutableList.<String>of(HOST_NAME_1, HOST_NAME_2), ImmutableList.<String>of(CATEGORY_AND_SAMPLE_KIND_1, CATEGORY_AND_SAMPLE_KIND_2), nextStartTime);
        Assert.assertEquals(samplesForSampleKindAndHost.size(), 3);
    }

    private void sendSamples(final Integer hostId, final Integer sampleKindId, final DateTime startTime) throws IOException
    {
        final TimelineHostEventAccumulator accumulator = handler.getOrAddHostEventAccumulator(hostId, eventTypeId, startTime, Integer.MAX_VALUE);
        // 120 samples per hour
        for (int i = 0; i < 120; i++) {
            final DateTime eventDateTime = startTime.plusSeconds(i * 30);
            final Map<Integer, ScalarSample> event = createEvent(sampleKindId, eventDateTime.getMillis());
            final HostSamplesForTimestamp samples = new HostSamplesForTimestamp(hostId, EVENT_TYPE, eventDateTime, event);
            accumulator.addHostSamples(samples);
        }
        accumulator.extractAndQueueTimelineChunks();
    }

    private Map<Integer, ScalarSample> createEvent(final Integer sampleKindId, final long ts)
    {
        return ImmutableMap.<Integer, ScalarSample>of(sampleKindId, new ScalarSample(SampleOpcode.LONG, Long.MIN_VALUE + ts));
    }

    private List<Map<String, Object>> getSamplesSinceDateTime(final List<String> hosts, final List<String> categoriesAndSampleKinds, final DateTime startTime) throws IOException
    {
        final StreamingOutput output = resource.getHostSamples(
                new DateTimeParameter(startTime.toString()),
                new DateTimeParameter(null),
                true,
                false,
                false,
                null,
                hosts,
                categoriesAndSampleKinds
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
