package com.ning.arecibo.agent.config;

import com.ning.arecibo.agent.datasource.DataSourceException;

public interface ConfigIterator {
	public Config getNextConfig() throws DataSourceException;
}
