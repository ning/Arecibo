package com.ning.arecibo.util.timeline;

public class TimelineCacheManager {
    private final LRUObjectCache timelineTimesCache;
    private final LRUObjectCache timelineCache;

    public TimelineCacheManager(int maxTimelineTimes, int maxTimelines) {
        this.timelineTimesCache = new LRUObjectCache(maxTimelineTimes);
        this.timelineCache = new LRUObjectCache(maxTimelines);
    }

    public TimelineChunk getTimelineChunk(final long objectId) {
        return (TimelineChunk)timelineCache.findObject(objectId);
    }

    public TimelineTimes getTimelineTimes(final long objectId) {
        return (TimelineTimes)timelineTimesCache.findObject(objectId);
    }

    public void addTimelineChunk(final TimelineChunk chunk) {
        timelineCache.insertObject(chunk);
    }

    public void addTimelineTimes(final TimelineTimes times) {
        timelineTimesCache.insertObject(times);
    }
}
