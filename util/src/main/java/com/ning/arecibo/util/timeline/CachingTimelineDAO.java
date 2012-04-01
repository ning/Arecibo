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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class CachingTimelineDAO implements TimelineDAO
{
    private static final Logger log = LoggerFactory.getLogger(CachingTimelineDAO.class);

    private final LoadingCache<Integer, String> hostsCache;
    private final LoadingCache<String, Integer> hostIdsCache;
    private final LoadingCache<Integer, Set<Integer>> hostIdsSampleKindIdsCache;
    private final LoadingCache<Integer, CategoryIdAndSampleKind> sampleKindsCache;
    private final LoadingCache<CategoryIdAndSampleKind, Integer> sampleKindIdsCache;
    private final LoadingCache<Integer, String> eventCategoriesCache;
    private final LoadingCache<String, Integer> eventCategoryIdsCache;

    private final TimelineDAO delegate;

    public CachingTimelineDAO(final TimelineDAO delegate, final long maxNbHosts, final long maxNbEventCategories, final long maxNbSampleKinds)
    {
        this.delegate = delegate;

        // Cache eviction is directed by the size only

        hostsCache = CacheBuilder.newBuilder()
            .maximumSize(maxNbHosts)
            .removalListener(new RemovalListener<Integer, String>()
            {
                @Override
                public void onRemoval(final RemovalNotification<Integer, String> removedObjectNotification)
                {
                    final String hostName = removedObjectNotification.getValue();
                    if (hostName != null) {
                        log.info("{} was evicted from the hosts cache", hostName);
                    }
                }
            })
            .build(new CacheLoader<Integer, String>()
            {
                @Override
                public String load(final Integer hostId) throws Exception
                {
                    log.info("Loading hosts cache for key {}", hostId);
                    return delegate.getHost(hostId);
                }
            });

        hostIdsCache = CacheBuilder.newBuilder()
            .maximumSize(maxNbHosts)
            .removalListener(new RemovalListener<String, Integer>()
            {
                @Override
                public void onRemoval(final RemovalNotification<String, Integer> removedObjectNotification)
                {
                    final Integer hostId = removedObjectNotification.getValue();
                    if (hostId != null) {
                        log.info("{} was evicted from the hostIds cache", hostId);
                    }
                }
            })
            .build(new CacheLoader<String, Integer>()
            {
                @Override
                public Integer load(final String host) throws Exception
                {
                    log.info("Loading hostIds cache for key {}", host);
                    return delegate.getHostId(host);
                }
            });

        hostIdsSampleKindIdsCache = CacheBuilder.newBuilder()
            .maximumSize(maxNbHosts)
            .removalListener(new RemovalListener<Integer, Set<Integer>>()
            {
                @Override
                public void onRemoval(final RemovalNotification<Integer, Set<Integer>> removedObjectNotification)
                {
                    final Set<Integer> sampleKindIds = removedObjectNotification.getValue();
                    if (sampleKindIds != null) {
                        log.info("{} was evicted from the hostIdsSampleKindIdsCache cache", sampleKindIds);
                    }
                }
            })
            .build(new CacheLoader<Integer, Set<Integer>>()
            {
                @Override
                public Set<Integer> load(final Integer hostId) throws Exception
                {
                    log.info("Loading hostIdsSampleKindIdsCache cache for key {}", hostId);
                    return new HashSet<Integer>(ImmutableList.<Integer>copyOf(delegate.getSampleKindIdsByHostId(hostId)));
                }
            });

        sampleKindsCache = CacheBuilder.newBuilder()
            .maximumSize(maxNbSampleKinds)
            .removalListener(new RemovalListener<Integer, CategoryIdAndSampleKind>()
            {
                @Override
                public void onRemoval(final RemovalNotification<Integer, CategoryIdAndSampleKind> removedObjectNotification)
                {
                    final CategoryIdAndSampleKind categoryIdAndSampleKind = removedObjectNotification.getValue();
                    if (categoryIdAndSampleKind != null) {
                        log.info("Event category id {} and sample kind {} was evicted from the sampleKinds cache",
                                categoryIdAndSampleKind.getEventCategoryId(), categoryIdAndSampleKind.getSampleKind());
                    }
                }
            })
            .build(new CacheLoader<Integer, CategoryIdAndSampleKind>()
            {
                @Override
                public CategoryIdAndSampleKind load(final Integer sampleKindId) throws Exception
                {
                    log.info("Loading sampleKinds cache for key {}", sampleKindId);
                    return delegate.getCategoryIdAndSampleKind(sampleKindId);
                }
            });

        sampleKindIdsCache = CacheBuilder.newBuilder()
            .maximumSize(maxNbSampleKinds)
            .removalListener(new RemovalListener<CategoryIdAndSampleKind, Integer>()
            {
                @Override
                public void onRemoval(final RemovalNotification<CategoryIdAndSampleKind, Integer> removedObjectNotification)
                {
                    final Integer sampleKindId = removedObjectNotification.getValue();
                    if (sampleKindId != null) {
                        log.info("{} was evicted from the sampleKinds cache", sampleKindId);
                    }
                }
            })
            .build(new CacheLoader<CategoryIdAndSampleKind, Integer>()
            {
                @Override
                public Integer load(final CategoryIdAndSampleKind categoryIdAndSampleKind) throws Exception
                {
                    log.info("Loading sampleKindIds cache for event category id {}, sample kind {}",
                            categoryIdAndSampleKind.getEventCategoryId(), categoryIdAndSampleKind.getSampleKind());
                    return delegate.getSampleKindId(categoryIdAndSampleKind.getEventCategoryId(), categoryIdAndSampleKind.getSampleKind());
                }
            });

    eventCategoriesCache = CacheBuilder.newBuilder()
        .maximumSize(maxNbEventCategories)
        .removalListener(new RemovalListener<Integer, String>()
        {
            @Override
            public void onRemoval(final RemovalNotification<Integer, String> removedObjectNotification)
            {
                final String eventCategory = removedObjectNotification.getValue();
                if (eventCategory != null) {
                    log.info("{} was evicted from the eventCategories cache", eventCategory);
                }
            }
        })
        .build(new CacheLoader<Integer, String>()
        {
            @Override
            public String load(final Integer eventCategoryId) throws Exception
            {
                log.info("Loading eventCategories cache for key {}", eventCategoryId);
                return delegate.getEventCategory(eventCategoryId);
            }
        });

    eventCategoryIdsCache = CacheBuilder.newBuilder()
        .maximumSize(maxNbEventCategories)
        .removalListener(new RemovalListener<String, Integer>()
        {
            @Override
            public void onRemoval(final RemovalNotification<String, Integer> removedObjectNotification)
            {
                final Integer eventCategoryId = removedObjectNotification.getValue();
                if (eventCategoryId != null) {
                    log.info("{} was evicted from the eventCategories cache", eventCategoryId);
                }
            }
        })
        .build(new CacheLoader<String, Integer>()
        {
            @Override
            public Integer load(final String eventCategory) throws Exception
            {
                log.info("Loading eventCategoryIds cache for key {}", eventCategory);
                return delegate.getEventCategoryId(eventCategory);
            }
        });
    }

    @Override
    public Integer getHostId(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        try {
            return hostIdsCache.get(host);
        }
        catch (ExecutionException e) {
            throw new CallbackFailedException(e);
        }
    }

    @Override
    public String getHost(final Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        try {
            return hostsCache.get(hostId);
        }
        catch (ExecutionException e) {
            throw new CallbackFailedException(e);
        }
    }

    @Override
    public BiMap<Integer, String> getHosts() throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getHosts();
    }

    @Override
    public synchronized Integer getOrAddHost(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        Integer hostId = hostIdsCache.getIfPresent(host);
        if (hostId == null) {
            hostId = delegate.getOrAddHost(host);
            hostIdsCache.put(host, hostId);
        }

        return hostId;
    }

    @Override
    public Integer getEventCategoryId(String eventCategory) throws UnableToObtainConnectionException, CallbackFailedException
    {
        try {
            return eventCategoryIdsCache.get(eventCategory);
        }
        catch (ExecutionException e) {
            throw new CallbackFailedException(e);
        }
    }

    @Override
    public String getEventCategory(Integer eventCategoryId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        try {
            return eventCategoriesCache.get(eventCategoryId);
        }
        catch (ExecutionException e) {
            throw new CallbackFailedException(e);
        }
    }

    @Override
    public BiMap<Integer, String> getEventCategories() throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getEventCategories();
    }

    @Override
    public Integer getOrAddEventCategory(String eventCategory) throws UnableToObtainConnectionException, CallbackFailedException {
        Integer eventCategoryId = eventCategoryIdsCache.getIfPresent(eventCategory);
        if (eventCategoryId == null) {
            eventCategoryId = delegate.getOrAddEventCategory(eventCategory);
            eventCategoryIdsCache.put(eventCategory, eventCategoryId);
        }
        return eventCategoryId;
    }

    @Override
    public Integer getSampleKindId(final int eventCategoryId, final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        try {
            return sampleKindIdsCache.get(new CategoryIdAndSampleKind(eventCategoryId, sampleKind));
        }
        catch (ExecutionException e) {
            throw new CallbackFailedException(e);
        }
    }

    @Override
    public CategoryIdAndSampleKind getCategoryIdAndSampleKind(final Integer sampleKindId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        try {
            return sampleKindsCache.get(sampleKindId);
        }
        catch (ExecutionException e) {
            throw new CallbackFailedException(e);
        }
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
        Integer sampleKindId = sampleKindIdsCache.getIfPresent(categoryIdAndSampleKind);
        if (sampleKindId == null) {
            sampleKindId = delegate.getOrAddSampleKind(hostId, eventCategoryId, sampleKind);
            sampleKindIdsCache.put(categoryIdAndSampleKind, sampleKindId);
        }

        hostIdsSampleKindIdsCache.getUnchecked(hostId).add(sampleKindId);

        return sampleKindId;
    }
    @Override
    public Iterable<Integer> getSampleKindIdsByHostId(final Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        try {
            return ImmutableList.copyOf(hostIdsSampleKindIdsCache.get(hostId));
        }
        catch (ExecutionException e) {
            throw new CallbackFailedException(e);
        }
    }

    @Override
    public Integer insertTimelineTimes(final TimelineTimes timelineTimes) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.insertTimelineTimes(timelineTimes);
    }

    @Override
    public Integer insertTimelineChunk(final TimelineChunk timelineChunk) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.insertTimelineChunk(timelineChunk);
    }

    @Override
    public void getSamplesByHostIdsAndSampleKindIds(final List<Integer> hostIds, @Nullable final List<Integer> sampleKindIds,
                                                    final DateTime startTime, final DateTime endTime, final TimelineChunkAndTimesConsumer chunkConsumer) throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.getSamplesByHostIdsAndSampleKindIds(hostIds, sampleKindIds, startTime, endTime, chunkConsumer);
    }

    @Override
    public void test() throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.test();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have returned a cached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostsCacheHitCount()
    {
        return hostsCache.stats().hitCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were hits", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getHostsCacheHitRate()
    {
        return hostsCache.stats().hitRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have returned an uncached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostsCacheMissCount()
    {
        return hostsCache.stats().missCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were misses", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getHostsCacheMissRate()
    {
        return hostsCache.stats().missRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have successfully loaded a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostsCacheLoadSuccessCount()
    {
        return hostsCache.stats().loadSuccessCount();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods threw an exception while loading a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostsCacheLoadExceptionCount()
    {
        return hostsCache.stats().loadExceptionCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache loading attempts which threw exceptions", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getHostsCacheLoadExceptionRate()
    {
        return hostsCache.stats().loadExceptionRate();
    }

    @MonitorableManaged(description = "Returns the total number of nanoseconds the cache has spent loading new values", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostsCacheTotalLoadTime()
    {
        return hostsCache.stats().totalLoadTime();
    }

    @MonitorableManaged(description = "Returns the average time spent loading new values", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getHostsCacheAverageLoadPenalty()
    {
        return hostsCache.stats().averageLoadPenalty();
    }

    @MonitorableManaged(description = "Returns the number of times an entry has been evicted", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostsCacheEvictionCount()
    {
        return hostsCache.stats().evictionCount();
    }

    @MonitorableManaged(description = "Returns the number of times Cache lookup methods have returned a cached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostIdsCacheHitCount()
    {
        return hostIdsCache.stats().hitCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were hits", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getHostIdsCacheHitRate()
    {
        return hostIdsCache.stats().hitRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have returned an uncached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostIdsCacheMissCount()
    {
        return hostIdsCache.stats().missCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were misses", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getHostIdsCacheMissRate()
    {
        return hostIdsCache.stats().missRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have successfully loaded a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostIdsCacheLoadSuccessCount()
    {
        return hostIdsCache.stats().loadSuccessCount();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods threw an exception while loading a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostIdsCacheLoadExceptionCount()
    {
        return hostIdsCache.stats().loadExceptionCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache loading attempts which threw exceptions", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getHostIdsCacheLoadExceptionRate()
    {
        return hostIdsCache.stats().loadExceptionRate();
    }

    @MonitorableManaged(description = "Returns the total number of nanoseconds the cache has spent loading new values", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostIdsCacheTotalLoadTime()
    {
        return hostIdsCache.stats().totalLoadTime();
    }

    @MonitorableManaged(description = "Returns the average time spent loading new values", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getHostIdsCacheAverageLoadPenalty()
    {
        return hostIdsCache.stats().averageLoadPenalty();
    }

    @MonitorableManaged(description = "Returns the number of times an entry has been evicted", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostIdsCacheEvictionCount()
    {
        return hostIdsCache.stats().evictionCount();
    }

    @MonitorableManaged(description = "Returns the number of times Cache lookup methods have returned a cached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostsSampleKindsCacheHitCount()
    {
        return hostIdsSampleKindIdsCache.stats().hitCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were hits", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getHostsSampleKindsCacheHitRate()
    {
        return hostIdsSampleKindIdsCache.stats().hitRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have returned an uncached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostsSampleKindsCacheMissCount()
    {
        return hostIdsSampleKindIdsCache.stats().missCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were misses", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getHostsSampleKindsCacheMissRate()
    {
        return hostIdsSampleKindIdsCache.stats().missRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have successfully loaded a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostsSampleKindsCacheLoadSuccessCount()
    {
        return hostIdsSampleKindIdsCache.stats().loadSuccessCount();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods threw an exception while loading a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostsSampleKindsCacheLoadExceptionCount()
    {
        return hostIdsSampleKindIdsCache.stats().loadExceptionCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache loading attempts which threw exceptions", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getHostsSampleKindsCacheLoadExceptionRate()
    {
        return hostIdsSampleKindIdsCache.stats().loadExceptionRate();
    }

    @MonitorableManaged(description = "Returns the total number of nanoseconds the cache has spent loading new values", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostsSampleKindsCacheTotalLoadTime()
    {
        return hostIdsSampleKindIdsCache.stats().totalLoadTime();
    }

    @MonitorableManaged(description = "Returns the average time spent loading new values", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getHostsSampleKindsCacheAverageLoadPenalty()
    {
        return hostIdsSampleKindIdsCache.stats().averageLoadPenalty();
    }

    @MonitorableManaged(description = "Returns the number of times an entry has been evicted", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getHostsSampleKindsCacheEvictionCount()
    {
        return hostIdsSampleKindIdsCache.stats().evictionCount();
    }

    @MonitorableManaged(description = "Returns the number of times Cache lookup methods have returned a cached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventCategoriesCacheHitCount()
    {
        return eventCategoriesCache.stats().hitCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were hits", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getEventCategoriesCacheHitRate()
    {
        return eventCategoriesCache.stats().hitRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have returned an uncached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventCategoriesCacheMissCount()
    {
        return eventCategoriesCache.stats().missCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were misses", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getEventCategoriesCacheMissRate()
    {
        return eventCategoriesCache.stats().missRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have successfully loaded a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventCategoriesCacheLoadSuccessCount()
    {
        return eventCategoriesCache.stats().loadSuccessCount();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods threw an exception while loading a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventCategoriesCacheLoadExceptionCount()
    {
        return eventCategoriesCache.stats().loadExceptionCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache loading attempts which threw exceptions", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getEventCategoriesCacheLoadExceptionRate()
    {
        return eventCategoriesCache.stats().loadExceptionRate();
    }

    @MonitorableManaged(description = "Returns the total number of nanoseconds the cache has spent loading new values", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventCategoriesCacheTotalLoadTime()
    {
        return eventCategoriesCache.stats().totalLoadTime();
    }

    @MonitorableManaged(description = "Returns the average time spent loading new values", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getEventCategoriesCacheAverageLoadPenalty()
    {
        return eventCategoriesCache.stats().averageLoadPenalty();
    }

    @MonitorableManaged(description = "Returns the number of times an entry has been evicted", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventCategoriesCacheEvictionCount()
    {
        return eventCategoriesCache.stats().evictionCount();
    }

    @MonitorableManaged(description = "Returns the number of times Cache lookup methods have returned a cached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventCategoryIdsCacheHitCount()
    {
        return eventCategoryIdsCache.stats().hitCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were hits", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getEventCategoryIdsCacheHitRate()
    {
        return eventCategoryIdsCache.stats().hitRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have returned an uncached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventCategoryIdsCacheMissCount()
    {
        return eventCategoryIdsCache.stats().missCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were misses", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getEventCategoryIdsCacheMissRate()
    {
        return eventCategoryIdsCache.stats().missRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have successfully loaded a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventCategoryIdsCacheLoadSuccessCount()
    {
        return eventCategoryIdsCache.stats().loadSuccessCount();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods threw an exception while loading a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventCategoryIdsCacheLoadExceptionCount()
    {
        return eventCategoryIdsCache.stats().loadExceptionCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache loading attempts which threw exceptions", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getEventCategoryIdsCacheLoadExceptionRate()
    {
        return eventCategoryIdsCache.stats().loadExceptionRate();
    }

    @MonitorableManaged(description = "Returns the total number of nanoseconds the cache has spent loading new values", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventCategoryIdsCacheTotalLoadTime()
    {
        return eventCategoryIdsCache.stats().totalLoadTime();
    }

    @MonitorableManaged(description = "Returns the average time spent loading new values", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getEventCategoryIdsCacheAverageLoadPenalty()
    {
        return eventCategoryIdsCache.stats().averageLoadPenalty();
    }

    @MonitorableManaged(description = "Returns the number of times an entry has been evicted", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventCategoryIdsCacheEvictionCount()
    {
        return eventCategoryIdsCache.stats().evictionCount();
    }

    @MonitorableManaged(description = "Returns the number of times Cache lookup methods have returned a cached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getSampleKindsCacheHitCount()
    {
        return sampleKindsCache.stats().hitCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were hits", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getSampleKindsCacheHitRate()
    {
        return sampleKindsCache.stats().hitRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have returned an uncached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getSampleKindsCacheMissCount()
    {
        return sampleKindsCache.stats().missCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were misses", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getSampleKindsCacheMissRate()
    {
        return sampleKindsCache.stats().missRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have successfully loaded a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getSampleKindsCacheLoadSuccessCount()
    {
        return sampleKindsCache.stats().loadSuccessCount();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods threw an exception while loading a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getSampleKindsCacheLoadExceptionCount()
    {
        return sampleKindsCache.stats().loadExceptionCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache loading attempts which threw exceptions", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getSampleKindsCacheLoadExceptionRate()
    {
        return sampleKindsCache.stats().loadExceptionRate();
    }

    @MonitorableManaged(description = "Returns the total number of nanoseconds the cache has spent loading new values", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getSampleKindsCacheTotalLoadTime()
    {
        return sampleKindsCache.stats().totalLoadTime();
    }

    @MonitorableManaged(description = "Returns the average time spent loading new values", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getSampleKindsCacheAverageLoadPenalty()
    {
        return sampleKindsCache.stats().averageLoadPenalty();
    }

    @MonitorableManaged(description = "Returns the number of times an entry has been evicted", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getSampleKindsCacheEvictionCount()
    {
        return sampleKindsCache.stats().evictionCount();
    }

    @MonitorableManaged(description = "Returns the number of times Cache lookup methods have returned a cached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getSampleKindIdsCacheHitCount()
    {
        return sampleKindIdsCache.stats().hitCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were hits", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getSampleKindIdsCacheHitRate()
    {
        return sampleKindIdsCache.stats().hitRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have returned an uncached value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getSampleKindIdsCacheMissCount()
    {
        return sampleKindIdsCache.stats().missCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache requests which were misses", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getSampleKindIdsCacheMissRate()
    {
        return sampleKindIdsCache.stats().missRate();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods have successfully loaded a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getSampleKindIdsCacheLoadSuccessCount()
    {
        return sampleKindIdsCache.stats().loadSuccessCount();
    }

    @MonitorableManaged(description = "Returns the number of times cache lookup methods threw an exception while loading a new value", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getSampleKindIdsCacheLoadExceptionCount()
    {
        return sampleKindIdsCache.stats().loadExceptionCount();
    }

    @MonitorableManaged(description = "Returns the ratio of cache loading attempts which threw exceptions", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getSampleKindIdsCacheLoadExceptionRate()
    {
        return sampleKindIdsCache.stats().loadExceptionRate();
    }

    @MonitorableManaged(description = "Returns the total number of nanoseconds the cache has spent loading new values", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getSampleKindIdsCacheTotalLoadTime()
    {
        return sampleKindIdsCache.stats().totalLoadTime();
    }

    @MonitorableManaged(description = "Returns the average time spent loading new values", monitored = true, monitoringType = {MonitoringType.VALUE})
    public double getSampleKindIdsCacheAverageLoadPenalty()
    {
        return sampleKindIdsCache.stats().averageLoadPenalty();
    }

    @MonitorableManaged(description = "Returns the number of times an entry has been evicted", monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getSampleKindIdsCacheEvictionCount()
    {
        return sampleKindIdsCache.stats().evictionCount();
    }
}
