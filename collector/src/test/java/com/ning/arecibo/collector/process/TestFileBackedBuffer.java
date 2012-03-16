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

package com.ning.arecibo.collector.process;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ning.arecibo.collector.MockTimelineDAO;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.persistent.FileBackedBuffer;
import com.ning.arecibo.util.timeline.persistent.Replayer;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.config.ConfigurationObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestFileBackedBuffer
{
    private static final Logger log = LoggerFactory.getLogger(TestFileBackedBuffer.class);

    private static final UUID HOST_UUID = UUID.randomUUID();
    private static final String KIND_A = "kindA";
    private static final String KIND_B = "kindB";
    private static final Map<String, Object> EVENT = ImmutableMap.<String, Object>of(KIND_A, 12, KIND_B, 42);
    private static final int NB_EVENTS = 1220491;
    private static final File basePath = new File(System.getProperty("java.io.tmpdir"), "TestFileBackedBuffer-" + System.currentTimeMillis());

    private final TimelineDAO dao = new MockTimelineDAO();
    private CollectorEventProcessor processor;
    private TimelineEventHandler timelineEventHandler;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        Assert.assertTrue(basePath.mkdir());
        System.setProperty("arecibo.collector.timelines.spoolDir", basePath.getAbsolutePath());
        System.setProperty("arecibo.collector.timelines.length", "60s");
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        timelineEventHandler = new TimelineEventHandler(config, dao, new FileBackedBuffer(config.getSpoolDir(), "TimelineEventHandler"));
        processor = new CollectorEventProcessor(ImmutableList.<EventHandler>of(timelineEventHandler));
    }

    @Test(groups = "slow")
    public void testAppend() throws Exception
    {
        final List<Event> eventsSent = new ArrayList<Event>();

        // Sanity check before the tests
        Assert.assertEquals(timelineEventHandler.getBackingBuffer().getFilesCreated(), 0);
        Assert.assertEquals(FileUtils.listFiles(basePath, new String[]{"bin"}, false).size(), 0);

        // Send enough events to spill over to disk
        final DateTime startTime = new DateTime(DateTimeZone.UTC);
        for (int i = 0; i < NB_EVENTS; i++) {
            final Event event = new MapEvent(startTime.plusSeconds(i).getMillis(), "NOT_USED", HOST_UUID, EVENT);
            processor.processEvent(event);
            eventsSent.add(event);
        }

        // Check the files have been created (at least one per accumulator)
        final long bytesOnDisk = timelineEventHandler.getBackingBuffer().getBytesOnDisk();
        Assert.assertTrue(timelineEventHandler.getBackingBuffer().getFilesCreated() > 0);
        final Collection<File> writtenFiles = FileUtils.listFiles(basePath, new String[]{"bin"}, false);
        Assert.assertTrue(writtenFiles.size() > 0);

        log.info("Sent {} events and wrote {} bytes on disk ({} bytes/event)", new Object[]{NB_EVENTS, bytesOnDisk, bytesOnDisk / NB_EVENTS});

        // Replay the events. Note that eventsSent != eventsReplayed as some of the ones sent are still in memory
        final Replayer replayer = new Replayer(basePath.getAbsolutePath());
        final List<HostSamplesForTimestamp> eventsReplayed = replayer.readAll();
        for (int i = 0; i < eventsReplayed.size(); i++) {
            Assert.assertEquals(eventsReplayed.get(i).getTimestamp().getMillis(), eventsSent.get(i).getTimestamp());
            Assert.assertEquals(eventsReplayed.get(i).getCategory(), eventsSent.get(i).getEventType());
        }

        // Make sure files have been deleted
        Assert.assertEquals(FileUtils.listFiles(basePath, new String[]{"bin"}, false).size(), 0);
    }
}
