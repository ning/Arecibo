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
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimesConsumer;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.TimelineTimes;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MockTimelineDAO implements TimelineDAO
{
    private final BiMap<Integer, String> hosts = HashBiMap.create();
    private final BiMap<Integer, CategoryIdAndSampleKind> sampleKinds = HashBiMap.create();
    private final BiMap<Integer, String> eventCategories = HashBiMap.create();
    private final BiMap<Integer, TimelineTimes> timelineTimes = HashBiMap.create();
    private final BiMap<Integer, TimelineChunk> timelineChunks = HashBiMap.create();
    private final Multimap<Integer, Integer> hostSampleKindIds = HashMultimap.create();
    private final Map<Integer, Map<Integer, List<TimelineChunkAndTimes>>> samplesPerHostAndSampleKind = new HashMap<Integer, Map<Integer, List<TimelineChunkAndTimes>>>();

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
    public Integer getOrAddHost(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        synchronized (hosts) {
            final Integer hostId = getHostId(host);
            if (hostId == null) {
                hosts.put(hosts.size(), host);
                return hosts.size() - 1;
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
    public Integer getOrAddEventCategory( String eventCategory) throws UnableToObtainConnectionException, CallbackFailedException {
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
    public Integer getOrAddSampleKind(final Integer hostId, final Integer eventCategoryId, final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        synchronized (sampleKinds) {
            Integer sampleKindId = getSampleKindId(eventCategoryId, sampleKind);
            if (sampleKindId == null) {
                sampleKindId = sampleKinds.size() + 1;
                sampleKinds.put(sampleKindId, new CategoryIdAndSampleKind(eventCategoryId, sampleKind));
            }

            hostSampleKindIds.put(hostId, sampleKindId);
            return sampleKindId;
        }
    }

    @Override
    public Iterable<Integer> getSampleKindIdsByHostId(final Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        synchronized (sampleKinds) {
            return hostSampleKindIds.get(hostId);
        }
    }

    @Override
    public Integer insertTimelineTimes(final TimelineTimes timeline)
    {
        synchronized (timelineTimes) {
            timelineTimes.put(timelineTimes.size(), timeline);
            return timelineTimes.size() - 1;
        }
    }

    @Override
    public Integer insertTimelineChunk(final TimelineChunk chunk)
    {
        final Integer timelineChunkId;
        synchronized (timelineChunks) {
            timelineChunks.put(timelineChunks.size(), chunk);
            timelineChunkId = timelineChunks.size() - 1;
        }

        synchronized (samplesPerHostAndSampleKind) {
            Map<Integer, List<TimelineChunkAndTimes>> samplesPerSampleKind = samplesPerHostAndSampleKind.get(chunk.getHostId());
            if (samplesPerSampleKind == null) {
                samplesPerSampleKind = new HashMap<Integer, List<TimelineChunkAndTimes>>();
            }

            List<TimelineChunkAndTimes> chunkAndTimes = samplesPerSampleKind.get(chunk.getSampleKindId());
            if (chunkAndTimes == null) {
                chunkAndTimes = new ArrayList<TimelineChunkAndTimes>();
            }

            chunkAndTimes.add(new TimelineChunkAndTimes(chunk.getHostId(), chunk.getSampleKindId(), chunk, timelineTimes.get(chunk.getTimelineTimesId())));
            samplesPerSampleKind.put(chunk.getSampleKindId(), chunkAndTimes);

            samplesPerHostAndSampleKind.put(chunk.getHostId(), samplesPerSampleKind);
        }

        return timelineChunkId;
    }

    @Override
    public void getSamplesByHostIdsAndSampleKindIds(final List<Integer> hostIds, @Nullable final List<Integer> sampleKindIds, final DateTime startTime, final DateTime endTime, final TimelineChunkAndTimesConsumer chunkConsumer) throws UnableToObtainConnectionException, CallbackFailedException
    {
        for (final Integer hostId : samplesPerHostAndSampleKind.keySet()) {
            if (hostIds.indexOf(hostId) == -1) {
                continue;
            }

            final Map<Integer, List<TimelineChunkAndTimes>> samplesPerSampleKind = samplesPerHostAndSampleKind.get(hostId);
            for (final Integer sampleKindId : samplesPerSampleKind.keySet()) {
                if (sampleKindIds != null && sampleKindIds.indexOf(sampleKindId) == -1) {
                    continue;
                }

                for (final TimelineChunkAndTimes chunkAndTimes : samplesPerSampleKind.get(sampleKindId)) {
                    if (chunkAndTimes.getTimelineTimes().getStartTime().isAfter(endTime) || chunkAndTimes.getTimelineTimes().getEndTime().isBefore(startTime)) {
                        continue;
                    }

                    chunkConsumer.processTimelineChunkAndTimes(chunkAndTimes);
                }
            }
        }
    }

    @Override
    public void test() throws UnableToObtainConnectionException, CallbackFailedException
    {
    }

    public BiMap<Integer, TimelineChunk> getTimelineChunks()
    {
        return timelineChunks;
    }
}
