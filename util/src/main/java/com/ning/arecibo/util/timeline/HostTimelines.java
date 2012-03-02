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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class is used to represent timeline information for a collection
 * of sample kinds for a single host, starting at a specific time.  This class
 * is only used while samples are being accumulated before the samples are
 * written to the db.
 */
public class HostTimelines {
    /**
     *  Mapping from sample kind to the ordered map from time to the latest
     *  timeline chunk.  This cl
     *  range.  And this is why the SampleTimelineChunk has to point
     *  back at the SampleSetTimelineChunk; that's how we get the
     *  timeline times instance.
     */
    private final Map<String, TreeMap<Integer, TimelineHostEventAccumulator>> sampleKindTimelines;

    private HostTimelines() {
        sampleKindTimelines = new HashMap<String, TreeMap<Integer, TimelineHostEventAccumulator>>();
    }
}
