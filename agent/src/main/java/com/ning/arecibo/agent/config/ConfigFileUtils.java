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

package com.ning.arecibo.agent.config;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.util.Set;

import com.ning.arecibo.util.Logger;
import com.google.inject.Inject;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;

public class ConfigFileUtils {
	
	private static final Logger log = Logger.getLogger(ConfigFileUtils.class);
	
	public final static String CONFIG_FILE_SUFFIX = "_cfg.json";
	public final static String DEFAULT_CONFIG_FILE_PREFIX = "default";
	public final static String CONFIG_FILE_SUB_PATH_DELIMITER = "#";

    private final GuiceDefaultsForDataSources guiceDefaults;
    private final String[] defaultPathParts = {""};

    @Inject
    public ConfigFileUtils(GuiceDefaultsForDataSources guiceDefaults) {
        this.guiceDefaults = guiceDefaults;
    }

    public List<InputStream> getMonitoringTypeWithGalaxyPathConfigStreamList(String monitoringType, String configPath) {
        return getMonitoringTypeConfigStreamList(monitoringType,configPath,true,4);
	}
	
	public List<InputStream> getMonitoringTypeConfigStreamList(String monitoringType,String configPath) {
		return getMonitoringTypeConfigStreamList(monitoringType,configPath,true);
	}
	
	public List<InputStream> getMonitoringTypeConfigStreamList(String monitoringType,String configPath,boolean includeDefault) {
		return getMonitoringTypeConfigStreamList(monitoringType,configPath,includeDefault,1);
	}
	
	public List<InputStream> getMonitoringTypeConfigStreamList(String monitoringType,String configPath,boolean includeDefault,int pathPartOffset) {
        return getConfigStreamList(monitoringType,configPath,includeDefault,pathPartOffset,guiceDefaults.getConfigTypesEnabled());
    }

    public InputStream getDefaultConfigSteam(ConfigType configType) {
        Set<ConfigType> configTypes = new HashSet<ConfigType>();
        configTypes.add(configType);

        List<InputStream> streamList = getConfigStreamList(null,null,true,-1,configTypes);

        // streamList should only contain at most 1 entry
        if(streamList != null && streamList.size() >= 1)
            return streamList.get(0);
        else
            return null;
    }

    public List<InputStream> getConfigStreamList(String monitoringType,String configPath,boolean includeDefault,int pathPartOffset, Set<ConfigType> configTypes) {

		ArrayList<InputStream> retList = new ArrayList<InputStream>();

		ClassLoader cLoader = Thread.currentThread().getContextClassLoader();

        // do per core type
        if(monitoringType != null) {

            String[] pathParts;
            if(configPath != null)
                pathParts = configPath.split("/");
            else
                pathParts = defaultPathParts;

            for(ConfigType configType:configTypes) {
                addPerConfigTypeFile(configType,monitoringType,pathParts,pathPartOffset,cLoader,retList);
            }
            if(retList.size() == 0) {
                addPerConfigTypeFile(null,monitoringType,pathParts,pathPartOffset,cLoader,retList);
            }
        }

		//try default config file
		if(includeDefault) {
            for(ConfigType configType:configTypes) {
                addPerConfigTypeFile(configType,DEFAULT_CONFIG_FILE_PREFIX,defaultPathParts,1,cLoader,retList);
            }
		}
		
		//warn if no configs found
		if(retList.size() == 0) {
		    log.info("Could not find any applicable monitoring config entries for coreType = '" + monitoringType + "', configPath = '" + configPath + "'");
		}
		
		return retList;
	}


    private void addPerConfigTypeFile(ConfigType configType,
                                      String monitoringType,
                                      String[] pathParts,
                                      int pathPartOffset,
                                      ClassLoader cLoader,
                                      List<InputStream> retList) {

        // try to find the most specific file name match incorporating any configPath elements
        // we start with the 4th part and higher
        for(int i=pathParts.length; i>=pathPartOffset; i--) {

            String monitoringTypeConfigFileName = monitoringType;

            if(configType != null)
                monitoringTypeConfigFileName += "_" + configType.toString().toLowerCase();

            for(int j=pathPartOffset;j<i;j++) {
                monitoringTypeConfigFileName += CONFIG_FILE_SUB_PATH_DELIMITER + pathParts[j];
            }

            monitoringTypeConfigFileName += CONFIG_FILE_SUFFIX;
            log.debug("[** Looking for config file '" + monitoringTypeConfigFileName + "']");

            //try specific monitoring type config file
            InputStream monitoringTypeStream = cLoader.getResourceAsStream(monitoringTypeConfigFileName);
            if(monitoringTypeStream != null) {
                log.info("Loading config entries from '" + monitoringTypeConfigFileName + "'");
                retList.add(monitoringTypeStream);

                break;
            }
            else {
                log.debug("\tCould not load config file '" + monitoringTypeConfigFileName + "'");
            }
        }
    }
}
