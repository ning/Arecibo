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

package com.ning.arecibo.collector.rest;

import com.ning.arecibo.collector.CollectorClient;
import com.ning.arecibo.collector.CollectorClientConfig;
import com.ning.arecibo.collector.discovery.CollectorFinder;
import com.ning.arecibo.collector.discovery.DefaultCollectorFinder;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "integration,slow", enabled = false)
public class TestDefaultCollectorClientIntegration
{
    CollectorClient client;

    @BeforeMethod
    public void setUp() throws Exception
    {
        System.setProperty("arecibo.collectorClient.collectorUri", "http://127.0.0.1:8088");
        final CollectorClientConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorClientConfig.class);
        final CollectorFinder finder = new DefaultCollectorFinder(config);
        client = new DefaultCollectorClient(finder);
    }

    @Test
    public void testGetHosts() throws Exception
    {
        Assert.assertNotNull(client.getHosts());
        Assert.assertTrue(client.getHosts().iterator().hasNext());
    }

    @Test
    public void testGetSampleKinds() throws Exception
    {
        Assert.assertNotNull(client.getSampleKinds());
        Assert.assertTrue(client.getSampleKinds().iterator().hasNext());
    }
}
