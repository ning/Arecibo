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

package com.ning.arecibo.collector.persistent;

import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineChunkMapper;
import com.ning.arecibo.util.timeline.TimelineTimes;
import com.ning.arecibo.util.timeline.TimelineTimesBinder;
import com.ning.arecibo.util.timeline.TimelineTimesMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.ExternalizedSqlViaStringTemplate3;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.List;

@ExternalizedSqlViaStringTemplate3()
@RegisterMapper({TimelineTimesMapper.class, TimelineChunkMapper.class})
public interface TimelineAggregatorDAO extends Transactional<TimelineAggregatorDAO>
{
    @SqlQuery
    List<TimelineChunk> getTimelineChunksForTimelineTimes(@BindIn("timelineTimesIds") final List<Long> timelineTimesId);

    @SqlQuery
    List<TimelineTimes> getTimelineTimesAggregationCandidates(@Bind("aggregationLevel") final int aggregationLevel);

    @SqlUpdate
    void insertNewInvalidTimelineTimes(@TimelineTimesBinder final TimelineTimes times, @Bind("aggregationLevel") final int aggregationLevel);

    @SqlQuery
    int getLastInsertedId();

    @SqlUpdate
    void makeTimelineTimesValid(@Bind("timelineTimesId") final int timelineTimeId);

    @SqlUpdate
    void makeTimelineTimesInvalid(@BindIn("timelineTimesIds") final List<Long> timelineTimesIds);

    @SqlUpdate
    void deleteTimelineTimes(@BindIn("timelineTimesIds") final List<Long> timelineTimesIds);

    @SqlUpdate
    void deleteTimelineChunks(@BindIn("timelineTimesIds") final List<Long> timelineTimesIds);
}
