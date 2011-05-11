package com.ning.arecibo.util.timeline;

/**
 * This class manages a collection of timelines for one or more hosts that
 * extends into the indefinite past.  It encaches the bytes for SampleTimelineChunks
 * stored in the db in response to requests, and keeps track of the total memory
 * space used.
 */
public class TimelineCacheManager {

    private final int maxMemory;

    public TimelineCacheManager(int maxMemory) {
        this.maxMemory = maxMemory;
    }


}
