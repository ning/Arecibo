package com.ning.arecibo.agent.datasource;

import java.util.Map;
import com.ning.arecibo.agent.config.Config;


public interface DataSource
{
	public boolean isInitialized();
    public void initialize() throws DataSourceException;
    public void closeResources() throws DataSourceException;
    public boolean canExpandConfigs();
    Map<String, Config> expandConfigs(Map<String, Config> configs) throws DataSourceException;
	public void prepareConfig(Config config) throws DataSourceException;
	public void finalizePreparation() throws DataSourceException;
	public Map<String, Object> getValues()  throws DataSourceException;
	
	public DataSourceType getDataSourceType();
}
