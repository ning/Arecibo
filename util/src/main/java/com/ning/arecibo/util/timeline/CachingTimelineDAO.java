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
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class CachingTimelineDAO implements TimelineDAO
{
    private static final Logger log = LoggerFactory.getLogger(CachingTimelineDAO.class);

    private final LoadingCache<Integer, String> hostsCache;
    private final LoadingCache<Integer, String> sampleKindsCache;

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
    public int addHost(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        final int hostId = delegate.addHost(host);
        hostsCache.put(hostId, host);
        return hostId;
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
    public int addSampleKind(final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        final int sampleKindId = delegate.addSampleKind(sampleKind);
        sampleKindsCache.put(sampleKindId, sampleKind);
        return sampleKindId;
    }

    @Override
    public BiMap<Integer, String> getSampleKinds() throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getSampleKinds();
    }

    @Override
    public int insertTimelineTimes(final TimelineTimes timelineTimes) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.insertTimelineTimes(timelineTimes);
    }

    @Override
    public int insertTimelineChunk(final TimelineChunk timelineChunk) throws UnableToObtainConnectionException, CallbackFailedException
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
