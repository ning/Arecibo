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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class TestJacksonUpgrade
{
    @Test(groups = "fast")
    public void testVerySimple() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper().registerModule(new JodaModule());
        final Map<String, Object> map = ImmutableMap.<String, Object>of("A", new DateTime(DateTimeZone.UTC), "B", 0);

        final String json = mapper.writeValueAsString(map);
        final Map<String, Object> deserialized = mapper.readValue(json, new TypeReference<Map<String, Object>>()
        {
        });
        Assert.assertEquals(deserialized.get("B"), 0);
    }

    @Test(groups = "fast")
    public void testScalarSample() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper().registerModule(new JodaModule());
        final ScalarSample<Byte> sample = new ScalarSample<Byte>(SampleOpcode.BYTE, (byte) 0);

        // Try plain json
        final String json = mapper.writeValueAsString(sample);
        final ScalarSample deserialized = mapper.readValue(json, new TypeReference<ScalarSample>()
        {
        });
        Assert.assertEquals(deserialized.getOpcode(), sample.getOpcode());
        Assert.assertEquals(deserialized.getSampleValue(), sample.getSampleValue());

        // Try smile
        final SmileFactory smileFactory = new SmileFactory();
        smileFactory.setCodec(mapper);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final SmileGenerator generator = smileFactory.createJsonGenerator(out);
        generator.writeObject(sample);
        generator.flush();
        final ScalarSample<Byte> deserializedFromSmile = smileFactory.createJsonParser(out.toByteArray()).readValueAs(new TypeReference<ScalarSample<Byte>>()
        {
        });
        Assert.assertEquals(deserializedFromSmile.getOpcode(), sample.getOpcode());
        Assert.assertEquals(deserializedFromSmile.getSampleValue(), sample.getSampleValue());
    }

    @Test(groups = "fast")
    public void testScalarSamples() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper().registerModule(new JodaModule());
        final ScalarSample<Byte> sample = new ScalarSample<Byte>(SampleOpcode.BYTE, (byte) 0);
        final Map<Integer, ScalarSample> samples = ImmutableMap.<Integer, ScalarSample>of(0, sample);
        final int hostId = 1;
        final String category = "a";
        final DateTime timestamp = new DateTime(DateTimeZone.UTC);
        final HostSamplesForTimestamp samplesForTimestamp = new HostSamplesForTimestamp(hostId, category, timestamp, samples);

        // Try plain json
        final String json = mapper.writeValueAsString(samplesForTimestamp);
        // json is {"H":1,"V":"a","T":1332980011441,"S":{"1":{"O":1,"K":"java.lang.Byte","V":0}}}
        final HostSamplesForTimestamp deserialized = mapper.readValue(json, HostSamplesForTimestamp.class);
        Assert.assertEquals(deserialized.getHostId(), hostId);
        Assert.assertEquals(deserialized.getCategory(), category);
        Assert.assertEquals(deserialized.getTimestamp(), timestamp);
        Assert.assertEquals(deserialized.getSamples().size(), 1);
        Assert.assertEquals(deserialized.getSamples().get(0).getOpcode(), sample.getOpcode());
        Assert.assertEquals(deserialized.getSamples().get(0).getSampleValue(), sample.getSampleValue());

        // Try smile
        final SmileFactory smileFactory = new SmileFactory();
        smileFactory.setCodec(mapper);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final SmileGenerator generator = smileFactory.createJsonGenerator(out);
        generator.writeObject(samplesForTimestamp);
        generator.flush();
        final HostSamplesForTimestamp deserializedFromSmile = smileFactory.createJsonParser(out.toByteArray()).readValueAs(HostSamplesForTimestamp.class);
        Assert.assertEquals(deserializedFromSmile.getHostId(), hostId);
        Assert.assertEquals(deserializedFromSmile.getCategory(), category);
        Assert.assertEquals(deserializedFromSmile.getTimestamp(), timestamp);
        Assert.assertEquals(deserializedFromSmile.getSamples().size(), 1);
        Assert.assertEquals(deserializedFromSmile.getSamples().get(0).getOpcode(), sample.getOpcode());
        Assert.assertEquals(deserializedFromSmile.getSamples().get(0).getSampleValue(), sample.getSampleValue());
    }

    @Test(groups = "fast")
    public void testJodaDateTime() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper().registerModule(new JodaModule());
        final int hostId = 1;
        final String category = "a";
        final DateTime timestamp = new DateTime(DateTimeZone.UTC);
        final HostSamplesForTimestamp samples = new HostSamplesForTimestamp(hostId, category, timestamp);

        // Try plain json
        final String json = mapper.writeValueAsString(samples);
        final HostSamplesForTimestamp deserialized = mapper.readValue(json, HostSamplesForTimestamp.class);

        Assert.assertEquals(deserialized.getTimestamp(), timestamp);
        Assert.assertEquals(deserialized.getHostId(), hostId);
        Assert.assertEquals(deserialized.getCategory(), category);

        // Try smile
        final SmileFactory smileFactory = new SmileFactory();
        smileFactory.setCodec(mapper);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final SmileGenerator generator = smileFactory.createJsonGenerator(out);
        generator.writeObject(samples);
        generator.flush();
        final HostSamplesForTimestamp deserializedFromSmile = smileFactory.createJsonParser(out.toByteArray()).readValueAs(HostSamplesForTimestamp.class);

        Assert.assertEquals(deserializedFromSmile.getTimestamp(), timestamp);
        Assert.assertEquals(deserializedFromSmile.getHostId(), hostId);
        Assert.assertEquals(deserializedFromSmile.getCategory(), category);
    }
}
