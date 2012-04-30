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

package com.ning.arecibo.collector;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ning.arecibo.util.timeline.CategoryIdAndSampleKind;
import com.ning.arecibo.util.timeline.HostIdAndSampleKindId;
import com.ning.arecibo.util.timeline.StartTimes;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineChunkConsumer;
import com.ning.arecibo.util.timeline.TimelineDAO;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class MockTimelineDAO implements TimelineDAO
{
    private final BiMap<Integer, String> hosts = HashBiMap.create();
    private final BiMap<Integer, CategoryIdAndSampleKind> sampleKinds = HashBiMap.create();
    private final BiMap<Integer, String> eventCategories = HashBiMap.create();
    private final BiMap<Integer, TimelineChunk> timelineChunks = HashBiMap.create();
    private final Map<Integer, Set<Integer>> sampleKindIdsForHostId = new HashMap<Integer, Set<Integer>>();
    private final Map<Integer, Map<Integer, List<TimelineChunk>>> samplesPerHostAndSampleKind = new HashMap<Integer, Map<Integer, List<TimelineChunk>>>();
    private final AtomicReference<StartTimes> lastStartTimes = new AtomicReference<StartTimes>();

    @Override
    public Integer getHostId(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        synchronized (hosts) {
            return hosts.inverse().get(host);
        }
    }

    @Override
    public String getHost(final Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        synchronized (hosts) {
            return hosts.get(hostId);
        }
    }

    @Override
    public BiMap<Integer, String> getHosts()
    {
        return hosts;
    }

    @Override
    public int getOrAddHost(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        synchronized (hosts) {
            final Integer hostId = getHostId(host);
            if (hostId == null) {
                hosts.put(hosts.size() + 1, host);
                return hosts.size();
            }
            else {
                return hostId;
            }
        }
    }

    @Override
    public Integer getEventCategoryId(String eventCategory) throws UnableToObtainConnectionException, CallbackFailedException {
        synchronized (eventCategories) {
            return eventCategories.inverse().get(eventCategory);
        }
    }

    @Override
    public String getEventCategory(Integer eventCategoryId) throws UnableToObtainConnectionException, CallbackFailedException {
        synchronized (eventCategories) {
            return eventCategories.get(eventCategoryId);
        }
    }

    @Override
    public int getOrAddEventCategory( String eventCategory) throws UnableToObtainConnectionException, CallbackFailedException {
        synchronized (eventCategories) {
            Integer eventCategoryId = getEventCategoryId(eventCategory);
            if (eventCategoryId == null) {
                eventCategoryId = eventCategories.size() + 1;
                eventCategories.put(eventCategoryId, eventCategory);
            }

            return eventCategoryId;
        }
    }

    @Override
    public BiMap<Integer, String> getEventCategories() throws UnableToObtainConnectionException, CallbackFailedException {
        return eventCategories;
    }

    @Override
    public Integer getSampleKindId(final int eventCategoryId, final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        synchronized (sampleKinds) {
            return sampleKinds.inverse().get(new CategoryIdAndSampleKind(eventCategoryId, sampleKind));
        }
    }

    @Override
    public CategoryIdAndSampleKind getCategoryIdAndSampleKind(final Integer sampleKindId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        synchronized (sampleKinds) {
            return sampleKinds.get(sampleKindId);
        }
    }

    @Override
    public BiMap<Integer, CategoryIdAndSampleKind> getSampleKinds()
    {
        synchronized (sampleKinds) {
            return sampleKinds;
        }
    }

    @Override
    public int getOrAddSampleKind(final Integer hostId, final Integer eventCategoryId, final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        synchronized (sampleKinds) {
            Integer sampleKindId = getSampleKindId(eventCategoryId, sampleKind);
            if (sampleKindId == null) {
                sampleKindId = sampleKinds.size() + 1;
                sampleKinds.put(sampleKindId, new CategoryIdAndSampleKind(eventCategoryId, sampleKind));
            }
            if (hostId != null) {
                Set<Integer> sampleKindIds = sampleKindIdsForHostId.get(hostId);
                if (sampleKindIds == null) {
                    sampleKindIds = new HashSet<Integer>();
                    sampleKindIdsForHostId.put(hostId, sampleKindIds);
                }
                sampleKindIds.add(sampleKindId);
            }
            return sampleKindId;
        }
    }

    @Override
    public Iterable<Integer> getSampleKindIdsByHostId(final Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        final List<Integer> sampleKindIds = new ArrayList<Integer>();
        synchronized (sampleKindIdsForHostId) {
            final Set<Integer> sampleKindIdsSet = sampleKindIdsForHostId.get(hostId);
            if (sampleKindIdsSet != null) {
                sampleKindIds.addAll(sampleKindIdsSet);
            }
        }
        return sampleKindIds;
    }

    @Override
    public Iterable<HostIdAndSampleKindId> getSampleKindIdsForAllHosts() throws UnableToObtainConnectionException, CallbackFailedException {
        final List<HostIdAndSampleKindId> hostIdsAndSampleKindIds = new ArrayList<HostIdAndSampleKindId>();
        synchronized (sampleKindIdsForHostId) {
            for (Map.Entry<Integer, Set<Integer>> entry : sampleKindIdsForHostId.entrySet()) {
                final int hostId = entry.getKey();
                final Set<Integer> sampleKindIds = entry.getValue();
                for (int sampleKindId : sampleKindIds) {
                    hostIdsAndSampleKindIds.add(new HostIdAndSampleKindId(hostId, sampleKindId));
                }
            }
        }
        return hostIdsAndSampleKindIds;
    }

    @Override
    public Long insertTimelineChunk(final TimelineChunk chunk)
    {
        final Long timelineChunkId;
        synchronized (timelineChunks) {
            timelineChunks.put(timelineChunks.size(), chunk);
            timelineChunkId = (long)timelineChunks.size() - 1;
        }

        synchronized (samplesPerHostAndSampleKind) {
            Map<Integer, List<TimelineChunk>> samplesPerSampleKind = samplesPerHostAndSampleKind.get(chunk.getHostId());
            if (samplesPerSampleKind == null) {
                samplesPerSampleKind = new HashMap<Integer, List<TimelineChunk>>();
            }

            List<TimelineChunk> chunkAndTimes = samplesPerSampleKind.get(chunk.getSampleKindId());
            if (chunkAndTimes == null) {
                chunkAndTimes = new ArrayList<TimelineChunk>();
            }

            chunkAndTimes.add(chunk);
            samplesPerSampleKind.put(chunk.getSampleKindId(), chunkAndTimes);

            samplesPerHostAndSampleKind.put(chunk.getHostId(), samplesPerSampleKind);
        }

        return timelineChunkId;
    }

    @Override
    public void getSamplesByHostIdsAndSampleKindIds(final List<Integer> hostIds, @Nullable final List<Integer> sampleKindIds, final DateTime startTime, final DateTime endTime, final TimelineChunkConsumer chunkConsumer) throws UnableToObtainConnectionException, CallbackFailedException
    {
        for (final Integer hostId : samplesPerHostAndSampleKind.keySet()) {
            if (hostIds.indexOf(hostId) == -1) {
                continue;
            }

            final Map<Integer, List<TimelineChunk>> samplesPerSampleKind = samplesPerHostAndSampleKind.get(hostId);
            for (final Integer sampleKindId : samplesPerSampleKind.keySet()) {
                if (sampleKindIds != null && sampleKindIds.indexOf(sampleKindId) == -1) {
                    continue;
                }

                for (final TimelineChunk chunk : samplesPerSampleKind.get(sampleKindId)) {
                    if (chunk.getStartTime().isAfter(endTime) || chunk.getEndTime().isBefore(startTime)) {
                        continue;
                    }

                    chunkConsumer.processTimelineChunk(chunk);
                }
            }
        }
    }

    @Override
    public StartTimes getLastStartTimes() {
        return lastStartTimes.get();
    }

    @Override
    public Integer insertLastStartTimes(StartTimes startTimes) {
        lastStartTimes.set(startTimes);
        return 1;
    }

    @Override
    public void deleteLastStartTimes()
    {
        lastStartTimes.set(null);
    }

    @Override
    public void test() throws UnableToObtainConnectionException, CallbackFailedException
    {
    }

    public BiMap<Integer, TimelineChunk> getTimelineChunks()
    {
        return timelineChunks;
    }

    @Override
    public void bulkInsertEventCategories(List<String> categoryNames) throws UnableToObtainConnectionException, CallbackFailedException {
        for (String eventCategory : categoryNames) {
            getOrAddEventCategory(eventCategory);
        }
    }

    @Override
    public void bulkInsertHosts(List<String> hosts) throws UnableToObtainConnectionException, CallbackFailedException {
        for (String host : hosts) {
            getOrAddHost(host);
        }
    }

    @Override
    public void bulkInsertSampleKinds(List<CategoryIdAndSampleKind> categoryAndKinds) {
        for (CategoryIdAndSampleKind c : categoryAndKinds) {
            getOrAddSampleKind(0, c.getEventCategoryId(), c.getSampleKind());
        }
    }

    @Override
    public void bulkInsertTimelineChunks(List<TimelineChunk> timelineChunkList) {
        for (TimelineChunk chunk : timelineChunkList) {
            insertTimelineChunk(chunk);
        }
    }
}
