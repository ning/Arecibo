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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.DBI;

import com.google.common.collect.BiMap;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.timeline.CategoryIdAndSampleKind;
import com.ning.arecibo.util.timeline.chunks.TimelineChunk;
import com.ning.arecibo.util.timeline.persistent.CachingTimelineDAO;
import com.ning.arecibo.util.timeline.persistent.DefaultTimelineDAO;
import com.ning.arecibo.util.timeline.times.TimelineCoder;

/**
 * This class simulates the database load due to insertions and deletions of
 * TimelineChunks rows, as required by sample processing and
 * aggregation.  Each is single-threaded.
 */
public class TimelineLoadGenerator {
    private static final Logger log = Logger.getLogger(TimelineLoadGenerator.class);
    private static final int EVENT_CATEGORY_COUNT = Integer.parseInt(System.getProperty("arecibo.collector.timeline.eventCategoryCount", "250"));
    private static final int HOST_ID_COUNT = Integer.parseInt(System.getProperty("arecibo.collector.timeline.hostIdCount", "2000"));
    private static final int AVERAGE_SAMPLE_KINDS_PER_CATEGORY = Integer.parseInt(System.getProperty("arecibo.collector.timeline.averageSampleKindsPerCategory", "20"));
    private static final int AVERAGE_CATEGORIES_PER_HOST = Integer.parseInt(System.getProperty("arecibo.collector.timeline.averageSampleKindsPerCategory", "25"));
    private static final int SAMPLE_KIND_COUNT = EVENT_CATEGORY_COUNT * AVERAGE_SAMPLE_KINDS_PER_CATEGORY;
    private static final int CREATE_BATCH_SIZE = Integer.parseInt(System.getProperty("arecibo.collector.timeline.createBatchSize", "1000"));
    // Mandatory properties
    private static final String DBI_URL = System.getProperty("arecibo.collector.timeline.db.url");
    private static final String DBI_USER = System.getProperty("arecibo.collector.timeline.db.user");
    private static final String DBI_PASSWORD = System.getProperty("arecibo.collector.timeline.db.password");

    private static final Random rand = new Random(System.currentTimeMillis());

    private final List<Integer> hostIds;
    private final BiMap<Integer, String> hosts;
    private final BiMap<Integer, String> eventCategories;
    private final List<Integer> eventCategoryIds;
    private final BiMap<Integer, CategoryIdAndSampleKind> sampleKindsBiMap;
    private final Map<Integer, List<Integer>> categorySampleKindIds;
    private final Map<Integer, List<Integer>> categoriesForHostId;

    private final DefaultTimelineDAO defaultTimelineDAO;
    private final CachingTimelineDAO timelineDAO;
    private final DBI dbi;

    private final AtomicInteger timelineChunkIdCounter = new AtomicInteger(0);

