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
    private final String hostName;
    private final String category;
    private final DateTime timestamp;
    private final Map<String, ScalarSample> samples;

    public HostSamplesForTimestamp(String hostName, String category, DateTime timestamp) {
        this(hostName, category, timestamp, new HashMap<String, ScalarSample>());
    }

    public HostSamplesForTimestamp(String hostName, String category, DateTime timestamp, Map<String, ScalarSample> samples) {
        this.hostName = hostName;
        this.category = category;
        this.timestamp = timestamp;
        this.samples = samples;
    }

    public String getHostName() {
        return hostName;
    }

    public String getCategory() {
        return category;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, ScalarSample> getSamples() {
        return samples;
    }
}
