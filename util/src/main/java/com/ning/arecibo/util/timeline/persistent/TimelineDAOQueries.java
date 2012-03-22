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

package com.ning.arecibo.util.timeline.persistent;

import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineChunkBinder;
import com.ning.arecibo.util.timeline.TimelineChunkMapper;
import com.ning.arecibo.util.timeline.TimelineTimes;
import com.ning.arecibo.util.timeline.TimelineTimesBinder;
import com.ning.arecibo.util.timeline.TimelineTimesMapper;
import org.skife.jdbi.v2.DefaultMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.ExternalizedSqlViaStringTemplate3;

import java.util.List;
import java.util.Map;

@ExternalizedSqlViaStringTemplate3()
@RegisterMapper({TimelineTimesMapper.class, TimelineChunkMapper.class})
public interface TimelineDAOQueries extends Transactional<TimelineDAOQueries>
{
    @SqlQuery
    Integer getHostId(@Bind("hostName") final String host);

    @SqlQuery
    String getHost(@Bind("hostId") final Integer hostId);

    @SqlQuery
    @Mapper(DefaultMapper.class)
    List<Map<String, Object>> getHosts();

    @SqlUpdate
    void addHost(@Bind("hostName") final String host);

    @SqlQuery
    Integer getSampleKindId(@Bind("sampleKind") final String sampleKind);

    @SqlQuery
    String getSampleKind(@Bind("sampleKindId") final Integer sampleKindId);

    @SqlUpdate
    void addSampleKind(@Bind("sampleKind") final String sampleKind);

    @SqlQuery
    Iterable<String> getSampleKindsByHostName(@Bind("hostName") final String host);

    @SqlQuery
    @Mapper(DefaultMapper.class)
    List<Map<String, Object>> getSampleKinds();

    @SqlQuery
    int getLastInsertedId();

    @SqlUpdate
    void insertTimelineTimes(@TimelineTimesBinder final TimelineTimes timelineTimes);

    @SqlUpdate
    void insertTimelineChunk(@TimelineChunkBinder final TimelineChunk timelineChunk);
}