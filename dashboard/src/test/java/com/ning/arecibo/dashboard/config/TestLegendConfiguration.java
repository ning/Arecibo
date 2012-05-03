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

import org.codehaus.jackson.map.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestLegendConfiguration
{
    @Test(groups = "fast")
    public void testSerialization() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        final String legendConfigurationString = "{\n" +
                "  \"JVMMemory\": {\n" +
                "    \"heapMax\": \"formatBase1024KMGTP\",\n" +
                "    \"nonHeapMax\": \"formatBase1024KMGTP\",\n" +
                "    \"heapUsed\": \"formatBase1024KMGTP\",\n" +
                "    \"nonHeapUsed\": \"formatBase1024KMGTP\"\n" +
                "  },\n" +
                "  \"ParSurvivorSpace\": {\n" +
                "    \"memoryPoolMax\": \"formatBase1024KMGTP\",\n" +
                "    \"memoryPoolUsed\": \"formatBase1024KMGTP\"\n" +
                "  }\n" +
                "}";

        final LegendConfiguration legendConfiguration = mapper.readValue(legendConfigurationString, LegendConfiguration.class);
        Assert.assertEquals(legendConfiguration.size(), 2);
        Assert.assertEquals(legendConfiguration.get("JVMMemory").size(), 4);
        Assert.assertEquals(legendConfiguration.get("JVMMemory").get("heapMax"), "formatBase1024KMGTP");
        Assert.assertEquals(legendConfiguration.get("ParSurvivorSpace").size(), 2);
        Assert.assertEquals(legendConfiguration.get("ParSurvivorSpace").get("memoryPoolMax"), "formatBase1024KMGTP");
        Assert.assertNull(legendConfiguration.get("foo"));
    }
}
