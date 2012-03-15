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

public interface TimelineDAO
{

    String getHost(Integer hostId) throws UnableToObtainConnectionException, CallbackFailedException;

    BiMap<Integer, String> getHosts() throws UnableToObtainConnectionException, CallbackFailedException;

    int addHost(String host) throws UnableToObtainConnectionException, CallbackFailedException;

    String getSampleKind(Integer sampleKindId) throws UnableToObtainConnectionException, CallbackFailedException;

    BiMap<Integer, String> getSampleKinds() throws UnableToObtainConnectionException, CallbackFailedException;

    int addSampleKind(String sampleKind) throws UnableToObtainConnectionException, CallbackFailedException;

    int insertTimelineTimes(TimelineTimes timelineTimes) throws UnableToObtainConnectionException, CallbackFailedException;

    int insertTimelineChunk(TimelineChunk timelineChunk) throws UnableToObtainConnectionException, CallbackFailedException;

    List<TimelineChunkAndTimes> getSamplesByHostName(String hostName, DateTime startTime, DateTime endTime) throws UnableToObtainConnectionException, CallbackFailedException;

    List<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(String hostName, String sampleKind, DateTime startTime, DateTime endTime) throws UnableToObtainConnectionException, CallbackFailedException;
}
