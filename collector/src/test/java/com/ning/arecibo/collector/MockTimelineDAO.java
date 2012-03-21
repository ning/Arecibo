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
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimesConsumer;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.TimelineTimes;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MockTimelineDAO implements TimelineDAO
{
    private final BiMap<Integer, String> hosts = HashBiMap.create();
    private final BiMap<Integer, String> sampleKinds = HashBiMap.create();
    private final BiMap<Integer, TimelineTimes> timelineTimes = HashBiMap.create();
    private final BiMap<Integer, TimelineChunk> timelineChunks = HashBiMap.create();
    private final Multimap<String, String> hostSampleKinds = HashMultimap.create();
    private final Map<Integer, Map<Integer, List<TimelineChunkAndTimes>>> samplesPerHostAndSampleKind = new HashMap<Integer, Map<Integer, List<TimelineChunkAndTimes>>>();

    @Override
    public Integer getHostId(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return hosts.inverse().get(host);
    }

    @Override
    public String getHost(final Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return hosts.get(hostId);
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
    public Integer getSampleKindId(final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return sampleKinds.inverse().get(sampleKind);
    }

    @Override
    public String getSampleKind(final Integer sampleKindId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return sampleKinds.get(sampleKindId);
    }

    @Override
    public BiMap<Integer, String> getSampleKinds()
    {
        return sampleKinds;
    }

    @Override
    public Integer getOrAddSampleKind(final Integer hostId, final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        synchronized (sampleKinds) {
            final Integer sampleKindId = getSampleKindId(sampleKind);
            if (sampleKindId == null) {
                sampleKinds.put(sampleKinds.size(), sampleKind);
                hostSampleKinds.put(getHost(hostId), sampleKind);
                return sampleKinds.size() - 1;
            }
            else {
                return sampleKindId;
            }
        }
    }

    @Override
    public Iterable<String> getSampleKindsByHostName(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return hostSampleKinds.get(host);
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

            final String hostName = getHost(chunk.getHostId());
            final String sampleKind = getSampleKind(chunk.getSampleKindId());
            chunkAndTimes.add(new TimelineChunkAndTimes(hostName, sampleKind, chunk, timelineTimes.get(chunk.getTimelineTimesId())));
            samplesPerSampleKind.put(chunk.getSampleKindId(), chunkAndTimes);

            samplesPerHostAndSampleKind.put(chunk.getHostId(), samplesPerSampleKind);
        }

        return timelineChunkId;
    }

    @Override
    public void getSamplesByHostNamesAndSampleKinds(final List<String> hostNames, final List<String> sampleKinds, final DateTime startTime, final DateTime endTime, final TimelineChunkAndTimesConsumer chunkConsumer) throws UnableToObtainConnectionException, CallbackFailedException
    {
        for (final Integer hostId : samplesPerHostAndSampleKind.keySet()) {
            final String hostName = getHost(hostId);
            if (hostNames.indexOf(hostName) == -1) {
                continue;
            }

            final Map<Integer, List<TimelineChunkAndTimes>> samplesPerSampleKind = samplesPerHostAndSampleKind.get(hostId);
            for (final Integer sampleKindId : samplesPerSampleKind.keySet()) {
                final String sampleKind = getSampleKind(sampleKindId);
                if (sampleKinds.indexOf(sampleKind) == -1) {
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

    public BiMap<Integer, TimelineChunk> getTimelineChunks()
    {
        return timelineChunks;
    }
}
