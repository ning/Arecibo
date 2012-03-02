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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.JMException;
import org.apache.commons.lang.StringUtils;
import com.ning.arecibo.agent.config.jmx.JMXConfig;
import com.ning.arecibo.agent.config.jmx.JMXEnumerator;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.galaxy.GalaxyCorePicker;
import com.ning.arecibo.util.galaxy.GalaxyCoreStatus;

/*
 ** used for debugging purposes, primarily
 */
public class GalaxyJMXDiscoveryConfigReader implements ConfigReader
{
	private static final Logger log = Logger.getLogger(GalaxyJMXDiscoveryConfigReader.class);
	
	private List<Config> configList = null;

	public GalaxyJMXDiscoveryConfigReader(GuiceDefaultsForDataSources guiceDefaultsForDataSources, GalaxyCorePicker galaxyCorePicker) throws ConfigException
	{
		try {
			configList = new ArrayList<Config>();
			
			List<GalaxyCoreStatus> list = galaxyCorePicker.getCores();
			
			for (GalaxyCoreStatus item : list) {
				if (StringUtils.equals(item.getRunStatus(), "running")) {
					log.info("surveying %s %s\n", item.getZoneHostName(), item.getCoreType());
	
					JMXEnumerator jenum = new JMXEnumerator(item.getZoneHostName() + ":" + guiceDefaultsForDataSources.getJmxPort());
					List<JMXEnumerator.MBeanAttribute> config = jenum.enumerate();
					if (config.size() > 0) {
						for ( JMXEnumerator.MBeanAttribute mbeanAttr : config ) {

                            Map<String, Object> optionsMap = new HashMap<String, Object>();
                            optionsMap.put(JMXConfig.OBJECT_NAME, mbeanAttr.getObjectName());
                            optionsMap.put(JMXConfig.ATTRIBUTE, mbeanAttr.getAttribute());
                            optionsMap.put(Config.EVENT_TYPE, mbeanAttr.getEventType());
                            optionsMap.put(Config.EVENT_ATTRIBUTE_TYPE, mbeanAttr.getAttribute());

                            Config c = new JMXConfig(item.getZoneHostName(), item.getConfigPath(), item.getCoreType(), guiceDefaultsForDataSources, optionsMap);

							configList.add(c);
						}
					}
				}
			}
		}
		catch(IOException ioEx) {
			throw new ConfigException("Problem in GalaxyJMXDiscoveryConfigReader",ioEx);
			
		}
		catch(JMException jmxEx) {
			throw new ConfigException("Problem in GalaxyJMXDiscoveryConfigReader",jmxEx);
		}
	}

    public List<Config> getConfigurations() {
	    return configList;
	}
}
