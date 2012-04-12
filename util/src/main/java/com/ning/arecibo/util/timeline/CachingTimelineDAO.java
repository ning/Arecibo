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

package com.ning.arecibo.util.timeline;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;

public class CachingTimelineDAO implements TimelineDAO
{
    private static final Logger log = LoggerFactory.getLogger(CachingTimelineDAO.class);

    private final BiMap<Integer, String> hostsCache;
    private final Map<Integer, Set<Integer>> hostIdsSampleKindIdsCache;
    private final BiMap<Integer, CategoryIdAndSampleKind> sampleKindsCache;
    private final BiMap<Integer, String> eventCategoriesCache;

    private final TimelineDAO delegate;

    public CachingTimelineDAO(final TimelineDAO delegate, final long maxNbHosts, final long maxNbEventCategories, final long maxNbSampleKinds)
    {
        this.delegate = delegate;
        hostsCache = delegate.getHosts();
        sampleKindsCache = delegate.getSampleKinds();
        eventCategoriesCache = delegate.getEventCategories();
        hostIdsSampleKindIdsCache = new HashMap<Integer, Set<Integer>>();
        for (HostIdAndSampleKindId both : delegate.getSampleKindIdsForAllHosts()) {
            final int hostId = both.getHostId();
            final int sampleKindId = both.getSampleKindId();
            Set<Integer> sampleKindIds = hostIdsSampleKindIdsCache.get(hostId);
            if (sampleKindIds == null) {
                sampleKindIds = new HashSet<Integer>();
                hostIdsSampleKindIdsCache.put(hostId, sampleKindIds);
            }
            sampleKindIds.add(sampleKindId);
        }
    }

    @Override
    public Integer getHostId(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return hostsCache.inverse().get(host);
    }

    @Override
    public String getHost(final Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return hostsCache.get(hostId);
    }

    @Override
    public BiMap<Integer, String> getHosts() throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getHosts();
    }

    @Override
    public synchronized Integer getOrAddHost(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        Integer hostId = hostsCache.inverse().get(host);
        if (hostId == null) {
            hostId = delegate.getOrAddHost(host);
            hostsCache.put(hostId, host);
        }

        return hostId;
    }

    @Override
    public Integer getEventCategoryId(String eventCategory) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return eventCategoriesCache.inverse().get(eventCategory);
    }

    @Override
    public String getEventCategory(Integer eventCategoryId) throws UnableToObtainConnectionException
    {
        return eventCategoriesCache.get(eventCategoryId);
    }

    @Override
    public BiMap<Integer, String> getEventCategories() throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getEventCategories();
    }

    @Override
    public Integer getOrAddEventCategory(String eventCategory) throws UnableToObtainConnectionException, CallbackFailedException {
        Integer eventCategoryId = eventCategoriesCache.inverse().get(eventCategory);
        if (eventCategoryId == null) {
            eventCategoryId = delegate.getOrAddEventCategory(eventCategory);
            eventCategoriesCache.put(eventCategoryId, eventCategory);
        }
        return eventCategoryId;
    }

    @Override
    public Integer getSampleKindId(final int eventCategoryId, final String sampleKind) throws UnableToObtainConnectionException
    {
        return sampleKindsCache.inverse().get(new CategoryIdAndSampleKind(eventCategoryId, sampleKind));
    }

    @Override
    public CategoryIdAndSampleKind getCategoryIdAndSampleKind(final Integer sampleKindId) throws UnableToObtainConnectionException
    {
        return sampleKindsCache.get(sampleKindId);
    }

    @Override
    public BiMap<Integer, CategoryIdAndSampleKind> getSampleKinds() throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getSampleKinds();
    }

    @Override
    public synchronized Integer getOrAddSampleKind(final Integer hostId, final Integer eventCategoryId, final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        final CategoryIdAndSampleKind categoryIdAndSampleKind = new CategoryIdAndSampleKind(eventCategoryId, sampleKind);
        Integer sampleKindId = sampleKindsCache.inverse().get(categoryIdAndSampleKind);
        if (sampleKindId == null) {
            sampleKindId = delegate.getOrAddSampleKind(hostId, eventCategoryId, sampleKind);
            sampleKindsCache.put(sampleKindId, categoryIdAndSampleKind);
        }
        if (hostId != null) {
            Set<Integer> sampleKindIds = hostIdsSampleKindIdsCache.get(hostId);
            if (sampleKindIds == null) {
                sampleKindIds = new HashSet<Integer>();
                hostIdsSampleKindIdsCache.put(hostId, sampleKindIds);
            }
            sampleKindIds.add(sampleKindId);
        }
        return sampleKindId;
    }

    @Override
    public Iterable<Integer> getSampleKindIdsByHostId(final Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return ImmutableList.copyOf(hostIdsSampleKindIdsCache.get(hostId));
    }

    @Override
    public Iterable<HostIdAndSampleKindId> getSampleKindIdsForAllHosts() throws UnableToObtainConnectionException, CallbackFailedException {
        return delegate.getSampleKindIdsForAllHosts();
    }


    @Override
    public Long insertTimelineChunk(final TimelineChunk timelineChunk) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.insertTimelineChunk(timelineChunk);
    }

    @Override
    public void getSamplesByHostIdsAndSampleKindIds(final List<Integer> hostIds, @Nullable final List<Integer> sampleKindIds,
                                                    final DateTime startTime, final DateTime endTime, final TimelineChunkConsumer chunkConsumer) throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.getSamplesByHostIdsAndSampleKindIds(hostIds, sampleKindIds, startTime, endTime, chunkConsumer);
    }

    @Override
    public Integer insertLastStartTimes(final StartTimes startTimes)
    {
        return delegate.insertLastStartTimes(startTimes);
    }

    @Override
    public StartTimes getLastStartTimes()
    {
        return delegate.getLastStartTimes();
    }

    @Override
    public void deleteLastStartTimes()
    {
        delegate.deleteLastStartTimes();
    }

    @Override
    public void bulkInsertEventCategories(List<String> categoryNames) throws UnableToObtainConnectionException, CallbackFailedException {
        delegate.bulkInsertEventCategories(categoryNames);
    }

    @Override
    public void bulkInsertHosts(List<String> hosts) throws UnableToObtainConnectionException, CallbackFailedException {
        delegate.bulkInsertHosts(hosts);
    }

    @Override
    public void bulkInsertSampleKinds(List<CategoryIdAndSampleKind> categoryAndKinds) {
        delegate.bulkInsertSampleKinds(categoryAndKinds);
    }

    @Override
    public List<Long> bulkInsertTimelineChunks(List<TimelineChunk> timelineChunkList) {
        return delegate.bulkInsertTimelineChunks(timelineChunkList);
    }

    @Override
    public void test() throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.test();
    }
}
