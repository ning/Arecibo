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
import com.ning.arecibo.dashboard.config.SuperGroupsManager;
import com.ning.arecibo.dashboard.guice.DashboardConfig;
import com.ning.arecibo.util.timeline.CategoryAndSampleKinds;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GroupsAndSampleKindsStore
{
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Logger log = LoggerFactory.getLogger(GroupsAndSampleKindsStore.class);
    private final HashFunction HASH = Hashing.murmur3_128();
    private final Object updateMonitor = new Object();
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor("sampleKindsUpdater");

    private final SuperGroupsManager groupsManager;
    private final CollectorClient client;
    private final DashboardConfig config;

    private Iterable<CategoryAndSampleKinds> collectorSampleKinds = null;
    private String etag = HASH.hashLong(System.currentTimeMillis()).toString();
    private String json = null;

    @Inject
    public GroupsAndSampleKindsStore(final SuperGroupsManager groupsManager, final CollectorClient client, final DashboardConfig config)
    {
        this.groupsManager = groupsManager;
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
                final Iterable<CategoryAndSampleKinds> sampleKinds = client.getSampleKinds();
                if (sampleKinds != null) {
                    updateCacheIfNeeded(sampleKinds);
                }
            }
        }, 0, config.getSampleKindsUpdaterDelay().getMillis(), TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                service.shutdownNow();
            }
        });
    }

    void updateCacheIfNeeded(final Iterable<CategoryAndSampleKinds> newSampleKinds)
    {
        if (collectorSampleKinds == null || !Iterables.elementsEqual(newSampleKinds, collectorSampleKinds)) {
            log.info("Detected change in sample kinds - updating cache");
            cacheGroupsAndSampleKinds(newSampleKinds);
        }
    }

    void cacheGroupsAndSampleKinds(final Iterable<CategoryAndSampleKinds> newSampleKinds)
    {
        final ImmutableMap.Builder builder = new ImmutableMap.Builder();
        builder.put("groups", groupsManager.getCustomGroups());
        builder.put("sampleKinds", newSampleKinds);
        final ImmutableMap immutableMap = builder.build();

        final String newBytes;
        try {
            newBytes = mapper.writeValueAsString(immutableMap);
        }
        catch (IOException e) {
            log.warn("Unable to serialize new kinds", e);
            return;
        }

        synchronized (updateMonitor) {
            collectorSampleKinds = newSampleKinds;
            etag = HASH.hashBytes(newBytes.getBytes(UTF_8)).toString();
            json = newBytes;
        }
    }

    Iterable<CategoryAndSampleKinds> getCollectorSampleKinds()
    {
        return collectorSampleKinds;
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
