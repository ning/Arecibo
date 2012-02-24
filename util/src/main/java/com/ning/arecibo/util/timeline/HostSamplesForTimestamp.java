package com.ning.arecibo.util.timeline;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 * Instances of this class represent samples sent from one host and one
 * category, e.g., JVM, representing one point in time.
 */
@SuppressWarnings("unchecked")
public class HostSamplesForTimestamp
{
    private final int hostId;
    private final String category;
    private final DateTime timestamp;
    // A map from sample id to sample value for that timestamp
    private final Map<Integer, ScalarSample> samples;

    public HostSamplesForTimestamp(final int hostId, final String category, final DateTime timestamp)
    {
        this(hostId, category, timestamp, new HashMap<Integer, ScalarSample>());
    }

    @JsonCreator
    public HostSamplesForTimestamp(@JsonProperty("hostId") final int hostId, @JsonProperty("category") final String category, @JsonProperty("timestamp") final DateTime timestamp, @JsonProperty("samples") final Map<Integer, ScalarSample> samples)
    {
        this.hostId = hostId;
        this.category = category;
        this.timestamp = timestamp;
        this.samples = samples;
    }

    public int getHostId()
    {
        return hostId;
    }

    public String getCategory()
    {
        return category;
    }

    public DateTime getTimestamp()
    {
        return timestamp;
    }

    public Map<Integer, ScalarSample> getSamples()
    {
        return samples;
    }
}
