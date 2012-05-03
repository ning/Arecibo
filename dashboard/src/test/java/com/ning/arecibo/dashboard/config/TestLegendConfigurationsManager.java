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
import java.util.Map;

public class TestLegendConfigurationsManager
{
    @Test(groups = "fast")
    public void testDefaultConfiguration() throws Exception
    {
        final DashboardConfig config = new ConfigurationObjectFactory(System.getProperties()).build(DashboardConfig.class);
        final LegendConfigurationsManager manager = new LegendConfigurationsManager(config);
        final Map configuration = manager.getConfiguration();

        Assert.assertEquals(configuration.size(), 2);
        // By default, no custom mappings
        Assert.assertEquals(((Map) configuration.get(LegendConfigurationsManager.LEGENDS_KEY)).size(), 0);
        // By default, no extra fixture to be eval'ed on the client side
        Assert.assertEquals(configuration.get(LegendConfigurationsManager.FIXTURES_KEY), "");
    }

    @Test(groups = "fast")
    public void testLegendConfiguration() throws Exception
    {
        // Create a legend configuration file
        final String configPath = System.getProperty("java.io.tmpdir") + "/TestLegendConfigurationsManager-" + System.currentTimeMillis();
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
        Files.write(legendConfigurationString, new File(configPath), Charsets.UTF_8);

        // Mock the config
        final DashboardConfig config = Mockito.mock(DashboardConfig.class);
        Mockito.when(config.getLegendGroupsFile()).thenReturn(configPath);

        final LegendConfigurationsManager manager = new LegendConfigurationsManager(config);
        final Map configuration = manager.getConfiguration();

        Assert.assertEquals(configuration.size(), 2);
        final Map configurationMap = (Map) configuration.get(LegendConfigurationsManager.LEGENDS_KEY);
        Assert.assertEquals(configurationMap.size(), 2);
        Assert.assertEquals(((Map) configurationMap.get("JVMMemory")).size(), 4);
        Assert.assertEquals(((Map) configurationMap.get("JVMMemory")).get("heapMax"), "formatBase1024KMGTP");
        Assert.assertEquals(((Map) configurationMap.get("ParSurvivorSpace")).size(), 2);
        Assert.assertEquals(((Map) configurationMap.get("ParSurvivorSpace")).get("memoryPoolMax"), "formatBase1024KMGTP");
        Assert.assertEquals(configuration.get(LegendConfigurationsManager.FIXTURES_KEY), "");
    }

    @Test(groups = "fast")
    public void testFixturesConfiguration() throws Exception
    {
        // Create a legend configuration file
        final String configPath = System.getProperty("java.io.tmpdir") + "/TestLegendConfigurationsManager-" + System.currentTimeMillis();
        final String fixturesString = "" +
                "// Default format\n" +
                "formatDefault = function(y) {\n" +
                "    if (y >= 1000000000000)   { return y / 1000000000000 + \"T\" }-\n" +
                "    else if (y >= 1000000000) { return y / 1000000000 + \"B\" }-\n" +
                "    else if (y >= 1000000)    { return y / 1000000 + \"M\" }-\n" +
                "    else if (y >= 1000)       { return y / 1000 + \"K\" }\n" +
                "    else if (y < 1 && y > 0)  { return y.toFixed(2) }\n" +
                "    else if (y == 0)          { return '' }\n" +
                "    else                      { return y }\n" +
                "};\n" +
                "\n" +
                "// Base 1024 format (e.g. memory size)\n" +
                "formatBase1024KMGTP = function(y) {\n" +
                "    if (y >= 1125899906842624)  { return y / 1125899906842624 + \"P\" }\n" +
                "    else if (y >= 1099511627776){ return y / 1099511627776 + \"T\" }\n" +
                "    else if (y >= 1073741824)   { return y / 1073741824 + \"G\" }\n" +
                "    else if (y >= 1048576)      { return y / 1048576 + \"M\" }\n" +
                "    else if (y >= 1024)         { return y / 1024 + \"K\" }\n" +
                "    else if (y < 1 && y > 0)    { return y.toFixed(2) }\n" +
                "    else if (y == 0)            { return '' }\n" +
                "    else                        { return y }\n" +
                "};";
        Files.write(fixturesString, new File(configPath), Charsets.UTF_8);

        // Mock the config
        final DashboardConfig config = Mockito.mock(DashboardConfig.class);
        Mockito.when(config.getLegendFixturesFile()).thenReturn(configPath);

        final LegendConfigurationsManager manager = new LegendConfigurationsManager(config);
        final Map configuration = manager.getConfiguration();

        Assert.assertEquals(configuration.size(), 2);
        Assert.assertEquals(((Map) configuration.get(LegendConfigurationsManager.LEGENDS_KEY)).size(), 0);
        Assert.assertEquals(configuration.get(LegendConfigurationsManager.FIXTURES_KEY), fixturesString);
    }
}
