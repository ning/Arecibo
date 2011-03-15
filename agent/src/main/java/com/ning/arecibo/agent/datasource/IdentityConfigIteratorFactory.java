package com.ning.arecibo.agent.datasource;

import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigIterator;
import com.ning.arecibo.agent.config.ConfigIteratorFactory;

public class IdentityConfigIteratorFactory implements ConfigIteratorFactory {

	public ConfigIterator getConfigIterator(Config baseConfig) throws DataSourceException
	{
		return new IdentityConfigIterator(baseConfig);
	}
}
