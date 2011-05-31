package com.ning.arecibo.util.timeline;

import java.util.Comparator;

import com.ning.arecibo.util.LRUCache;

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
 * keeping track of LRU behavior.  In the second case we ask timeline
 * objects as they accumulate to copy their samples arrays and return the
 * copies.
 * <p>
 * The cache manager caches two kinds of data: TimelineTimestamps, and
 * TimelineChunks.  It provides LRU behavior by keeping the live objects
 * in a linked list, moving them to the head of the list when they are
 * referenced; and removing them from the end of the list when newer ones
 * are referenced.
 */
public class CacheManager {
    @SuppressWarnings("unused")
    private static Comparator<CachedObject> comparer = new Comparator<CachedObject>() {

        @Override
        public int compare(CachedObject o1, CachedObject o2) {
            final long o1ReferencedTime = o1.getLastReferencedTime();
            final long o2ReferencedTime = o2.getLastReferencedTime();
            if (o1ReferencedTime > o2ReferencedTime) {
                return -1;
            }
            else if (o1ReferencedTime < o2ReferencedTime) {
                return 1;
            }
            else {
                return 0;
            }
        }
    };

    private final LRUCache<Long, CachedObject> cacheMap;

    public CacheManager(int maxObjects) {
        this.cacheMap = new LRUCache<Long, CachedObject>(maxObjects);
    }

    public synchronized CachedObject findObject(final long objectId) {
        CachedObject cachedObject = cacheMap.remove(objectId);
        if (cachedObject != null) {
            // Move it to the front of the list by inserting it again
            cacheMap.put(objectId, cachedObject);
            return cachedObject;
        }
        else {
            return null;
        }
    }

    public synchronized CachedObject insertObject(final CachedObject cachableObject) {
        final long objectId = cachableObject.getObjectId();
        final CachedObject cachedObject = findObject(objectId);
        if (cachedObject != null) {
            return cachedObject;
        }
        else {
            cacheMap.put(objectId, cachableObject);
            return cachableObject;
        }
    }
}
