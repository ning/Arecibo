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

package com.ning.arecibo.agent.config.jmx;

import java.util.ArrayList;
import java.util.Iterator;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigIterator;
import com.ning.arecibo.agent.datasource.DataSourceException;


public class JMXConfigIterator implements ConfigIterator {
	
	private final JMXConfig config;
	private final ArrayList<JMXConfig> configList;
	private final Iterator<JMXConfig> configIterator;
	
	public JMXConfigIterator(Config config,JMXDynamicUtils jmxDynamicUtils) throws DataSourceException {

        if(!(config instanceof JMXConfig))
            throw new DataSourceException("Instance of JMXConfig expected");

		this.config = (JMXConfig)config;

		configList = new ArrayList<JMXConfig>();
		configList.add(this.config);
		
		// this will result in a possibly expanded configList
		jmxDynamicUtils.expandMBeanWildcards(configList);
		
		configIterator = configList.iterator();
	}
	
	public Config getNextConfig() throws DataSourceException {
		if(configIterator.hasNext())
			return configIterator.next();
		else
			return null;
	}
		
}
