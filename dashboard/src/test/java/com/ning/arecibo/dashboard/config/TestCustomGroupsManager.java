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

import com.ning.arecibo.dashboard.guice.DashboardConfig;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.mockito.Mockito;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

public class TestCustomGroupsManager
{
    @Test(groups = "fast")
    public void testDefaultConfiguration() throws Exception
    {
        final DashboardConfig config = new ConfigurationObjectFactory(System.getProperties()).build(DashboardConfig.class);
        final CustomGroupsManager manager = new CustomGroupsManager(config);
        final List<CustomGroup> configuration = manager.getCustomGroups();

        Assert.assertEquals(configuration.size(), 0);
    }

    @Test(groups = "fast")
    public void testCustomGroupsConfiguration() throws Exception
    {
        // Create a groups configuration file
        final String configPath = System.getProperty("java.io.tmpdir") + "/TestCustomGroupsManager-" + System.currentTimeMillis();
        final String groupsConfigurationString = "" +
                "[\n" +
                "    {\n" +
                "        \"name\":\"JVM\",\n" +
                "        \"kinds\":[\n" +
                "            {\n" +
                "                \"eventCategory\":\"JVMMemory\",\n" +
                "                \"sampleKinds\":[\"heapUsed\",\"nonHeapUsed\"]\n" +
                "            },\n" +
                "            {\n" +
                "                \"eventCategory\":\"CMSOldGen\",\n" +
                "                \"sampleKinds\":[\"memoryPoolUsed\"]\n" +
                "            },\n" +
                "            {\n" +
                "                \"eventCategory\":\"CMSPermGen\",\n" +
                "                \"sampleKinds\":[\"memoryPoolUsed\"]\n" +
                "            },\n" +
                "            {\n" +
                "                \"eventCategory\":\"CodeCache\",\n" +
                "                \"sampleKinds\":[\"memoryPoolUsed\"]\n" +
                "            },\n" +
                "            {\n" +
                "                \"eventCategory\":\"ParEdenSpace\",\n" +
                "                \"sampleKinds\":[\"memoryPoolUsed\"]\n" +
                "            },\n" +
                "            {\n" +
                "                \"eventCategory\":\"ParSurvivorSpace\",\n" +
                "                \"sampleKinds\":[\"memoryPoolUsed\"]\n" +
                "            },\n" +
                "            {\n" +
                "                \"eventCategory\":\"ConcurrentMarkSweepGC\",\n" +
                "                \"sampleKinds\":[\"garbageCollectionRate\"]\n" +
                "            },\n" +
                "            {\n" +
                "                \"eventCategory\":\"ParNewGC\",\n" +
                "                \"sampleKinds\":[\"garbageCollectionRate\"]\n" +
                "            },\n" +
                "            {\n" +
                "                \"eventCategory\":\"Threading\",\n" +
                "                \"sampleKinds\":[\"threadCount\"]\n" +
                "            },\n" +
                "            {\n" +
                "                \"eventCategory\":\"JVMOperatingSystemPerZone\",\n" +
                "                \"sampleKinds\":[\"ProcessCpuTime\"]\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "]";
        Files.write(groupsConfigurationString, new File(configPath), Charsets.UTF_8);

        // Mock the config
        final DashboardConfig config = Mockito.mock(DashboardConfig.class);
        Mockito.when(config.getCustomGroupsFile()).thenReturn(configPath);

        final CustomGroupsManager manager = new CustomGroupsManager(config);
        final List<CustomGroup> configuration = manager.getCustomGroups();

        Assert.assertEquals(configuration.size(), 1);
        final CustomGroup group = configuration.get(0);
        Assert.assertEquals(group.getName(), "JVM");
        Assert.assertEquals(group.getKinds().size(), 10);
        Assert.assertEquals(group.getKinds().get(0).getEventCategory(), "JVMMemory");
        Assert.assertEquals(group.getKinds().get(0).getSampleKinds().size(), 2);
    }
}
