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

package com.ning.arecibo.dashboard.resources;

import com.ning.arecibo.collector.CollectorClient;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;
import com.ning.arecibo.dashboard.guice.DashboardConfig;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.mogwee.executors.Executors;
import org.codehaus.jackson.annotate.JsonValue;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HostsStore
{
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Logger log = LoggerFactory.getLogger(HostsStore.class);
    private final HashFunction HASH = Hashing.murmur3_128();
    private final Object updateMonitor = new Object();
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor("sampleKindsUpdater");

    private final GalaxyStatusManager galaxyStatusManager;
    private final CollectorClient client;
    private final DashboardConfig config;

    private ImmutableMultimap<String, Map<String, String>> hostsInfo = null;
    private String etag = HASH.hashLong(System.currentTimeMillis()).toString();
    private String json = null;

    @Inject
    public HostsStore(final GalaxyStatusManager galaxyStatusManager, final CollectorClient client, final DashboardConfig config)
    {
        this.galaxyStatusManager = galaxyStatusManager;
        this.client = client;
        this.config = config;

        start();
    }

    private void start()
    {
        service.scheduleWithFixedDelay(new Runnable()
        {
            @Override
            public void run()
            {
                final ImmutableMultimap.Builder<String, Map<String, String>> builder = new ImmutableMultimap.Builder<String, Map<String, String>>();
                final Iterable<String> hosts = client.getHosts();

                for (final String hostName : hosts) {
                    final String coreType = Strings.nullToEmpty(galaxyStatusManager.getCoreType(hostName));
                    builder.put(coreType, ImmutableMap.<String, String>of(
                            "hostName", hostName,
                            "globalZone", Strings.nullToEmpty(galaxyStatusManager.getGlobalZone(hostName)),
                            "configPath", Strings.nullToEmpty(galaxyStatusManager.getConfigPath(hostName)),
                            "configSubPath", Strings.nullToEmpty(galaxyStatusManager.getConfigSubPath(hostName)),
                            "coreType", coreType
                    ));
                }

                updateCacheIfNeeded(builder.build());
            }
        }, config.getSampleKindsUpdaterDelay().getMillis(), config.getSampleKindsUpdaterDelay().getMillis(), TimeUnit.MILLISECONDS);
        // We give an initial delay for the Galaxy manager to query the Gonsole first

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                service.shutdownNow();
            }
        });
    }

    void updateCacheIfNeeded(final ImmutableMultimap<String, Map<String, String>> hosts)
    {
        if (hostsInfo == null || !hostsInfo.equals(hosts)) {
            log.info("Detected change in hosts - updating cache");
            cacheHosts(hosts);
        }
    }

    void cacheHosts(final ImmutableMultimap<String, Map<String, String>> newHostsInfo)
    {
        final String newBytes;
        try {
            newBytes = mapper.writeValueAsString(newHostsInfo.asMap());
        }
        catch (IOException e) {
            log.warn("Unable to serialize new hosts", e);
            return;
        }

        synchronized (updateMonitor) {
            hostsInfo = newHostsInfo;
            etag = HASH.hashBytes(newBytes.getBytes(UTF_8)).toString();
            json = newBytes;
        }
    }

    ImmutableMultimap<String, Map<String, String>> getHostsInfo()
    {
        return hostsInfo;
    }

    public String getEtag()
    {
        synchronized (updateMonitor) {
            return etag;
        }
    }

    @JsonValue
    public String getJsonString()
    {
        synchronized (updateMonitor) {
            return json;
        }
    }
}
