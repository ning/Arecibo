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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import com.ning.arecibo.agent.config.jmx.JMXMonitoringProfilePoller;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.galaxy.GalaxyCorePicker;
import com.ning.arecibo.util.galaxy.GalaxyCoreStatus;

public class GalaxyOutputConfigReader extends CompoundConfigReader {
	private static final Logger log = Logger.getLogger(GalaxyOutputConfigReader.class);

	public GalaxyOutputConfigReader(GuiceDefaultsForDataSources guiceDefaultsForDataSources, GalaxyCorePicker galaxyCorePicker, ConfigFileUtils configFileUtils) throws ConfigException
	{
		try {
			List<GalaxyCoreStatus> list = galaxyCorePicker.getCores();

			for (GalaxyCoreStatus coreStatus : list ) {
	
			    List<InputStream> monitoringConfigStreamList = null;
			    
			    monitoringConfigStreamList = configFileUtils.getMonitoringTypeWithGalaxyPathConfigStreamList(coreStatus.getCoreType(),coreStatus.getConfigPath());
			    
			    if(monitoringConfigStreamList == null) {
			    	log.info("Skipping monitoring for server running on zone: " + coreStatus.getZoneHostName());
			    	log.warn("    Couldn't retrieve hierarchically composed monitoring config file for core type: " + coreStatus.getCoreType());
			        
			    	continue;
			    }
			    
			    try {
			    	for(InputStream monitoringConfigStream:monitoringConfigStreamList) {
			    		
			    		InputStreamReader streamReader = null;
			    		try {
			    			streamReader = new InputStreamReader(monitoringConfigStream);
			    			
			        		ConfigReader configReader = new ConfigStreamReader(streamReader,coreStatus.getZoneHostName(),
			                                                       coreStatus.getConfigPath(), coreStatus.getCoreType(), guiceDefaultsForDataSources);
			    
			    			if(configReader != null)
			        			this.addConfigReader(configReader);
			    		}
			    		finally {
			        		if(streamReader != null) {
			            		streamReader.close();
			        		}
			    		}
			    	}
			    }
			    finally {
			    	for(InputStream monitoringConfigStream:monitoringConfigStreamList) {
			        	if(monitoringConfigStream != null) {
			        		monitoringConfigStream.close();
			        	}
			    	}
			    }

                if(guiceDefaultsForDataSources.isJMXMonitoringProfilePollingEnabled()) {
				    this.addConfigReader(new JMXMonitoringProfilePoller(coreStatus, guiceDefaultsForDataSources));
                }
			}
		}
		catch(IOException ioEx) {
			throw new ConfigException("Problem retrieving Galaxy output",ioEx);
		}
	}
}
