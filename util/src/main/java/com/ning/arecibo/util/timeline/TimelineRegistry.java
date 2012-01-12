package com.ning.arecibo.util.timeline;

import com.google.common.collect.BiMap;
import com.google.inject.Inject;

// TODO: There is no synchronization: Either prove caller is single-threaded or add synchronization.
public class TimelineRegistry {

    private final TimelineDAO dao;
    private final BiMap<Integer, String> hosts;
    private final BiMap<Integer, String> sampleKinds;

    @Inject
    public TimelineRegistry(TimelineDAO dao) {
        this.dao = dao;
        this.hosts = dao.getHosts();
        this.sampleKinds = dao.getSampleKinds();
    }

    public int getOrAddHost(final String host) {
        final Integer hostId = hosts.inverse().get(host);
        if (hostId != null) {
            return hostId;
        }
        else {
            final int newHostId = dao.addHost(host);
            hosts.put(newHostId, host);
            return newHostId;
        }
    }

    public int getOrAddSampleKind(final String sampleKind) {
        final Integer sampleKindId = sampleKinds.inverse().get(sampleKind);
        if (sampleKindId != null) {
            return sampleKindId;
        }
        else {
            final int newSampleKindId = dao.addSampleKind(sampleKind);
            sampleKinds.put(newSampleKindId, sampleKind);
            return newSampleKindId;
        }
    }
}
