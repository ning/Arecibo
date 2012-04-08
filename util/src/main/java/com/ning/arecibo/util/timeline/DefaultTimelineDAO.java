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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

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

import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.ning.arecibo.util.timeline.persistent.TimelineDAOQueries;

public class DefaultTimelineDAO implements TimelineDAO
{
    private static final Logger log = LoggerFactory.getLogger(DefaultTimelineDAO.class);
    private static final Joiner JOINER = Joiner.on(",");
    private static final TimelineChunkMapper timelineChunkMapper = new TimelineChunkMapper();

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
    public Integer getEventCategoryId(String eventCategory) throws UnableToObtainConnectionException, CallbackFailedException {
        return delegate.getEventCategoryId(eventCategory);
    }

    @Override
    public String getEventCategory(Integer eventCategoryId) throws UnableToObtainConnectionException, CallbackFailedException {
        return delegate.getEventCategory(eventCategoryId);
    }

    @Override
    public BiMap<Integer, String> getEventCategories() throws UnableToObtainConnectionException, CallbackFailedException {
        final HashBiMap<Integer, String> accumulator = HashBiMap.create();
        for (final Map<String, Object> eventCategory : delegate.getEventCategories()) {
            accumulator.put(Integer.valueOf(eventCategory.get("event_category_id").toString()), eventCategory.get("event_category").toString());
        }
        return accumulator;
    }

    @Override
    public synchronized Integer getOrAddEventCategory(String eventCategory) throws UnableToObtainConnectionException, CallbackFailedException {
        delegate.begin();
        delegate.addEventCategory(eventCategory);
        final Integer eventCategoryId = delegate.getEventCategoryId(eventCategory);
        delegate.commit();

        return eventCategoryId;
    }

    @Override
    public Integer getSampleKindId(final int eventCategoryId, final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getSampleKindId(eventCategoryId, sampleKind);
    }

    @Override
    public CategoryIdAndSampleKind getCategoryIdAndSampleKind(final Integer sampleKindId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getEventCategoryIdAndSampleKind(sampleKindId);
    }

    @Override
    public BiMap<Integer, CategoryIdAndSampleKind> getSampleKinds() throws UnableToObtainConnectionException, CallbackFailedException
    {
        final HashBiMap<Integer, CategoryIdAndSampleKind> accumulator = HashBiMap.create();
        for (final Map<String, Object> sampleKindInfo : delegate.getSampleKinds()) {
            accumulator.put(Integer.valueOf(sampleKindInfo.get("sample_kind_id").toString()),
                    new CategoryIdAndSampleKind((Integer)sampleKindInfo.get("event_category_id"), sampleKindInfo.get("sample_kind").toString()));
        }
        return accumulator;
    }

    @Override
    public synchronized Integer getOrAddSampleKind(final Integer hostId, final Integer eventCategoryId, final String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.begin();
        delegate.addSampleKind(eventCategoryId, sampleKind);
        final Integer sampleKindId = delegate.getSampleKindId(eventCategoryId, sampleKind);
        delegate.commit();

        return sampleKindId;
    }

    @Override
    public Iterable<Integer> getSampleKindIdsByHostId(final Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException
    {
        return delegate.getSampleKindIdsByHostId(hostId);
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
                                                    final TimelineChunkConsumer chunkConsumer)
    {
        dbi.withHandle(new HandleCallback<Void>()
        {
            @Override
            public Void withHandle(final Handle handle) throws Exception
            {
                handle.setStatementLocator(new StringTemplate3StatementLocator(TimelineDAOQueries.class));

                ResultIterator<TimelineChunk> iterator = null;
                try {
                    final Query<Map<String, Object>> query = handle
                        .createQuery("getSamplesByHostIdsAndSampleKindIds")
                        .bind("startTime", DateTimeUtils.unixSeconds(startTime))
                        .bind("endTime", DateTimeUtils.unixSeconds(endTime))
                        .define("hostIds", JOINER.join(hostIdList));

                    if (sampleKindIdList != null && !sampleKindIdList.isEmpty()) {
                        query.define("sampleKindIds", JOINER.join(sampleKindIdList));
                    }

                    iterator = query
                        .map(timelineChunkMapper)
                        .iterator();

                    while (iterator.hasNext()) {
                        chunkConsumer.processTimelineChunk(iterator.next());
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
    public void test() throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.test();
    }

    @Override
    public synchronized void bulkInsertHosts(final List<String> hosts) throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.bulkInsertHosts(hosts.iterator());
    }

    @Override
    public synchronized void bulkInsertEventCategories(final List<String> categoryNames) throws UnableToObtainConnectionException, CallbackFailedException
    {
        delegate.bulkInsertEventCategories(categoryNames.iterator());

    }

    @Override
    public synchronized void bulkInsertSampleKinds(final List<CategoryIdAndSampleKind> categoryAndKinds)
    {
        delegate.bulkInsertSampleKinds(categoryAndKinds.iterator());
    }

    @Override
    public synchronized List<TimelineChunk> bulkInsertTimelineChunks(final List<TimelineChunk> timelineChunkList)
    {
        delegate.begin();
        Long lastTimelineChunkId = delegate.getHighestTimelineChunkId();
        final int count = timelineChunkList.size();
        final List<TimelineChunk> chunksWithIds = new ArrayList<TimelineChunk>(count);
        long chunkId = lastTimelineChunkId + 1;
        for (TimelineChunk timelineChunk : timelineChunkList) {
            chunksWithIds.add(new TimelineChunk(chunkId++, timelineChunk));
        }
        delegate.bulkInsertTimelineChunks(chunksWithIds.iterator());
        delegate.commit();
        return chunksWithIds;
    }

}
