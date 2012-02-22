package com.ning.arecibo.collector.process;

import com.google.common.collect.ImmutableMap;
import com.ning.arecibo.collector.MockTimelineDAO;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineDAO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TestCollectorEventProcessor
{
    private static final UUID HOST_UUID = UUID.randomUUID();
    private static final String KIND_A = "kindA";
    private static final String KIND_B = "kindB";
    private static final Map<String, Object> EVENT = ImmutableMap.<String, Object>of(KIND_A, 12, KIND_B, 42);
    private static final int NB_EVENTS = 5;

    private final TimelineDAO dao = new MockTimelineDAO();
    private CollectorEventProcessor processor;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        System.setProperty("arecibo.events.collector.timelines.length", "2s");
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        processor = new CollectorEventProcessor(config, dao);
    }

    @Test(groups = "slow")
    public void testCache() throws Exception
    {
        final DateTime startTime = new DateTime(DateTimeZone.UTC);

        // Check initial state
        Assert.assertEquals(processor.getEventsReceived(), 0);
        Assert.assertEquals(processor.getInMemoryTimelines(), 0);
        Assert.assertEquals(processor.getInMemoryTimelineChunkAndTimes().size(), 0);
        Assert.assertEquals(dao.getHosts().size(), 0);
        Assert.assertEquals(dao.getSampleKinds().size(), 0);

        String csvSamplesKindA = "";
        String csvSamplesKindB = "";
        for (int i = 0; i < NB_EVENTS; i++) {
            final long eventTs = new DateTime(DateTimeZone.UTC).getMillis();
            processor.processEvent(new MapEvent(eventTs, "NOT_USED", HOST_UUID, EVENT));

            // Build expected CSV output
            if (i > 0) {
                csvSamplesKindA += ",";
                csvSamplesKindB += ",";
            }
            csvSamplesKindA = String.format("%s%d,%d", csvSamplesKindA, eventTs / 1000, 12);
            csvSamplesKindB = String.format("%s%d,%d", csvSamplesKindB, eventTs / 1000, 42);

            checkProcessorState(processor, csvSamplesKindA, csvSamplesKindB, i + 1);
        }
        final DateTime endTime = new DateTime(DateTimeZone.UTC);

        // Check the state before the flush to the db
        Assert.assertEquals(processor.getEventsReceived(), NB_EVENTS);
        // One per host
        Assert.assertEquals(processor.getInMemoryTimelines(), 1);
        // One per host and type
        Assert.assertEquals(processor.getInMemoryTimelineChunkAndTimes().size(), 2);
        Assert.assertEquals(dao.getHosts().size(), 1);
        Assert.assertEquals(dao.getSampleKinds().size(), 2);
        Assert.assertTrue(dao.getSampleKinds().values().contains(KIND_A));
        Assert.assertTrue(dao.getSampleKinds().values().contains(KIND_B));

        Thread.sleep(2 * 1000 + 100);

        // Check the state after the flush to the db
        Assert.assertEquals(processor.getEventsReceived(), NB_EVENTS);
        // Should have been flushed
        Assert.assertEquals(processor.getInMemoryTimelines(), 0);
        Assert.assertEquals(processor.getInMemoryTimelineChunkAndTimes().size(), 0);
    }

    private void checkProcessorState(final CollectorEventProcessor processor, final String csvSamplesKindA, final String csvSamplesKindB, final int eventSent) throws IOException
    {
        Assert.assertEquals(processor.getInMemoryTimelines(), 1);
        Assert.assertEquals(processor.getEventsReceived(), eventSent);

        // One per host and per type (two types here: kindA and kindB)
        final List<TimelineChunkAndTimes> inMemoryTimelineChunkAndTimes = processor.getInMemoryTimelineChunkAndTimes();
        Assert.assertEquals(inMemoryTimelineChunkAndTimes.size(), 2);

        checkInMemoryState(inMemoryTimelineChunkAndTimes, csvSamplesKindA, csvSamplesKindB);
    }

    private void checkInMemoryState(final List<TimelineChunkAndTimes> inMemoryTimelineChunkAndTimes, final String csvSamplesKindA, final String csvSamplesKindB) throws IOException
    {
        final Set<String> hostNames = new HashSet<String>();
        final Set<String> sampleKinds = new HashSet<String>();
        for (final TimelineChunkAndTimes chunkAndTimes : inMemoryTimelineChunkAndTimes) {
            // Gather hostnames and sample kinds for later check (see below)
            hostNames.add(chunkAndTimes.getHostName());
            sampleKinds.add(chunkAndTimes.getSampleKind());

            // Check the actual samples
            if (chunkAndTimes.getSampleKind().equals(KIND_A)) {
                Assert.assertEquals(chunkAndTimes.getSamplesAsCSV(), csvSamplesKindA);
            }
            else if (chunkAndTimes.getSampleKind().equals(KIND_B)) {
                Assert.assertEquals(chunkAndTimes.getSamplesAsCSV(), csvSamplesKindB);
            }
            else {
                Assert.fail();
            }
        }

        // Data is only sent from one host
        Assert.assertEquals(hostNames.size(), 1);
        Assert.assertTrue(hostNames.contains(HOST_UUID.toString()));

        // This host sent two types of beans: kindA and kindB
        Assert.assertEquals(sampleKinds.size(), 2);
        Assert.assertTrue(sampleKinds.contains(KIND_A));
        Assert.assertTrue(sampleKinds.contains(KIND_B));
    }
}
