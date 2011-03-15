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
