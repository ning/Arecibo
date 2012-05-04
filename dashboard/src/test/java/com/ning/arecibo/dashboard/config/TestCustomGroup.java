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

public class TestCustomGroup
{
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test(groups = "fast")
    public void testJsonSerialization() throws Exception
    {
        final CategoryAndSampleKinds kind = new CategoryAndSampleKinds("JVMMemory", ImmutableList.<String>of("heapUsed", "nonHeapUsed"));

        final String groupName = "JVM";
        final CustomGroup group = new CustomGroup(groupName, ImmutableList.<CategoryAndSampleKinds>of(kind));
        Assert.assertEquals(group.getName(), groupName);
        Assert.assertEquals(group.getKinds().size(), 1);

        final String json = mapper.writeValueAsString(group);
        Assert.assertEquals(json, "{\"name\":\"JVM\",\"kinds\":[{\"eventCategory\":\"JVMMemory\",\"sampleKinds\":[\"nonHeapUsed\",\"heapUsed\"]}]}");

        final CustomGroup parsedGroup = mapper.readValue(json.getBytes(), CustomGroup.class);
        Assert.assertEquals(parsedGroup, group);
        Assert.assertEquals(parsedGroup.getName(), groupName);
        Assert.assertEquals(parsedGroup.getKinds().size(), 1);
    }
}
