package com.ning.arecibo.util.timeline;

public class CachedObject {
    private final long objectId;
    private long lastReferencedTime;

    public CachedObject(long objectId) {
        this.objectId = objectId;
        this.lastReferencedTime = System.nanoTime();
    }

    public CachedObject(long objectId, long lastReferencedTime) {
        this.objectId = objectId;
        this.lastReferencedTime = lastReferencedTime;
    }

    public long getObjectId() {
        return objectId;
    }

    public long getLastReferencedTime() {
        return lastReferencedTime;
    }

    public void setLastReferencedTime(final long lastReferencedTime) {
        this.lastReferencedTime = lastReferencedTime;
    }
}
