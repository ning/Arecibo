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

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.File;
import java.util.Map;

public class LegendConfigurationsManager
{
    public static final String LEGENDS_KEY = "legends";
    public static final String FIXTURES_KEY = "fixtures";

    private static final ObjectMapper mapper = new ObjectMapper();

    private final LegendConfiguration legendConfiguration = new LegendConfiguration();
    private final String legendFixtures;

    @Inject
    public LegendConfigurationsManager(final DashboardConfig config) throws Exception
    {
        // Configure the legend if specified
        if (config.getLegendGroupsFile() != null) {
            final File legendGroupsFile = new File(config.getLegendGroupsFile());
            final LegendConfiguration legendConfiguration = mapper.readValue(legendGroupsFile, new TypeReference<LegendConfiguration>()
            {
            });
            this.legendConfiguration.putAll(legendConfiguration);
        }

        // Configure the legend fixtures if specified
        if (config.getLegendFixturesFile() != null) {
            final File legendFixturesFile = new File(config.getLegendFixturesFile());
            legendFixtures = Joiner.on("\n").join(Files.readLines(legendFixturesFile, Charsets.UTF_8));
        }
        else {
            legendFixtures = "";
        }
    }

    public Map getConfiguration()
    {
        final ImmutableMap.Builder builder = new ImmutableMap.Builder();
        builder.put(LEGENDS_KEY, legendConfiguration);
        builder.put(FIXTURES_KEY, legendFixtures);
        return builder.build();
    }
}
