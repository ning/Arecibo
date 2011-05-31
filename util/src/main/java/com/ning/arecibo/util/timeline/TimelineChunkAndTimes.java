package com.ning.arecibo.util.timeline;

public class TimelineChunkAndTimes {
    private final TimelineChunk timelineChunk;
    private final TimelineTimes timelineTimes;

    public TimelineChunkAndTimes(TimelineChunk timelineChunk, TimelineTimes timelineTimes) {
        this.timelineChunk = timelineChunk;
        this.timelineTimes = timelineTimes;
    }

    public TimelineChunk getTimelineChunk() {
        return timelineChunk;
    }

    public TimelineTimes getTimelineTimes() {
        return timelineTimes;
    }
}
