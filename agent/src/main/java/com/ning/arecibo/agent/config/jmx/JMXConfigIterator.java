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
