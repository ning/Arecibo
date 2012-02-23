package com.ning.arecibo.collector.persistent;

import com.google.common.collect.ImmutableMap;
import com.ning.arecibo.collector.MockTimelineDAO;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.collector.process.CollectorEventProcessor;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.timeline.TimelineDAO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.UUID;

public class TestFileBackedBuffer
{
    private static final UUID HOST_UUID = UUID.randomUUID();
    private static final String KIND_A = "kindA";
    private static final String KIND_B = "kindB";
    private static final Map<String, Object> EVENT = ImmutableMap.<String, Object>of(KIND_A, 12, KIND_B, 42);

    private final TimelineDAO dao = new MockTimelineDAO();
    private CollectorEventProcessor processor;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        System.setProperty("arecibo.events.collector.timelines.length", "60s");
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        processor = new CollectorEventProcessor(config, dao);
    }

    @Test(groups = "slow")
    public void testAppend() throws Exception
    {
        final Event event = new MapEvent(new DateTime(DateTimeZone.UTC).getMillis(), "NOT_USED", HOST_UUID, EVENT);

        Assert.assertEquals(processor.getBuffer().flushCount(), 0);

        for (int i = 0; i < 5000000; i++) {
            processor.processEvent(event);
        }

        Assert.assertTrue(processor.getBuffer().flushCount() > 0);
    }
}
