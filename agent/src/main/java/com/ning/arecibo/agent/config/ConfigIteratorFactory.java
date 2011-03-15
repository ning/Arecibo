package com.ning.arecibo.agent.config;

import com.ning.arecibo.agent.datasource.DataSourceException;

public interface ConfigIteratorFactory {
	public ConfigIterator getConfigIterator(Config baseConfig) throws DataSourceException;
}
