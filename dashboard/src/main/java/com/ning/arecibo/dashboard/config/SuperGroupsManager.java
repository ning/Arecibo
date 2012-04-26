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

package com.ning.arecibo.dashboard.config;

import com.ning.arecibo.dashboard.guice.DashboardConfig;
import com.ning.arecibo.util.timeline.CategoryAndSampleKinds;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SuperGroupsManager
{
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Iterable<CategoryAndSampleKinds> kinds;

    @Inject
    public SuperGroupsManager(final DashboardConfig config) throws IOException
    {
        if (config.getSuperGroupsFile() == null) {
            kinds = ImmutableList.<CategoryAndSampleKinds>of();
            return;
        }

        final File superGroupsFile = new File(config.getSuperGroupsFile());
        final List<SuperGroup> groups = mapper.readValue(superGroupsFile, new TypeReference<List<SuperGroup>>()
        {
        });

        final ImmutableList.Builder<CategoryAndSampleKinds> kindsBuilder = ImmutableList.<CategoryAndSampleKinds>builder();
        for (final SuperGroup group : groups) {
            kindsBuilder.add(group.asMetaCategoryAndSampleKinds());
        }

        kinds = kindsBuilder.build();
    }

    public Iterable<CategoryAndSampleKinds> getAllKinds()
    {
        return kinds;
    }
}
