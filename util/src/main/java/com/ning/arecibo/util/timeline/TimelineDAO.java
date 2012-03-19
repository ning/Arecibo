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

import com.google.common.collect.BiMap;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.CallbackFailedException;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;

import java.util.List;

import javax.annotation.Nullable;

public interface TimelineDAO
{
    // Hosts table

    Integer getHostId(String host) throws UnableToObtainConnectionException, CallbackFailedException;

    String getHost(Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException;

    BiMap<Integer, String> getHosts() throws UnableToObtainConnectionException, CallbackFailedException;

    Integer getOrAddHost(String host) throws UnableToObtainConnectionException, CallbackFailedException;

    // Sample kinds table

    Integer getSampleKindId(String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException;

    String getSampleKind(Integer sampleKindId) throws UnableToObtainConnectionException, CallbackFailedException;

    BiMap<Integer, String> getSampleKinds() throws UnableToObtainConnectionException, CallbackFailedException;

    Integer getOrAddSampleKind(Integer hostId, String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException;

    Iterable<String> getSampleKindsByHostName(String host) throws UnableToObtainConnectionException, CallbackFailedException;

    // Timelines tables

    Integer insertTimelineTimes(TimelineTimes timelineTimes) throws UnableToObtainConnectionException, CallbackFailedException;

    Integer insertTimelineChunk(TimelineChunk timelineChunk) throws UnableToObtainConnectionException, CallbackFailedException;

    List<TimelineChunkAndTimes> getSamplesByHostName(String hostName, DateTime startTime, DateTime endTime) throws UnableToObtainConnectionException, CallbackFailedException;

    List<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(String hostName, String sampleKind, DateTime startTime, DateTime endTime) throws UnableToObtainConnectionException, CallbackFailedException;

    void getSamplesByHostNamesAndSampleKinds(List<String> hostNames,
                                             @Nullable List<String> sampleKinds,
                                             DateTime startTime,
                                             DateTime endTime,
                                             TimelineChunkAndTimesConsumer chunkConsumer) throws UnableToObtainConnectionException, CallbackFailedException;
}
