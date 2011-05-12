package com.ning.arecibo.util.timeline;

/**
 * This class manages a collection of timelines for one or more hosts that
 * extends into the indefinite past.  It encaches the bytes for SampleTimelineChunks
 * stored in the db in response to requests, and keeps track of the total memory
 * space used.
 * <p>
 * Our theory of synchronized access is as follows.
 * The cache is built of two kinds of elements:
 * <ul>
 * <li>data read from the db, which are never modifified; and</li>
 * <li>data in a SampleSetTimeline instances, which are still adding
 * samples before being written to the db.</li>
 * <ul>
 * In the first case, the objects are immutable, so the main issue is
 * keeping track of LRU behavior.  In the second case
 */
public class TimelineCacheManager {

    private final int maxMemory;

    public TimelineCacheManager(int maxMemory) {
        this.maxMemory = maxMemory;
    }


}
