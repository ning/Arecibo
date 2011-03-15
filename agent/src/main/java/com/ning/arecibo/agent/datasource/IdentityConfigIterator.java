package com.ning.arecibo.agent.datasource;

import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigIterator;

public class IdentityConfigIterator implements ConfigIterator {
	
	private final Config config;
	private volatile boolean allocated = false;
	
	public IdentityConfigIterator(Config config) {
		this.config = config;
	}
	
	public Config getNextConfig() throws DataSourceException {
		if(!allocated) {
			allocated = true;
			return config;
		}
		else
			return null;
	}
}
