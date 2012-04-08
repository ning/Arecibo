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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ning.arecibo.util.LRUCache;

/**
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
 * The cache manager caches TimelineChunks.  It provides LRU behavior
 * by keeping the live objects in a linked list, moving them to the
 * head of the list when they are referenced; and removing them from
 * the end of the list when newer ones are referenced.
 */
public class LRUObjectCache {
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

    public LRUObjectCache(int maxObjects) {
        this.cacheMap = new LRUCache<Long, CachedObject>(maxObjects);
    }

    /**
     * Get the CachedObject with the given objectId, or null if there is none.
     * If one is found, remove it then re-insert it in the LRUCache, which makes
     * it most-recently used.
     * @param objectId the id of a CachedObject
     * @return the CachedObject with that id, or null if there is none.
     */
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

    /**
     * This method is intentionally not synchronized, because
     * (I think) it's better to let other threads in, since
     * we will look up anything we didn't get this way via a db
     * query.
     * <p>
     * The caller will call this function to get the timelines he needs for
     * his query.  Some won't be in the cache, and for these he'll issue one
     * or more db queries.
     * @param objectIds a collection of longs representing the object ids of
     * for which a response from the cache is requested.
     * @return a map from objectId to CachedObjects found in the cache.
     */
    public Map<Long, CachedObject> findObjects(final Collection<Long> objectIds) {
        final Map<Long, CachedObject> objects = new HashMap<Long, CachedObject>();
        final Iterator<Long> idIterator = objectIds.iterator();
        while (idIterator.hasNext()) {
            final long id = idIterator.next();
            final CachedObject cachedObject = findObject(id);
            if (cachedObject != null) {
                objects.put(id, cachedObject);
            }
        }
        return objects;
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
