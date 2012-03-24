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

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.node.TextNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestTimelineChunkAndTimesToJson
{
    private static final ObjectMapper mapper = new ObjectMapper().configure(SerializationConfig.Feature.DEFAULT_VIEW_INCLUSION, false);

    private static final long SAMPLE_TIMELINE_ID = 1242L;
    private static final int HOST_ID = 1422;
    private static final String EVENT_CATEGORY = "JVM";
    private static final int SAMPLE_KIND_ID = 1224;
    private static final int TIMELINE_TIMES_ID = 4221;
    private static final int SAMPLE_COUNT = 2142;
    private static final long TIMELINE_INTERVAL_ID = 1337L;
    private static final String HOST_NAME = UUID.randomUUID().toString();
    private static final String SAMPLE_KIND = UUID.randomUUID().toString();
    private static final DateTime END_TIME = new DateTime(DateTimeZone.UTC);
    private static final DateTime START_TIME = END_TIME.minusMinutes(SAMPLE_COUNT);

    private byte[] samples;
    private TimelineChunk chunk;
    private TimelineTimes times;
    private TimelineChunkAndTimes chunkAndTimes;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        final List<DateTime> dateTimes = new ArrayList<DateTime>();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final DataOutputStream output = new DataOutputStream(out);
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            SampleCoder.encodeSample(output, new ScalarSample<Long>(SampleOpcode.LONG, 10L));
            dateTimes.add(START_TIME.plusMinutes(i));
        }
        output.flush();
        output.close();
        samples = out.toByteArray();

        chunk = new TimelineChunk(SAMPLE_TIMELINE_ID, HOST_ID, SAMPLE_KIND_ID, TIMELINE_TIMES_ID, START_TIME, samples, SAMPLE_COUNT);
        times = new TimelineTimes(TIMELINE_INTERVAL_ID, HOST_ID, EVENT_CATEGORY, START_TIME, START_TIME, dateTimes);
        chunkAndTimes = new TimelineChunkAndTimes(HOST_NAME, SAMPLE_KIND, chunk, times);
    }

    @Test(groups = "fast")
    public void testTimelineChunkCompactMapping() throws Exception
    {
        final String chunkToString = mapper.writerWithView(TimelineChunksAndTimesViews.Compact.class).writeValueAsString(chunk);
        final Map chunkFromString = mapper.readValue(chunkToString, Map.class);
        Assert.assertEquals(chunkFromString.keySet().size(), 3);
        Assert.assertEquals(new TextNode((String) chunkFromString.get("samples")).getBinaryValue(), samples);
        Assert.assertEquals(chunkFromString.get("sampleCount"), SAMPLE_COUNT);
        Assert.assertEquals(chunkFromString.get("startTime"), START_TIME.getMillis());
    }

    @Test(groups = "fast")
    public void testTimelineTimesCompactMapping() throws Exception
    {
        final String timesToString = mapper.writerWithView(TimelineChunksAndTimesViews.Compact.class).writeValueAsString(times);
        final Map timesFromString = mapper.readValue(timesToString, Map.class);
        Assert.assertEquals(timesFromString.keySet().size(), 2);
        Assert.assertEquals(timesFromString.get("timeSampleCount"), SAMPLE_COUNT);
        Assert.assertNotNull(timesFromString.get("compressedTimes"));
    }

    @Test(groups = "fast")
    public void testTimelineChunkAndTimesCompactMapping() throws Exception
    {
        final String chunkAndTimesToString = mapper.writerWithView(TimelineChunksAndTimesViews.Compact.class).writeValueAsString(chunkAndTimes);
        final Map chunkAndTimesFromString = mapper.readValue(chunkAndTimesToString, Map.class);
        Assert.assertEquals(chunkAndTimesFromString.keySet().size(), 7);
        Assert.assertEquals(chunkAndTimesFromString.get("hostName"), HOST_NAME);
        Assert.assertEquals(chunkAndTimesFromString.get("sampleKind"), SAMPLE_KIND);
        Assert.assertEquals(new TextNode((String) chunkAndTimesFromString.get("samples")).getBinaryValue(), samples);
        Assert.assertEquals(chunkAndTimesFromString.get("sampleCount"), SAMPLE_COUNT);
        Assert.assertEquals(chunkAndTimesFromString.get("startTime"), START_TIME.getMillis());
        Assert.assertEquals(chunkAndTimesFromString.get("timeSampleCount"), SAMPLE_COUNT);
        Assert.assertNotNull(chunkAndTimesFromString.get("compressedTimes"));
    }

    @Test(groups = "fast")
    public void testTimelineChunkAndTimesLooseMapping() throws Exception
    {
        final String chunkAndTimesToString = mapper.writerWithView(TimelineChunksAndTimesViews.Loose.class).writeValueAsString(chunkAndTimes);
        final Map chunkAndTimesFromString = mapper.readValue(chunkAndTimesToString, Map.class);
        Assert.assertEquals(chunkAndTimesFromString.keySet().size(), 3);
        Assert.assertEquals(chunkAndTimesFromString.get("hostName"), HOST_NAME);
        Assert.assertEquals(chunkAndTimesFromString.get("sampleKind"), SAMPLE_KIND);
        Assert.assertNotNull(chunkAndTimesFromString.get("samplesAsCSV"));
    }
}
