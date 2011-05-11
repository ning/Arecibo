package com.ning.arecibo.util.timeline;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

public class TimelineTimestamps {

    private final SampleSetTimelineChunk timelines;

    private final List<DateTime> times;

    public TimelineTimestamps(SampleSetTimelineChunk timelines) {
        this.timelines = timelines;
        this.times = new ArrayList<DateTime>();
    }

    public int getSampleCount() {
        return times.size();
    }

    public DateTime getSampleTimestamp(final int sampleNumber) {
        if (sampleNumber < 0 || sampleNumber > times.size()) {
            return null;
        }
        else {
            return times.get(sampleNumber);
        }
    }

    public SampleSetTimelineChunk getTimelines() {
        return timelines;
    }
}
