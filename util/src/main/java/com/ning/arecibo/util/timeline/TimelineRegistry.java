package com.ning.arecibo.util.timeline;

import com.google.common.collect.BiMap;
import com.google.inject.Inject;

public class TimelineRegistry
{
    private final Object hostsMonitor = new Object();
    private final Object sampleKindsMonitor = new Object();

    private final TimelineDAO dao;
    private final BiMap<Integer, String> hosts;
    private final BiMap<Integer, String> sampleKinds;

    @Inject
    public TimelineRegistry(final TimelineDAO dao)
    {
        this.dao = dao;
        this.hosts = dao.getHosts();
        this.sampleKinds = dao.getSampleKinds();
    }

    public int getOrAddHost(final String host)
    {
        Integer hostId = hosts.inverse().get(host);

        if (hostId == null) {
            synchronized (hostsMonitor) {
                hostId = hosts.inverse().get(host);
                if (hostId == null) {
                    hostId = dao.addHost(host);
                    hosts.put(hostId, host);
                }
            }
        }

        return hostId;
    }

    public int getOrAddSampleKind(final String sampleKind)
    {
        Integer sampleKindId = sampleKinds.inverse().get(sampleKind);

        if (sampleKindId == null) {
            synchronized (sampleKindsMonitor) {
                sampleKindId = sampleKinds.inverse().get(sampleKind);
                if (sampleKindId == null) {
                    sampleKindId = dao.addSampleKind(sampleKind);
                    sampleKinds.put(sampleKindId, sampleKind);
                }
            }
        }

        return sampleKindId;
    }
}
