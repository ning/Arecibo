package com.ning.arecibo.agent.config;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import com.google.inject.Inject;
import com.ning.arecibo.agent.config.jmx.JMXMonitoringProfilePoller;
import com.ning.arecibo.agent.guice.ConfigInitTypeParam;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;
import com.ning.arecibo.agent.guice.GuicePropsForExplicitMode;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.galaxy.GalaxyCorePicker;

public class ConfigInitializer
{
	private static final Logger log = Logger.getLogger(ConfigInitializer.class);

	private final ConfigInitType configInitType;
	private final GuiceDefaultsForDataSources guiceDefaultsForDataSources;
	private final GuicePropsForExplicitMode guicePropsForExplicitMode ;
	private final GalaxyCorePicker galaxyCorePicker;
    private final ConfigFileUtils configFileUtils;

    @Inject
	public ConfigInitializer(@ConfigInitTypeParam ConfigInitType configInitType,
							GalaxyCorePicker galaxyCorePicker,
                            ConfigFileUtils configFileUtils,
							GuiceDefaultsForDataSources guiceDefaultsForDataSources,
							GuicePropsForExplicitMode guicePropsForExplicitMode)
	{
		this.configInitType = configInitType;
		this.galaxyCorePicker = galaxyCorePicker;
        this.configFileUtils = configFileUtils;
		this.guiceDefaultsForDataSources = guiceDefaultsForDataSources;
		this.guicePropsForExplicitMode = guicePropsForExplicitMode;
	}

	private ConfigReader getConfigViaExplicitParams() throws ConfigException
	{
	    // This mode is primarily for testing
		//
		// Get config params from lists of local monitoring config filenames, host names, and config paths...
		//  if the number of entries in one of the lists is less than the others, the last entry in the shorter list will
		//  be replicated for each remaining config set.
		//  Thus if there is only one config file name, but 10 hostnames, then the same config file will be applied to all 10 hosts, etc.
		//  (Note it's a bit inefficient therefore, as it will repeatedly open and close the same input file)...
		//
	    
	    List<String> configFileList = guicePropsForExplicitMode.getExplicitConfigFileList();
	    List<String> hostList = guicePropsForExplicitMode.getExplicitHostList();
	    List<String> pathList = guicePropsForExplicitMode.getExplicitPathList();
	    List<String> typeList = guicePropsForExplicitMode.getExplicitTypeList();
	    
	    if(hostList == null || typeList == null || pathList == null) {
	        log.debug("Could not configure via explicit params, must have non-null params for hostList, typeList and pathList");
	        return null;
	    }
	
	    Iterator<String> configFileIter = configFileList.iterator();
	    Iterator<String> hostIter = hostList.iterator();
	    Iterator<String> pathIter = pathList.iterator();
	    Iterator<String> typeIter = typeList.iterator();
	    
	    String configFile = null;
	    String host = null;
	    String path = null;
	    String type = null;
	    
	    CompoundConfigReader configReader = new CompoundConfigReader();
	    
		while (hostIter.hasNext() || typeIter.hasNext() || pathIter.hasNext()) {

			if (configFileIter.hasNext()) {
				configFile = configFileIter.next();
			}

			if (hostIter.hasNext()) {
				host = hostIter.next();
			}

			if (pathIter.hasNext()) {
				path = pathIter.next();
			}

			if (typeIter.hasNext()) {
				type = typeIter.next();
			}

			try {
				if(configFile != null) {
					log.info("Adding initial configuration with config file '" + configFile + "', host '" + host + "', path '" + path + "', type '" + type);
					Reader configFileReader = new FileReader(configFile);
					
					configReader.addConfigReader(new ConfigStreamReader(configFileReader, host, path, type, guiceDefaultsForDataSources));
				}
				else {
					log.info("Adding initial configuration with host '" + host + "', path '" + path + "', type '" + type);
					List<InputStream> configResourceStreams = configFileUtils.getMonitoringTypeConfigStreamList(type,path,false);
					for(InputStream configResourceStream:configResourceStreams) {
						InputStreamReader configStreamReader = new InputStreamReader(configResourceStream);

                        configReader.addConfigReader(new ConfigStreamReader(configStreamReader, host, path, type, guiceDefaultsForDataSources));
					}
				}

                if(guiceDefaultsForDataSources.isJMXMonitoringProfilePollingEnabled()) {
				    configReader.addConfigReader(new JMXMonitoringProfilePoller(host, path, type, guiceDefaultsForDataSources));
                }
			}
			catch (FileNotFoundException fnfEx) {
				log.warn("Could not open file %s: %s",configFile,fnfEx);
				throw new ConfigException("Could not open file " + configFile,fnfEx);
			}
		}

		return configReader;
	}

	private ConfigReader getConfigViaGalaxyAndCoreTypeConfig() throws ConfigException
	{
		ConfigReader configReader = new GalaxyOutputConfigReader(guiceDefaultsForDataSources, galaxyCorePicker, configFileUtils);
				
		return configReader;
	}

	private ConfigReader getConfigViaGalaxyAndDiscovery() throws ConfigException
	{
	    GalaxyJMXDiscoveryConfigReader galaxyDiscoverer = new GalaxyJMXDiscoveryConfigReader(guiceDefaultsForDataSources,galaxyCorePicker);
				
		return galaxyDiscoverer;
	}

	public List<Config> getCurrentConfigList() throws ConfigException
	{

		ConfigReader configReader = null;

		if (configInitType == ConfigInitType.CONFIG_BY_EXPLICIT_PARAMS) {
			configReader = getConfigViaExplicitParams();
		}
		else if(configInitType == ConfigInitType.CONFIG_BY_CORE_TYPE_VIA_GALAXY_OUTPUT) {
			configReader = getConfigViaGalaxyAndCoreTypeConfig();
		}
		else if(configInitType == ConfigInitType.CONFIG_BY_DISCOVERY_VIA_GALAXY_OUTPUT) {
			configReader = getConfigViaGalaxyAndDiscovery();
		}

		List<Config> configList = null;
		
		if(configReader != null) {
		    configList = configReader.getConfigurations();
		}
		
		if (configList != null && configList.size() > 0) {
			return configList;
		}
		else {
			log.info("No monitoring configurations initialized");
			return null;
		}
	}

    public List<Config> getExclusionList() throws ConfigException
    {
        try {

            InputStream inputStream = this.configFileUtils.getDefaultConfigSteam(ConfigType.EXCLUSION);

            try {
                InputStreamReader streamReader = null;
                try {
                    streamReader = new InputStreamReader(inputStream);

                    ConfigReader configReader = new ConfigStreamReader(streamReader, null, null, null, null);
                    return configReader.getConfigurations();
                }
                finally {
                    if (streamReader != null) {
                        streamReader.close();
                    }
                }
            }
            finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
        catch(IOException ioEx) {
            throw new ConfigException("Problem retrieving exclusion config list", ioEx);
        }
    }
}