    public TimelineLoadGenerator()
    {
        this.dbi = new DBI(DBI_URL, DBI_USER, DBI_PASSWORD);
        this.defaultTimelineDAO = new DefaultTimelineDAO(dbi);
        this.timelineDAO = new CachingTimelineDAO(defaultTimelineDAO);
        log.info("DBI initialized");

        // Make some hosts
        final List<String> hostNames = new ArrayList<String>(HOST_ID_COUNT);
        for (int i=0; i<HOST_ID_COUNT; i++) {
            hostNames.add(String.format("host-%d", i+1));
        }
        defaultTimelineDAO.bulkInsertHosts(hostNames);
        hosts = timelineDAO.getHosts();
        hostIds = new ArrayList<Integer>(hosts.keySet());
        Collections.sort(hostIds);
        log.info("%d hosts created", hostIds.size());

        // Make some event categories
        final List<String> categoryNames = new ArrayList<String>(EVENT_CATEGORY_COUNT);
        for (int i=0; i<EVENT_CATEGORY_COUNT; i++) {
            categoryNames.add(String.format("category-%d", i));
        }
        defaultTimelineDAO.bulkInsertEventCategories(categoryNames);
        eventCategories = timelineDAO.getEventCategories();
        eventCategoryIds = new ArrayList<Integer>(eventCategories.keySet());
        Collections.sort(eventCategoryIds);
        log.info("%d event categories created", eventCategoryIds.size());

        // Make some sample kinds.  For now, give each category the same number of sample kinds
        final List<CategoryIdAndSampleKind> categoriesAndSampleKinds = new ArrayList<CategoryIdAndSampleKind>();
        for (int eventCategoryId : eventCategoryIds) {
            for (int i=0; i<AVERAGE_SAMPLE_KINDS_PER_CATEGORY; i++) {
                final String sampleKind = String.format("%s-sample-kind-%d", eventCategories.get(eventCategoryId), i + 1);
                categoriesAndSampleKinds.add(new CategoryIdAndSampleKind(eventCategoryId, sampleKind));
            }
        }
        defaultTimelineDAO.bulkInsertSampleKinds(categoriesAndSampleKinds);
        // Make a fast map from categoryId to a list of sampleKindIds in that category
        sampleKindsBiMap = timelineDAO.getSampleKinds();
        categorySampleKindIds = new HashMap<Integer, List<Integer>>();
        int sampleKindIdCounter = 0;
        for (Map.Entry<Integer, CategoryIdAndSampleKind> entry : sampleKindsBiMap.entrySet()) {
            final int categoryId = entry.getValue().getEventCategoryId();
            List<Integer> sampleKindIds = categorySampleKindIds.get(categoryId);
            if (sampleKindIds == null) {
                sampleKindIds = new ArrayList<Integer>();
                categorySampleKindIds.put(categoryId, sampleKindIds);
            }
            final int sampleKindId = entry.getKey();
            sampleKindIds.add(sampleKindId);
            sampleKindIdCounter++;
        }
        log.info("%d sampleKindIds created", sampleKindIdCounter);
        // Assign categories to hosts
        categoriesForHostId = new HashMap<Integer, List<Integer>>();
        int categoryCounter = 0;
        for (int hostId : hostIds) {
            final List<Integer> categories = new ArrayList<Integer>();
            categoriesForHostId.put(hostId, categories);
            for (int i=0; i<AVERAGE_CATEGORIES_PER_HOST; i++) {
                final int categoryId = eventCategoryIds.get(categoryCounter);
                categories.add(categoryId);
                categoryCounter = (categoryCounter + 1) % EVENT_CATEGORY_COUNT;
            }
        }
        log.info("Finished creating hosts, categories and sample kinds");
    }

    private void addChunkAndMaybeSave(final List<TimelineChunk> timelineChunkList, final TimelineChunk timelineChunk)
    {
        timelineChunkList.add(timelineChunk);
        if (timelineChunkList.size() >= CREATE_BATCH_SIZE) {
            defaultTimelineDAO.bulkInsertTimelineChunks(timelineChunkList);
            timelineChunkList.clear();
            log.info("Inserted %d TimelineChunk rows", timelineChunkIdCounter.get());
        }
    }

    /**
     * This method simulates adding a ton of timelines, in more-or-less the way they would be added in real life.
     */
    private void insertManyTimelines() throws Exception
    {
        final List<TimelineChunk> timelineChunkList = new ArrayList<TimelineChunk>();
        DateTime startTime = new DateTime().minusDays(1);
        DateTime endTime = startTime.plusHours(1);
        final int sampleCount = 120;  // 1 hours worth
        for (int i=0; i<12; i++) {
            for (int hostId : hostIds) {
                for (int categoryId : categoriesForHostId.get(hostId)) {
                    final List<DateTime> dateTimes = new ArrayList<DateTime>(sampleCount);
                    for (int sc=0; sc<sampleCount; sc++) {
                        dateTimes.add(startTime.plusSeconds(sc * 30));
                    }
                    final byte[] timeBytes = TimelineCoder.compressDateTimes(dateTimes);
                    for (int sampleKindId : categorySampleKindIds.get(categoryId)) {
                        final TimelineChunk timelineChunk = makeTimelineChunk(hostId, sampleKindId, startTime, endTime, timeBytes, sampleCount);
                        addChunkAndMaybeSave(timelineChunkList, timelineChunk);

                    }
                }
            }
            if (timelineChunkList.size() > 0) {
                defaultTimelineDAO.bulkInsertTimelineChunks(timelineChunkList);
            }
            log.info("After hour %d, inserted %d TimelineChunk rows", i, timelineChunkIdCounter.get());
            startTime = endTime;
            endTime = endTime.plusHours(1);
        }
    }

    private TimelineChunk makeTimelineChunk(final int hostId, final int sampleKindId, final DateTime startTime, final DateTime endTime, final byte[] timeBytes, final int sampleCount)
    {
        final byte[] samples = new byte[3 + rand.nextInt(sampleCount) * 2];
        return new TimelineChunk(timelineChunkIdCounter.incrementAndGet(), hostId, sampleKindId, startTime, endTime, timeBytes, samples, sampleCount);
    }

    public static void main(String[] args) throws Exception
    {
        final TimelineLoadGenerator loadGenerator = new TimelineLoadGenerator();
        loadGenerator.insertManyTimelines();
    }
}
