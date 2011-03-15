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
