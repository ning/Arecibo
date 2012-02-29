package com.ning.arecibo.collector;

import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import org.joda.time.DateTime;

import java.io.InputStream;
import java.util.Map;

public interface CollectorClient
{
    public InputStream getHostsAsStream();

    public Iterable<String> getHosts();

    public InputStream getSampleKindsAsStream();

    public Iterable<String> getSampleKinds();

    public InputStream getSamplesByHostNameAsStream(final String hostName);

    public Map<String, TimelineChunkAndTimes> getSamplesByHostName(final String hostName);

    public InputStream getSamplesByHostNameAsStream(final String hostName, final DateTime from);

    public Map<String, TimelineChunkAndTimes> getSamplesByHostName(final String hostName, final DateTime from);

    public InputStream getSamplesByHostNameAsStream(final String hostName, final DateTime from, final DateTime to);

    public Map<String, TimelineChunkAndTimes> getSamplesByHostName(final String hostName, final DateTime from, final DateTime to);

    public InputStream getSamplesByHostNameAndSampleKindAsStream(final String hostName, final String sampleKind);

    public Iterable<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(final String hostName, final String sampleKind);

    public InputStream getSamplesByHostNameAndSampleKindAsStream(final String hostName, final String sampleKind, final DateTime from);

    public Iterable<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(final String hostName, final String sampleKind, final DateTime from);

    public InputStream getSamplesByHostNameAndSampleKindAsStream(final String hostName, final String sampleKind, final DateTime from, final DateTime to);

    public Iterable<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(final String hostName, final String sampleKind, final DateTime from, final DateTime to);
}
