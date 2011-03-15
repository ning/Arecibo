package com.ning.arecibo.util.galaxy;

import java.util.ArrayList;
import java.util.List;

public class GalaxyConfig
{
    private final List<String> gonsoleUrls;
    private final String galaxyCommandPath;
    private final int galaxyCommandTimeout;
    private final String coreTypeFilter;
    private final String localZoneFilter;
    private final String globalZoneFilter;
    private final String galaxyOutputOverrideFile;

    public GalaxyConfig(List<String> gonsoleUrls,
                        String galaxyCommandPath,
                        int galaxyCommandTimeout,
                        String coreTypeFilter,
                        String localZoneFilter,
                        String globalZoneFilter,
                        String galaxyOutputOverrideFile)
    {
        this.gonsoleUrls = new ArrayList<String>(gonsoleUrls);
        this.galaxyCommandPath = galaxyCommandPath;
        this.galaxyCommandTimeout = galaxyCommandTimeout;
        this.coreTypeFilter = coreTypeFilter;
        this.localZoneFilter = localZoneFilter;
        this.globalZoneFilter = globalZoneFilter;
        this.galaxyOutputOverrideFile = galaxyOutputOverrideFile;
    }

    public List<String> getGonsoleUrls()
    {
        return gonsoleUrls;
    }

    public String getGalaxyCommandPath()
    {
        return galaxyCommandPath;
    }

    public int getGalaxyCommandTimeout()
    {
        return galaxyCommandTimeout;
    }

    public String getCoreTypeFilter()
    {
        return coreTypeFilter;
    }

    public String getLocalZoneFilter()
    {
        return localZoneFilter;
    }

    public String getGlobalZoneFilter()
    {
        return globalZoneFilter;
    }

    public String getGalaxyOutputOverrideFile()
    {
        return galaxyOutputOverrideFile;
    }
}