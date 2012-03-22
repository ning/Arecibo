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

package com.ning.arecibo.collector.persistent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.ning.arecibo.collector.TestModulesFactory;
import com.ning.arecibo.collector.process.EventsUtils;
import com.ning.arecibo.dao.MysqlTestingHelper;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.SampleOpcode;
import com.ning.arecibo.util.timeline.ScalarSample;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimesConsumer;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.TimelineHostEventAccumulator;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Guice(moduleFactory = TestModulesFactory.class)
public class TestTimelineAggregator
{
    private static final UUID HOST_UUID = UUID.randomUUID();
    private static final String HOST_NAME = HOST_UUID.toString();
    private static final String EVENT_TYPE = "myType";
    private static final String MIN_HEAPUSED_KIND = EventsUtils.getSampleKindFromEventAttribute(EVENT_TYPE, "min_heapUsed");
    private static final String MAX_HEAPUSED_KIND = EventsUtils.getSampleKindFromEventAttribute(EVENT_TYPE, "max_heapUsed");
    private static final DateTime START_TIME = new DateTime(DateTimeZone.UTC);

    @Inject
    MysqlTestingHelper helper;

    @Inject
    TimelineDAO timelineDAO;

    @Inject
    TimelineAggregator aggregator;

    @Inject
    Injector injector;

    private Integer hostId = null;
    private Integer minHeapUsedKindId = null;
    private Integer maxHeapUsedKindId = null;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        final String ddl = IOUtils.toString(TestTimelineAggregator.class.getResourceAsStream("/collector.sql"));

        helper.startMysql();
        helper.initDb(ddl);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception
    {
        helper.stopMysql();
    }

    @Test(groups = "slow")
    public void testAggregation() throws Exception
    {
        // Create the host
        hostId = timelineDAO.getOrAddHost(HOST_NAME);
        Assert.assertNotNull(hostId);
        Assert.assertEquals(timelineDAO.getHosts().values().size(), 1);

        // Create the sample kinds
        minHeapUsedKindId = timelineDAO.getOrAddSampleKind(hostId, MIN_HEAPUSED_KIND);
        Assert.assertNotNull(minHeapUsedKindId);
        maxHeapUsedKindId = timelineDAO.getOrAddSampleKind(hostId, MAX_HEAPUSED_KIND);
        Assert.assertNotNull(maxHeapUsedKindId);
        Assert.assertEquals(timelineDAO.getSampleKinds().values().size(), 2);

        // Create two sets of times: T - 125 ... T - 65 ; T - 60 ... T (note the gap!)
        createAOneHourTimelineTimes(125);
        createAOneHourTimelineTimes(60);

        // Check the getSamplesByHostNamesAndSampleKinds DAO method works as expected
        // You might want to draw timelines on a paper and remember boundaries are inclusive to understand these numbers
        checkSamplesForATimeline(185, 126, 0);
        checkSamplesForATimeline(185, 125, 2);
        checkSamplesForATimeline(64, 61, 0);
        checkSamplesForATimeline(125, 65, 2);
        checkSamplesForATimeline(60, 0, 2);
        checkSamplesForATimeline(125, 0, 4);
        checkSamplesForATimeline(124, 0, 4);
        checkSamplesForATimeline(124, 66, 2);

        aggregator.getAndProcessTimelineAggregationCandidates();

        Assert.assertEquals(timelineDAO.getHosts().values().size(), 1);
        Assert.assertEquals(timelineDAO.getSampleKinds().values().size(), 2);

        // Similar than above, but we have only 2 now
        checkSamplesForATimeline(185, 126, 0);
        checkSamplesForATimeline(185, 125, 2);
        // Note, the gap is filled now
        checkSamplesForATimeline(64, 61, 2);
        checkSamplesForATimeline(125, 65, 2);
        checkSamplesForATimeline(60, 0, 2);
        checkSamplesForATimeline(125, 0, 2);
        checkSamplesForATimeline(124, 0, 2);
        checkSamplesForATimeline(124, 66, 2);
    }

    private void checkSamplesForATimeline(final Integer startTimeMinutesAgo, final Integer endTimeMinutesAgo, final long expectedChunks) throws InterruptedException
    {
        final AtomicLong timelineChunkSeen = new AtomicLong(0);

        timelineDAO.getSamplesByHostNamesAndSampleKinds(ImmutableList.<String>of(HOST_NAME), ImmutableList.<String>of(MIN_HEAPUSED_KIND, MAX_HEAPUSED_KIND),
            START_TIME.minusMinutes(startTimeMinutesAgo), START_TIME.minusMinutes(endTimeMinutesAgo), new TimelineChunkAndTimesConsumer()
        {
            @Override
            public void processTimelineChunkAndTimes(final TimelineChunkAndTimes chunkAndTimes)
            {
                Assert.assertEquals(chunkAndTimes.getHostName(), HOST_NAME);
                Assert.assertTrue(chunkAndTimes.getSampleKind().equals(MIN_HEAPUSED_KIND) || chunkAndTimes.getSampleKind().equals(MAX_HEAPUSED_KIND));
                timelineChunkSeen.incrementAndGet();
            }
        });

        Assert.assertEquals(timelineChunkSeen.get(), expectedChunks);
    }

    private void createAOneHourTimelineTimes(final int startTimeMinutesAgo) throws IOException
    {
        final TimelineHostEventAccumulator accumulator = new TimelineHostEventAccumulator(timelineDAO, hostId, EVENT_TYPE, false);
        // 120 samples per hour
        for (int i = 0; i < 120; i++) {
            final DateTime eventDateTime = START_TIME.minusMinutes(startTimeMinutesAgo).plusSeconds(i * 30);
            final Map<Integer, ScalarSample> event = createEvent(eventDateTime.getMillis());
            final HostSamplesForTimestamp samples = new HostSamplesForTimestamp(hostId, EVENT_TYPE, eventDateTime, event);
            accumulator.addHostSamples(samples);
        }

        accumulator.extractAndSaveTimelineChunks();
    }

    private Map<Integer, ScalarSample> createEvent(final long ts)
    {
        return ImmutableMap.<Integer, ScalarSample>of(
            minHeapUsedKindId, new ScalarSample(SampleOpcode.LONG, Long.MIN_VALUE + ts),
            maxHeapUsedKindId, new ScalarSample(SampleOpcode.LONG, Long.MAX_VALUE - ts)
        );
    }
}
