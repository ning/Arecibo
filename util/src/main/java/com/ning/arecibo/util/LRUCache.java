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

package com.ning.arecibo.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> extends LinkedHashMap<K, V>
{
    private static final long serialVersionUID    = 1L;

    private static final int  DEFAULT_MAX_ENTRIES = 1000;
    private int               maxEntries;

    public LRUCache()
    {
        this(DEFAULT_MAX_ENTRIES);
    }

    public LRUCache(int maxEntries)
    {
        super(maxEntries + 1, .75F, true);
        setMaxEntries(maxEntries);
    }

    // This method is called just after a new entry has been added
    public boolean removeEldestEntry(Map.Entry<K, V> eldest)
    {
        return size() > maxEntries;
    }

    public void setMaxEntries(int v)
    {
        maxEntries = v;
    }

    public int getMaxEntries()
    {
        return maxEntries;
    }
}
