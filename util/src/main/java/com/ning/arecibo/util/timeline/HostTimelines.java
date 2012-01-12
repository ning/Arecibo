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
