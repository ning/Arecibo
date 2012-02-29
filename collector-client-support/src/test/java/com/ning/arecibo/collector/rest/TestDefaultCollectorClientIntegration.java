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
