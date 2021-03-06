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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.ning.arecibo.dao.MysqlTestingHelper;
import com.ning.arecibo.util.timeline.CategoryIdAndSampleKind;
import com.ning.arecibo.util.timeline.chunks.TimelineChunk;
import com.ning.arecibo.util.timeline.chunks.TimelineChunkConsumer;
import com.ning.arecibo.util.timeline.persistent.DefaultTimelineDAO;
import com.ning.arecibo.util.timeline.persistent.TimelineDAO;
import com.ning.arecibo.util.timeline.samples.SampleCoder;
import com.ning.arecibo.util.timeline.samples.SampleCoderImpl;

public class TestDefaultTimelineDAO
{
    private static final SampleCoder sampleCoder = new SampleCoderImpl();
    private static final TimelineChunkConsumer FAIL_CONSUMER = new TimelineChunkConsumer()
    {
        @Override
        public void processTimelineChunk(final TimelineChunk chunk)
        {
            Assert.fail("Shouldn't find any sample");
        }
    };

    private MysqlTestingHelper helper;

    @BeforeMethod
    public void setUp() throws Exception
    {
        final String ddl = IOUtils.toString(TestDefaultTimelineDAO.class.getResourceAsStream("/collector.sql"));
        helper = new MysqlTestingHelper();

        helper.startMysql();
        helper.initDb(ddl);
    }

    @AfterMethod
    public void tearDown() throws Exception
    {
        helper.stopMysql();
    }

    @Test(groups = "slow")
    public void testGetSampleKindsByHostName() throws Exception
    {
        final TimelineDAO dao = new DefaultTimelineDAO(helper.getDBI(), sampleCoder);
        final DateTime startTime = new DateTime(DateTimeZone.UTC);
        final DateTime endTime = startTime.plusSeconds(2);

        // Create the host
        final String hostName = UUID.randomUUID().toString();
        final Integer hostId = dao.getOrAddHost(hostName);
        Assert.assertNotNull(hostId);

        // Create a timeline times (needed for the join in the dashboard query)
        final Integer eventCategoryId = 123;

        // Create the samples
        final String sampleOne = UUID.randomUUID().toString();
        final Integer sampleOneId = dao.getOrAddSampleKind(hostId, eventCategoryId, sampleOne);
        Assert.assertNotNull(sampleOneId);
        final String sampleTwo = UUID.randomUUID().toString();
        final Integer sampleTwoId = dao.getOrAddSampleKind(hostId, eventCategoryId, sampleTwo);
        Assert.assertNotNull(sampleTwoId);

        // Basic retrieval tests
        final BiMap<Integer, CategoryIdAndSampleKind> sampleKinds = dao.getSampleKinds();
        Assert.assertEquals(sampleKinds.size(), 2);
        Assert.assertEquals(sampleKinds.get(sampleOneId).getEventCategoryId(), (int) eventCategoryId);
        Assert.assertEquals(sampleKinds.get(sampleOneId).getSampleKind(), sampleOne);
        Assert.assertEquals(sampleKinds.get(sampleTwoId).getEventCategoryId(), (int) eventCategoryId);
        Assert.assertEquals(sampleKinds.get(sampleTwoId).getSampleKind(), sampleTwo);
        Assert.assertEquals(dao.getCategoryIdAndSampleKind(sampleOneId).getEventCategoryId(), (int) eventCategoryId);
        Assert.assertEquals(dao.getCategoryIdAndSampleKind(sampleOneId).getSampleKind(), sampleOne);
        Assert.assertEquals(dao.getCategoryIdAndSampleKind(sampleTwoId).getEventCategoryId(), (int) eventCategoryId);
        Assert.assertEquals(dao.getCategoryIdAndSampleKind(sampleTwoId).getSampleKind(), sampleTwo);

        // No samples yet
        Assert.assertEquals(ImmutableList.<Integer>copyOf(dao.getSampleKindIdsByHostId(hostId)).size(), 0);

        dao.insertTimelineChunk(new TimelineChunk(sampleCoder, 0, hostId, sampleOneId, startTime, endTime, new byte[0], new byte[0], 0));
        final ImmutableList<Integer> firstFetch = ImmutableList.<Integer>copyOf(dao.getSampleKindIdsByHostId(hostId));
        Assert.assertEquals(firstFetch.size(), 1);
        Assert.assertEquals(firstFetch.get(0), sampleOneId);

        dao.insertTimelineChunk(new TimelineChunk(sampleCoder, 0, hostId, sampleTwoId, startTime, endTime, new byte[0], new byte[0], 0));
        final ImmutableList<Integer> secondFetch = ImmutableList.<Integer>copyOf(dao.getSampleKindIdsByHostId(hostId));
        Assert.assertEquals(secondFetch.size(), 2);
        Assert.assertTrue(secondFetch.contains(sampleOneId));
        Assert.assertTrue(secondFetch.contains(sampleTwoId));

        // Random sampleKind for random host
        dao.insertTimelineChunk(new TimelineChunk(sampleCoder, 0, Integer.MAX_VALUE - 100, Integer.MAX_VALUE, startTime, endTime, new byte[0], new byte[0], 0));
        final ImmutableList<Integer> thirdFetch = ImmutableList.<Integer>copyOf(dao.getSampleKindIdsByHostId(hostId));
        Assert.assertEquals(secondFetch.size(), 2);
        Assert.assertTrue(thirdFetch.contains(sampleOneId));
        Assert.assertTrue(thirdFetch.contains(sampleTwoId));

        // Test dashboard query
        final AtomicInteger chunksSeen = new AtomicInteger(0);
        dao.getSamplesByHostIdsAndSampleKindIds(ImmutableList.<Integer>of(hostId), ImmutableList.<Integer>of(sampleOneId, sampleTwoId), startTime, startTime.plusSeconds(2), new TimelineChunkConsumer()
        {
            @Override
            public void processTimelineChunk(final TimelineChunk chunk)
            {
                chunksSeen.incrementAndGet();
                Assert.assertEquals((Integer)chunk.getHostId(), hostId);
                Assert.assertTrue(chunk.getSampleKindId() == sampleOneId || chunk.getSampleKindId() == sampleTwoId);
            }
        });
        Assert.assertEquals(chunksSeen.get(), 2);

        // Dummy queries
        dao.getSamplesByHostIdsAndSampleKindIds(ImmutableList.<Integer>of(Integer.MAX_VALUE), null, startTime, startTime.plusDays(1), FAIL_CONSUMER);
        dao.getSamplesByHostIdsAndSampleKindIds(ImmutableList.<Integer>of(hostId), ImmutableList.<Integer>of(Integer.MAX_VALUE), startTime, startTime.plusDays(1), FAIL_CONSUMER);
        dao.getSamplesByHostIdsAndSampleKindIds(ImmutableList.<Integer>of(hostId), ImmutableList.<Integer>of(sampleOneId, sampleTwoId), startTime.plusDays(1), startTime.plusDays(2), FAIL_CONSUMER);
    }
}
