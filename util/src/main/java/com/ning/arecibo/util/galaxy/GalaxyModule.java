package com.ning.arecibo.util.galaxy;

import java.util.Arrays;
import com.google.inject.AbstractModule;

public class GalaxyModule extends AbstractModule
{
    @Override
	public void configure()
	{
        String gonsoleUrlList = System.getProperty("arecibo.galaxywrapper.gonsole_url_list", "");
        String galaxyCommandPath = System.getProperty("arecibo.galaxywrapper.galaxy_command_path", "");
        int galaxyCommandTimeout = Integer.getInteger("arecibo.galaxywrapper.galaxy_command_timeout", 30);
        String coreTypeFilter = System.getProperty("arecibo.galaxywrapper.core_type_filter");
        String localZoneFilter = System.getProperty("arecibo.galaxywrapper.local_zone_filter");
        String globalZoneFilter = System.getProperty("arecibo.galaxywrapper.global_zone_filter");
        String galaxyOutputOverrideFile = System.getProperty("arecibo.galaxywrapper.galaxy_output_override_file", "");

        GalaxyConfig config = new GalaxyConfig(Arrays.asList(gonsoleUrlList.split("\\s*,\\s*")),
                                               galaxyCommandPath,
                                               galaxyCommandTimeout,
                                               coreTypeFilter,
                                               localZoneFilter,
                                               globalZoneFilter,
                                               galaxyOutputOverrideFile);

        bind(GalaxyConfig.class).toInstance(config);
	}
}
