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