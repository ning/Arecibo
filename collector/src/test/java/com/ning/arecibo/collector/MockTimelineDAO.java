package com.ning.arecibo.collector;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.TimelineTimes;
import org.joda.time.DateTime;

import java.util.List;

public final class MockTimelineDAO extends TimelineDAO
{
    private final BiMap<Integer, String> hosts = HashBiMap.create();
    private final BiMap<Integer, String> sampleKinds = HashBiMap.create();
    private final BiMap<Integer, TimelineTimes> timelineTimes = HashBiMap.create();
    private final BiMap<Integer, TimelineChunk> timelineChunks = HashBiMap.create();

    public MockTimelineDAO()
    {
        super(null);
    }

    @Override
    public BiMap<Integer, String> getHosts()
    {
        return hosts;
    }

    @Override
    public int addHost(final String host)
    {
        hosts.put(hosts.size(), host);
        return hosts.size() - 1;
    }

    @Override
    public BiMap<Integer, String> getSampleKinds()
    {
        return sampleKinds;
    }

    @Override
    public int addSampleKind(final String sampleKind)
    {
        sampleKinds.put(sampleKinds.size(), sampleKind);
        return sampleKinds.size() - 1;
    }

    @Override
    public int insertTimelineTimes(final TimelineTimes timeline)
    {
        timelineTimes.put(timelineTimes.size(), timeline);
        return timelineTimes.size() - 1;
    }

    @Override
    public int insertTimelineChunk(final TimelineChunk chunk)
    {
        timelineChunks.put(timelineChunks.size(), chunk);
        return timelineChunks.size() - 1;
    }

    @Override
    public List<TimelineChunkAndTimes> getSamplesByHostName(final String hostName, final DateTime startTime, final DateTime endTime)
    {
        throw new UnsupportedOperationException("TODO");
    }
}
