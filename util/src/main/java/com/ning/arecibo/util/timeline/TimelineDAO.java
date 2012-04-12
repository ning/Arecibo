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

import java.util.List;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;

import com.google.common.collect.BiMap;

public interface TimelineDAO
{
    // Hosts table

    Integer getHostId(String host) throws UnableToObtainConnectionException, CallbackFailedException;

    String getHost(Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException;

    BiMap<Integer, String> getHosts() throws UnableToObtainConnectionException, CallbackFailedException;

    Integer getOrAddHost(String host) throws UnableToObtainConnectionException, CallbackFailedException;

    // Event categories table

    Integer getEventCategoryId(String eventCategory) throws UnableToObtainConnectionException, CallbackFailedException;

    String getEventCategory(Integer eventCategoryId) throws UnableToObtainConnectionException, CallbackFailedException;

    BiMap<Integer, String> getEventCategories() throws UnableToObtainConnectionException, CallbackFailedException;

    Integer getOrAddEventCategory(String eventCategory) throws UnableToObtainConnectionException, CallbackFailedException;

    // Sample kinds table

    Integer getSampleKindId(int eventCategory, String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException;

    CategoryIdAndSampleKind getCategoryIdAndSampleKind(Integer sampleKindId) throws UnableToObtainConnectionException, CallbackFailedException;

    BiMap<Integer, CategoryIdAndSampleKind> getSampleKinds() throws UnableToObtainConnectionException, CallbackFailedException;

    Integer getOrAddSampleKind(Integer hostId, Integer eventCategoryId, String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException;

    Iterable<Integer> getSampleKindIdsByHostId(Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException;

    Iterable<HostIdAndSampleKindId> getSampleKindIdsForAllHosts() throws UnableToObtainConnectionException, CallbackFailedException;

    // Timelines tables

    Long insertTimelineChunk(TimelineChunk timelineChunk) throws UnableToObtainConnectionException, CallbackFailedException;

    void getSamplesByHostIdsAndSampleKindIds(List<Integer> hostIds,
                                             @Nullable List<Integer> sampleKindIds,
                                             DateTime startTime,
                                             DateTime endTime,
                                             TimelineChunkConsumer chunkConsumer) throws UnableToObtainConnectionException, CallbackFailedException;

    Integer insertLastStartTimes(StartTimes startTimes);

    StartTimes getLastStartTimes();

    void deleteLastStartTimes();

    void bulkInsertHosts(final List<String> hosts) throws UnableToObtainConnectionException, CallbackFailedException;

    void bulkInsertEventCategories(final List<String> categoryNames) throws UnableToObtainConnectionException, CallbackFailedException;

    void bulkInsertSampleKinds(final List<CategoryIdAndSampleKind> categoryAndKinds);

    void bulkInsertTimelineChunks(final List<TimelineChunk> timelineChunkList);

    void test() throws UnableToObtainConnectionException, CallbackFailedException;
}
