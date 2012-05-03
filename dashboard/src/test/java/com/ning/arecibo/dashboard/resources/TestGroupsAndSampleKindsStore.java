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

import com.google.common.collect.ImmutableList;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.skife.config.TimeSpan;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicReference;

public class TestGroupsAndSampleKindsStore
{
    @Test(groups = "slow")
    public void testCacheUpdater() throws Exception
    {
        final DashboardConfig config = Mockito.mock(DashboardConfig.class);
        Mockito.when(config.getSampleKindsUpdaterDelay()).thenReturn(new TimeSpan("1h"));

        final SuperGroupsManager groupsManager = new SuperGroupsManager(config);

        final AtomicReference<Iterable<CategoryAndSampleKinds>> kinds = new AtomicReference<Iterable<CategoryAndSampleKinds>>();
        kinds.set(ImmutableList.<CategoryAndSampleKinds>of());

        final CollectorClient client = Mockito.mock(CollectorClient.class);
        Mockito.when(client.getSampleKinds()).thenAnswer(new Answer<Iterable<CategoryAndSampleKinds>>()
        {
            @Override
            public Iterable<CategoryAndSampleKinds> answer(final InvocationOnMock invocation) throws Throwable
            {
                return kinds.get();
            }
        });

        final GroupsAndSampleKindsStore store = new GroupsAndSampleKindsStore(groupsManager, client, config);
        // Wait for the first (automatic) cache update
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        Thread.sleep(200);

        // Initial state
        Assert.assertEquals(store.getJsonString(), "{\"groups\":[],\"sampleKinds\":[]}");
        String etag = store.getEtag();

        // Test idempotence
        for (int i = 0; i < 10; i++) {
            store.updateCacheIfNeeded(kinds.get());
            Assert.assertEquals(store.getEtag(), etag);
        }

        // Test collection update
        final CategoryAndSampleKinds aKinds = new CategoryAndSampleKinds("JVM");
        aKinds.addSampleKind("GC");
        aKinds.addSampleKind("CPU");
        kinds.set(ImmutableList.<CategoryAndSampleKinds>of(aKinds));
        store.updateCacheIfNeeded(kinds.get());
        Assert.assertEquals(store.getJsonString(), "{\"groups\":[],\"sampleKinds\":[{\"eventCategory\":\"JVM\",\"sampleKinds\":[\"GC\",\"CPU\"]}]}");
        Assert.assertNotEquals(store.getEtag(), etag);
        etag = store.getEtag();
        for (int i = 0; i < 10; i++) {
            store.updateCacheIfNeeded(kinds.get());
            Assert.assertEquals(store.getEtag(), etag);
        }

        // Create a new list - containing the same objects
        kinds.set(ImmutableList.<CategoryAndSampleKinds>of(aKinds));
        for (int i = 0; i < 10; i++) {
            store.updateCacheIfNeeded(kinds.get());
            Assert.assertEquals(store.getEtag(), etag);
        }
    }

    @Test(groups = "fast")
    public void testCacheStore() throws Exception
    {
        final DashboardConfig config = Mockito.mock(DashboardConfig.class);
        Mockito.when(config.getSampleKindsUpdaterDelay()).thenReturn(new TimeSpan("1h"));
        final SuperGroupsManager groupsManager = new SuperGroupsManager(config);
        final CollectorClient client = Mockito.mock(CollectorClient.class);

        final GroupsAndSampleKindsStore store = new GroupsAndSampleKindsStore(groupsManager, client, config);
        Assert.assertNull(store.getCollectorSampleKinds());
        String etag = store.getEtag();
        Assert.assertNotNull(etag);
        Assert.assertNull(store.getJsonString());

        store.cacheGroupsAndSampleKinds(ImmutableList.<CategoryAndSampleKinds>of());
        Assert.assertEquals(ImmutableList.<CategoryAndSampleKinds>copyOf(store.getCollectorSampleKinds()).size(), 0);
        Assert.assertNotEquals(etag, store.getEtag());
        etag = store.getEtag();
        Assert.assertEquals(store.getJsonString(), "{\"groups\":[],\"sampleKinds\":[]}");

        final CategoryAndSampleKinds aKinds = new CategoryAndSampleKinds("JVM");
        aKinds.addSampleKind("GC");
        aKinds.addSampleKind("CPU");
        final CategoryAndSampleKinds bKinds = new CategoryAndSampleKinds("ZJVM");
        bKinds.addSampleKind("GC");
        bKinds.addSampleKind("CPU");
        bKinds.addSampleKind("Something else");

        store.cacheGroupsAndSampleKinds(ImmutableList.<CategoryAndSampleKinds>of(aKinds));
        Assert.assertEquals(ImmutableList.<CategoryAndSampleKinds>copyOf(store.getCollectorSampleKinds()).size(), 1);
        Assert.assertNotEquals(etag, store.getEtag());
        etag = store.getEtag();
        Assert.assertEquals(store.getJsonString(), "{\"groups\":[],\"sampleKinds\":[{\"eventCategory\":\"JVM\",\"sampleKinds\":[\"GC\",\"CPU\"]}]}");

        store.cacheGroupsAndSampleKinds(ImmutableList.<CategoryAndSampleKinds>of(aKinds, bKinds));
        Assert.assertEquals(ImmutableList.<CategoryAndSampleKinds>copyOf(store.getCollectorSampleKinds()).size(), 2);
        Assert.assertNotEquals(etag, store.getEtag());
        Assert.assertEquals(store.getJsonString(), "{\"groups\":[],\"sampleKinds\":[{\"eventCategory\":\"JVM\",\"sampleKinds\":[\"GC\",\"CPU\"]},{\"eventCategory\":\"ZJVM\",\"sampleKinds\":[\"GC\",\"Something else\",\"CPU\"]}]}");
    }
}
