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

package com.ning.arecibo.util.galaxy;

import java.util.List;
import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;

public interface GalaxyConfig
{
    @Config("arecibo.galaxywrapper.gonsole_url_list")
    @Default("")
    List<String> getGonsoleUrls();

    @Config("arecibo.galaxywrapper.galaxy_command_path")
    @Default("")
    String getGalaxyCommandPath();

    @Config("arecibo.galaxywrapper.galaxy_command_timeout")
    @Default("30")
    int getGalaxyCommandTimeout();

    @Config("arecibo.galaxywrapper.core_type_filter")
    @DefaultNull
    String getCoreTypeFilter();

    @Config("arecibo.galaxywrapper.local_zone_filter")
    @DefaultNull
    String getLocalZoneFilter();

    @Config("arecibo.galaxywrapper.global_zone_filter")
    @DefaultNull
    String getGlobalZoneFilter();

    @Config("arecibo.galaxywrapper.galaxy_output_override_file")
    @Default("")
    String getGalaxyOutputOverrideFile();
}