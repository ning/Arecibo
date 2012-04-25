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

package com.ning.arecibo.dashboard.config;

import com.ning.arecibo.util.timeline.CategoryAndSampleKinds;

import com.google.common.collect.ImmutableList;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestSuperGroup
{
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test(groups = "fast")
    public void testJsonSerialization() throws Exception
    {
        final CategoryAndSampleKinds kind = new CategoryAndSampleKinds("JVMMemory", ImmutableList.<String>of("heapUsed", "nonHeapUsed"));

        final String groupName = "JVM";
        final SuperGroup group = new SuperGroup(groupName, ImmutableList.<CategoryAndSampleKinds>of(kind));
        Assert.assertEquals(group.getName(), groupName);
        Assert.assertEquals(group.getKinds().size(), 1);

        final String json = mapper.writeValueAsString(group);
        Assert.assertEquals(json, "{\"name\":\"JVM\",\"kinds\":[{\"eventCategory\":\"JVMMemory\",\"sampleKinds\":[\"nonHeapUsed\",\"heapUsed\"]}]}");

        final SuperGroup parsedGroup = mapper.readValue(json.getBytes(), SuperGroup.class);
        Assert.assertEquals(parsedGroup, group);
        Assert.assertEquals(parsedGroup.getName(), groupName);
        Assert.assertEquals(parsedGroup.getKinds().size(), 1);
    }

    @Test(groups = "fast")
    public void testAsMetaCategoryAndSampleKinds() throws Exception
    {
        final CategoryAndSampleKinds jvmKinds = new CategoryAndSampleKinds("JVMMemory", ImmutableList.<String>of("heapUsed", "nonHeapUsed"));
        final CategoryAndSampleKinds cmsKinds = new CategoryAndSampleKinds("CMSOldGen", ImmutableList.<String>of("memoryPoolUsed", "memoryPoolMax"));

        final String groupName = "Memory";
        final SuperGroup group = new SuperGroup(groupName, ImmutableList.<CategoryAndSampleKinds>of(jvmKinds, cmsKinds));
        final CategoryAndSampleKinds meta = group.asMetaCategoryAndSampleKinds();

        Assert.assertEquals(meta.getEventCategory(), groupName);
        Assert.assertTrue(meta.getSampleKinds().contains("JVMMemory::heapUsed"));
        Assert.assertTrue(meta.getSampleKinds().contains("JVMMemory::nonHeapUsed"));
        Assert.assertTrue(meta.getSampleKinds().contains("CMSOldGen::memoryPoolUsed"));
        Assert.assertTrue(meta.getSampleKinds().contains("CMSOldGen::memoryPoolMax"));
        Assert.assertEquals(ImmutableList.<String>copyOf(meta.getSampleKinds()).size(), 4);
    }
}
