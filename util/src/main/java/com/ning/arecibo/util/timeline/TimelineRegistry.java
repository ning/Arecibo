/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.util.timeline;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

public class TimelineRegistry
{
    private final Object hostsMonitor = new Object();
    private final Object sampleKindsMonitor = new Object();

    private final TimelineDAO dao;
    private final BiMap<Integer, String> hosts;
    private final BiMap<Integer, String> sampleKinds;
    private final Multimap<Integer, String> sampleKindsForHost = HashMultimap.create();

    @Inject
    public TimelineRegistry(final TimelineDAO dao)
    {
        this.dao = dao;
        this.hosts = dao.getHosts();
        this.sampleKinds = dao.getSampleKinds();
    }

    public Iterable<String> getHosts()
    {
        return hosts.values();
    }

    public Integer getHostId(final String host)
    {
        return hosts.inverse().get(host);
    }

    public int getOrAddHost(final String host)
    {
        Integer hostId = getHostId(host);

        if (hostId == null) {
            synchronized (hostsMonitor) {
                hostId = getHostId(host);
                if (hostId == null) {
                    hostId = dao.addHost(host);
                    hosts.put(hostId, host);
                }
            }
        }

        return hostId;
    }

    public Iterable<String> getSampleKinds()
    {
        return sampleKinds.values();
    }

    public String getSampleKindById(final int sampleKindId)
    {
        return sampleKinds.get(sampleKindId);
    }

    public Iterable<String> getSampleKindsForHost(final String host)
    {
        final Integer hostId = getHostId(host);
        if (hostId != null) {
            return sampleKindsForHost.get(hostId);
        }
        else {
            return ImmutableList.of();
        }
    }

    public int getOrAddSampleKind(final int hostId, final String sampleKind)
    {
        Integer sampleKindId = sampleKinds.inverse().get(sampleKind);

        if (sampleKindId == null) {
            synchronized (sampleKindsMonitor) {
                sampleKindId = sampleKinds.inverse().get(sampleKind);
                if (sampleKindId == null) {
                    sampleKindId = dao.addSampleKind(sampleKind);
                    sampleKinds.put(sampleKindId, sampleKind);
                    sampleKindsForHost.put(hostId, sampleKind);
                }
            }
        }

        return sampleKindId;
    }
}
