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

import com.google.inject.Inject;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigIterator;
import com.ning.arecibo.agent.config.ConfigIteratorFactory;
import com.ning.arecibo.agent.datasource.DataSourceException;
	
import com.ning.arecibo.util.Logger;

public class JMXConfigIteratorFactory implements ConfigIteratorFactory {
	private static final Logger log = Logger.getLogger(JMXConfigIteratorFactory.class);
	
	private final JMXDynamicUtils jmxDynamicUtils;
	
	@Inject
	public JMXConfigIteratorFactory(JMXDynamicUtils jmxDynamicUtils) {
		this.jmxDynamicUtils = jmxDynamicUtils;
	}
	
	public ConfigIterator getConfigIterator(Config baseConfig) throws DataSourceException
	{
		return new JMXConfigIterator(baseConfig,jmxDynamicUtils);
	}
}
