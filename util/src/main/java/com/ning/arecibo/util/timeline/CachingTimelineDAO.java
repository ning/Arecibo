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
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class CachingTimelineDAO implements TimelineDAO
{
    private static final Logger log = LoggerFactory.getLogger(CachingTimelineDAO.class);

    private final LoadingCache<Integer, String> hostsCache;
    private final LoadingCache<String, Integer> hostIdsCache;
    private final LoadingCache<String, Set<String>> hostsSampleKindsCache;
    private final LoadingCache<Integer, String> sampleKindsCache;
    private final LoadingCache<String, Integer> sampleKindIdsCache;

    private final TimelineDAO delegate;

    public CachingTimelineDAO(final TimelineDAO delegate, final long maxNbHosts, final long maxNbSampleKinds)
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

        hostsSampleKindsCache = CacheBuilder.newBuilder()
            .maximumSize(maxNbHosts)
            .removalListener(new RemovalListener<String, Set<String>>()
            {
                @Override
                public void onRemoval(final RemovalNotification<String, Set<String>> removedObjectNotification)
                {
                    final Set<String> sampleKinds = removedObjectNotification.getValue();
                    if (sampleKinds != null) {
                        log.info("{} was evicted from the hostsSampleKindsCache cache", sampleKinds);
                    }
                }
            })
            .build(new CacheLoader<String, Set<String>>()
            {
                @Override
                public Set<String> load(final String host) throws Exception
                {
                    log.info("Loading hostsSampleKindsCache cache for key {}", host);
                    return new HashSet<String>(ImmutableList.<String>copyOf(delegate.getSampleKindsByHostName(host)));
                }
            });

        sampleKindsCache = CacheBuilder.newBuilder()
            .maximumSize(maxNbSampleKinds)
            .removalListener(new RemovalListener<Integer, String>()
            {
                @Override
                public void onRemoval(final RemovalNotification<Integer, String> removedObjectNotification)
                {
                    final String sampleKind = removedObjectNotification.getValue();
                    if (sampleKind != null) {
                        log.info("{} was evicted from the sampleKinds cache", sampleKind);
                    }
                }
            })
            .build(new CacheLoader<Integer, String>()
            {
                @Override
                public String load(final Integer sampleKindId) throws Exception
                {
                    log.info("Loading sampleKinds cache for key {}", sampleKindId);
                    return delegate.getHost(sampleKindId);
                }
            });

        sampleKindIdsCache = CacheBuilder.newBuilder()
            .maximumSize(maxNbSampleKinds)
            .removalListener(new RemovalListener<String, Integer>()
            {
                @Override
                public void onRemoval(final RemovalNotification<String, Integer> removedObjectNotification)
                {
                    final Integer sampleKindId = removedObjectNotification.getValue();
                    if (sampleKindId != null) {
                        log.info("{} was evicted from the sampleKinds cache", sampleKindId);
                    }
                }
            })
            .build(new CacheLoader<String, Integer>()
            {
                @Override
                public Integer load(final String sampleKind) throws Exception
                {
                    log.info("Loading sampleKindIds cache for key {}", sampleKind);
                    return delegate.getHostId(sampleKind);
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
    public Integer getSampleKindId(final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        try {
            return sampleKindIdsCache.get(sampleKind);
        }
        catch (ExecutionException e) {
            throw new CallbackFailedException(e);
        }
    }

    @Override
    public String getSampleKind(final Integer sampleKindId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        try {
            return sampleKindsCache.get(sampleKindId);
        }
        catch (ExecutionException e) {
            throw new CallbackFailedException(e);
        }
    }

    @Override
    public BiMap<Integer, String> getSampleKinds() throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getSampleKinds();
    }

    @Override
    public synchronized Integer getOrAddSampleKind(final Integer hostId, final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        Integer sampleKindId = sampleKindIdsCache.getIfPresent(sampleKind);
        if (sampleKindId == null) {
            sampleKindId = delegate.getOrAddSampleKind(hostId, sampleKind);
            sampleKindIdsCache.put(sampleKind, sampleKindId);
        }

        hostsSampleKindsCache.getUnchecked(getHost(hostId)).add(sampleKind);

        return sampleKindId;
    }

    @Override
    public Iterable<String> getSampleKindsByHostName(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        try {
            return ImmutableList.copyOf(hostsSampleKindsCache.get(host));
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
    public List<TimelineChunkAndTimes> getSamplesByHostName(final String hostName, final DateTime startTime, final DateTime endTime) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getSamplesByHostName(hostName, startTime, endTime);
    }

    @Override
    public List<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(final String hostName, final String sampleKind, final DateTime startTime, final DateTime endTime) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getSamplesByHostNameAndSampleKind(hostName, sampleKind, startTime, endTime);
    }
}
