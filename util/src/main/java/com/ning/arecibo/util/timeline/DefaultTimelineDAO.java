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

import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.ning.arecibo.util.timeline.persistent.TimelineDAOQueries;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.ResultIterator;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;
import org.skife.jdbi.v2.sqlobject.stringtemplate.StringTemplate3StatementLocator;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class DefaultTimelineDAO implements TimelineDAO
{
    private static final Logger log = LoggerFactory.getLogger(DefaultTimelineDAO.class);
    private static final Joiner JOINER = Joiner.on(",");

    final IDBI dbi;
    final TimelineDAOQueries delegate;

    @Inject
    public DefaultTimelineDAO(final IDBI dbi)
    {
        this.dbi = dbi;
        this.delegate = dbi.onDemand(TimelineDAOQueries.class);
    }

    @Override
    public String getHost(final Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getHost(hostId);
    }

    @Override
    public Integer getHostId(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getHostId(host);
    }

    @Override
    public BiMap<Integer, String> getHosts() throws UnableToObtainConnectionException, CallbackFailedException
    {
        final HashBiMap<Integer, String> accumulator = HashBiMap.create();
        for (final Map<String, Object> sampleKind : delegate.getHosts()) {
            accumulator.put(Integer.valueOf(sampleKind.get("host_id").toString()), sampleKind.get("host_name").toString());
        }
        return accumulator;
    }

    @Override
    public synchronized Integer getOrAddHost(final String host) throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.begin();
        delegate.addHost(host);
        final Integer hostId = delegate.getHostId(host);
        delegate.commit();

        return hostId;
    }

    @Override
    public Integer getSampleKindId(final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getSampleKindId(sampleKind);
    }

    @Override
    public String getSampleKind(final Integer sampleKindId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getSampleKind(sampleKindId);
    }

    @Override
    public BiMap<Integer, String> getSampleKinds() throws UnableToObtainConnectionException, CallbackFailedException
    {
        final HashBiMap<Integer, String> accumulator = HashBiMap.create();
        for (final Map<String, Object> sampleKind : delegate.getSampleKinds()) {
            accumulator.put(Integer.valueOf(sampleKind.get("sample_kind_id").toString()), sampleKind.get("sample_kind").toString());
        }
        return accumulator;
    }

    @Override
    public synchronized Integer getOrAddSampleKind(final Integer hostId, final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.begin();
        delegate.addSampleKind(sampleKind);
        final Integer sampleKindId = delegate.getSampleKindId(sampleKind);
        delegate.commit();

        return sampleKindId;
    }

    @Override
    public Iterable<Integer> getSampleKindIdsByHostId(final Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getSampleKindIdsByHostId(hostId);
    }

    @Override
    public synchronized Integer insertTimelineTimes(final TimelineTimes timelineTimes) throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.begin();
        delegate.insertTimelineTimes(timelineTimes);
        final Integer timelineTimesId = delegate.getLastInsertedId();
        delegate.commit();

        return timelineTimesId;
    }

    @Override
    public synchronized Integer insertTimelineChunk(final TimelineChunk timelineChunk) throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.begin();
        delegate.insertTimelineChunk(timelineChunk);
        final Integer timelineChunkId = delegate.getLastInsertedId();
        delegate.commit();

        return timelineChunkId;
    }

    @Override
    public void getSamplesByHostIdsAndSampleKindIds(final List<Integer> hostIdList,
                                                    @Nullable final List<Integer> sampleKindIdList,
                                                    final DateTime startTime,
                                                    final DateTime endTime,
                                                    final TimelineChunkAndTimesConsumer chunkConsumer)
    {
        dbi.withHandle(new HandleCallback<Void>()
        {
            @Override
            public Void withHandle(final Handle handle) throws Exception
            {
                handle.setStatementLocator(new StringTemplate3StatementLocator(TimelineDAOQueries.class));

                ResultIterator<TimelineChunkAndTimes> iterator = null;
                try {
                    final Query<Map<String, Object>> query = handle
                        .createQuery("getSamplesByHostIdsAndSampleKindIds")
                        .bind("startTime", TimelineTimes.unixSeconds(startTime))
                        .bind("endTime", TimelineTimes.unixSeconds(endTime))
                        .define("hostIds", JOINER.join(hostIdList));

                    if (sampleKindIdList != null) {
                        query.define("sampleKindIds", JOINER.join(sampleKindIdList));
                    }

                    iterator = query
                        .map(TimelineChunkAndTimes.mapper)
                        .iterator();

                    while (iterator.hasNext()) {
                        chunkConsumer.processTimelineChunkAndTimes(iterator.next());
                    }
                    return null;
                }
                finally {
                    if (iterator != null) {
                        try {
                            iterator.close();
                        }
                        catch (Exception e) {
                            log.error("Exception closing TimelineChunkAndTimes iterator for hostIds %s and sampleKindIds %s", hostIdList, sampleKindIdList);
                        }
                    }
                }
            }
        });

    }

    @Override
    public void test() throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.test();
    }
}
