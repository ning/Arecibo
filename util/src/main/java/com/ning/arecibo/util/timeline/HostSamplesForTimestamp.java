package com.ning.arecibo.util.timeline;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

/**
 * Instances of this class represent samples sent from one host and one
 * category, e.g., JVM, representing one point in time.
 */
@SuppressWarnings("unchecked")
public class HostSamplesForTimestamp {
    private final int hostId;
    private final String category;
    private final DateTime timestamp;
    // A map from sample id to sample value for that timestamp
    private final Map<Integer, ScalarSample> samples;

    public HostSamplesForTimestamp(int hostId, String category, DateTime timestamp) {
        this(hostId, category, timestamp, new HashMap<Integer, ScalarSample>());
    }

    public HostSamplesForTimestamp(int hostId, String category, DateTime timestamp, Map<Integer, ScalarSample> samples) {
        this.hostId = hostId;
        this.category = category;
        this.timestamp = timestamp;
        this.samples = samples;
    }

    public int getHostId() {
        return hostId;
    }

    public String getCategory() {
        return category;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public Map<Integer, ScalarSample> getSamples() {
        return samples;
    }
}
