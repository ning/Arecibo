package com.ning.arecibo.collector.process;

import com.google.common.collect.ImmutableMap;
import com.ning.arecibo.collector.MockTimelineDAO;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.TimelineHostEventAccumulator;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class TestFileBackedBuffer
{
    private static final UUID HOST_UUID = UUID.randomUUID();
    private static final String KIND_A = "kindA";
    private static final String KIND_B = "kindB";
    private static final Map<String, Object> EVENT = ImmutableMap.<String, Object>of(KIND_A, 12, KIND_B, 42);

    private final TimelineDAO dao = new MockTimelineDAO();
    private Path path;
    private CollectorEventProcessor processor;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        path = Files.createTempDirectory("TestFileBackedBuffer");
        System.setProperty("arecibo.events.collector.spoolDir", path.toString());
        System.setProperty("arecibo.events.collector.timelines.length", "60s");
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        processor = new CollectorEventProcessor(config, dao);
    }

    @Test(groups = "slow")
    public void testAppend() throws Exception
    {
        final Event event = new MapEvent(new DateTime(DateTimeZone.UTC).getMillis(), "NOT_USED", HOST_UUID, EVENT);

        for (final TimelineHostEventAccumulator accumulator : processor.getAccumulators()) {
            Assert.assertEquals(accumulator.getBackingBuffer().getFilesCreated(), 0);
        }
        Assert.assertEquals(FileUtils.listFiles(new File(path.toString()), new String[]{"bin"}, false).size(), 0);

        for (int i = 0; i < 5000000; i++) {
            processor.processEvent(event);
        }

        for (final TimelineHostEventAccumulator accumulator : processor.getAccumulators()) {
            Assert.assertTrue(accumulator.getBackingBuffer().getFilesCreated() > 0);
        }
        Assert.assertTrue(FileUtils.listFiles(new File(path.toString()), new String[]{"bin"}, false).size() > 0);

        // Discard files
        for (final TimelineHostEventAccumulator accumulator : processor.getAccumulators()) {
            accumulator.getBackingBuffer().discard();
        }
        Assert.assertEquals(FileUtils.listFiles(new File(path.toString()), new String[]{"bin"}, false).size(), 0);
    }
}
