package com.ning.arecibo.collector.process;

import com.google.common.collect.ImmutableMap;
import com.ning.arecibo.collector.MockTimelineDAO;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.util.timeline.TimelineDAO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.UUID;

public class TestCollectorEventProcessor
{
    @Test(groups = "slow")
    public void testCache() throws Exception
    {
        System.setProperty("arecibo.events.collector.timelines.length", "2s");
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        final TimelineDAO dao = new MockTimelineDAO();
        final CollectorEventProcessor processor = new CollectorEventProcessor(config, dao);

        final Map<String, Object> map = ImmutableMap.<String, Object>of("kindA", 12, "kindB", 42);
        final UUID hostUUID = UUID.randomUUID();
        final int nbEvents = 5;

        Assert.assertEquals(processor.getEventsReceived(), 0);
        Assert.assertEquals(processor.getInMemoryTimelines(), 0);
        Assert.assertEquals(dao.getHosts().size(), 0);
        Assert.assertEquals(dao.getSampleKinds().size(), 0);

        for (int i = 0; i < nbEvents; i++) {
            processor.processEvent(new MapEvent(new DateTime(DateTimeZone.UTC).getMillis(), "NOT_USED", hostUUID, map));

            Assert.assertEquals(processor.getInMemoryTimelines(), 1);
            Assert.assertEquals(processor.getEventsReceived(), i + 1);
        }

        Assert.assertEquals(processor.getEventsReceived(), nbEvents);
        // One per host
        Assert.assertEquals(processor.getInMemoryTimelines(), 1);
        Assert.assertEquals(dao.getHosts().size(), 1);
        Assert.assertEquals(dao.getSampleKinds().size(), 2);
        Assert.assertTrue(dao.getSampleKinds().values().contains("kindA"));
        Assert.assertTrue(dao.getSampleKinds().values().contains("kindB"));

        Thread.sleep(2 * 1000 + 100);

        Assert.assertEquals(processor.getEventsReceived(), nbEvents);
        // Should have been flushed
        Assert.assertEquals(processor.getInMemoryTimelines(), 0);
    }
}
