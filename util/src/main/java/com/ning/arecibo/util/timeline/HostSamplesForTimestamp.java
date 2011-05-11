package com.ning.arecibo.util.timeline;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

/**
 * Instances of this class represent samples sent from one host and one timestamp.
 */
@SuppressWarnings("unchecked")
public class HostSamplesForTimestamp {
    private final String hostName;
    private final DateTime timestamp;
    private final Map<String, ScalarSample> samples;

    public HostSamplesForTimestamp(String hostName, DateTime timestamp) {
        this(hostName, timestamp, new HashMap<String, ScalarSample>());
    }

    public HostSamplesForTimestamp(String hostName, DateTime timestamp, Map<String, ScalarSample> samples) {
        this.hostName = hostName;
        this.timestamp = timestamp;
        this.samples = samples;
    }

    public String getHostName() {
        return hostName;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, ScalarSample> getSamples() {
        return samples;
    }
}
