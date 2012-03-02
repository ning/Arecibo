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

package com.ning.arecibo.agent.datasource.jmx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.management.openmbean.CompositeData;
import org.apache.commons.lang.StringUtils;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.jmx.JMXConfig;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.agent.datasource.DataSourceType;
import com.ning.arecibo.util.Logger;

public class JMXCompositeDataSource extends JMXDataSource
{
	private static final Logger log = Logger.getLogger(JMXCompositeDataSource.class);

	private static final String compositeAttributeDelimiter = "@";

	private volatile Map<String, Set<String>> attributeMap ;

	public JMXCompositeDataSource(Config config, JMXClientCache jmxClientCache, JMXParserManager jmxParserManager) throws DataSourceException
	{
		super(config, jmxClientCache, jmxParserManager);
	}
	
	@Override
	public void finalizePreparation() {
		this.attributeMap = new HashMap<String, Set<String>>(); // keep a separate map for composites
		mapAttributes(this.attributes);

		// now set the attributes in the datasource
		super.finalizePreparation();
	}

	@Override
	public synchronized Map<String, Object> getValues()
		throws DataSourceException
	{
		if (this.jmxClient == null) {
			throw new DataSourceException("Client not initialized.");
		}

		try {
			String[] attributes = attributeMap.keySet().toArray(new String[attributeMap.keySet().size()]);     // these are the attributes in front of the compositeAttributeDelimeter
			Map<String, Object> rawValues = this.jmxClient.getAttributeValues(monitoredMBean.getMBeanDescriptor(), attributes); // get composites
			if (rawValues.size() == 0) {
				log.warn("Problem: bean %s has returned no pairs for attributes '%s'", this.monitoredMBean.getName(), StringUtils.join(monitoredMBean.getRelevantAttributes(), ", "));
				return null;
			}
			Map<String, Object> resultMap = new HashMap<String, Object>();
			for (Map.Entry<String, Object> entry : rawValues.entrySet()) {
				if (entry.getValue() instanceof CompositeData) {
					CompositeData cd = (CompositeData) entry.getValue();
					String[] sought = attributeMap.get(entry.getKey()).toArray(new String[attributeMap.get(entry.getKey()).size()]);
					Object[] values = cd.getAll(sought);    // get just the requested values
					for (int i = 0; i < sought.length; i++) {
						resultMap.put(configHashKeyMap.get(entry.getKey() + compositeAttributeDelimiter + sought[i]), values[i]);
					}
				}
				else {
					resultMap.put(configHashKeyMap.get(entry.getKey()), entry.getValue());
				}
			}
			return resultMap;
		}
		catch(RuntimeException ruEx) {
			log.info("Unable to get values for %s", this.objectName);
			closeAndInvalidateClient();
			
			// throw this out as a DataSourceException
			throw new DataSourceException("RuntimeException:",ruEx);
		}	
	}

	private void mapAttributes(Set<String> attributes)
	{
		for (String attributeStr : attributes) {
			String[] attrs = attributeStr.split(compositeAttributeDelimiter);
			if (attrs.length == 2) {
				String attribute = attrs[0];
				String part = attrs[1];
				Set<String> partsList = attributeMap.get(attribute);
				if (partsList == null) {
					partsList = new HashSet<String>();
					partsList.add(part);
					attributeMap.put(attribute, partsList);
					continue;
				}
				partsList.add(part);
			}
			else if (attrs.length == 1) {
				//nonComposites.add(attrs[0]);
				attributeMap.put(attrs[0], new HashSet<String>());
			}
		}
	}

	private static boolean matchesAttributeType(String attribute) {
		if(attribute.contains(compositeAttributeDelimiter))
			return true;
		
		return false;
	}
	
	public static boolean matchesConfig(Config config) {

        if (!(config instanceof JMXConfig))
            return false;

        return matchesAccessorType(((JMXConfig)config).getObjectName()) && matchesAttributeType(((JMXConfig)config).getAttribute());
	}
	
	@Override
	public DataSourceType getDataSourceType() {
		return DataSourceType.JMXComposite;
	}
	
}
