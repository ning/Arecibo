package com.ning.arecibo.agent.config.snmp;

import java.util.concurrent.ConcurrentHashMap;
import com.google.inject.Inject;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigIterator;
import com.ning.arecibo.agent.config.ConfigIteratorFactory;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.agent.datasource.snmp.SNMPDataSource;
import com.ning.arecibo.agent.guice.AgentConfig;

public class SNMPConfigIteratorFactory implements ConfigIteratorFactory {
	//TODO: This whole business of recognizing a config type by scanning for delimiters needs to be overhauled
	//	the configs should be more declarative of the config type	
	public static final String SNMP_TABLE_ATTRIBUTE_DELIMITER = "table:";
	
	private final AgentConfig agentConfig;
	private final ConcurrentHashMap<String, SNMPDataSource> dataSourceCache;
	
	@Inject
	public SNMPConfigIteratorFactory(AgentConfig agentConfig) {
		this.agentConfig = agentConfig;
		this.dataSourceCache = new ConcurrentHashMap<String, SNMPDataSource>();
	}
	
	public synchronized ConfigIterator getConfigIterator(Config baseConfig) throws DataSourceException
	{
        if(!(baseConfig instanceof SNMPConfig)) {
            // shouldn't happen
            throw new DataSourceException("Intance of SNMPConfig required");
        }
        
		String dataSourceHashKey = baseConfig.getHost();
		SNMPDataSource dataSource = dataSourceCache.get(dataSourceHashKey);
		
		if(dataSource == null) {
			dataSource = new SNMPDataSource(baseConfig,
			                                agentConfig.getConnectionTimeoutInitial(),
			                                agentConfig.getSNMPCompiledMibDir());
			dataSourceCache.put(dataSourceHashKey,dataSource);
		}
			
		if(((SNMPConfig)baseConfig).getOidRow().trim().startsWith(SNMP_TABLE_ATTRIBUTE_DELIMITER)) {
			return new SNMPTableConfigIterator((SNMPConfig)baseConfig,dataSource);
		}
		else {
			return new SNMPConfigIterator((SNMPConfig)baseConfig,dataSource);
		}
	}

    public synchronized void finishConfigIteration() {
        for(SNMPDataSource dataSource:dataSourceCache.values()) {
            try {
                dataSource.closeResources();
            } catch(DataSourceException dsEx) {}
        }
        dataSourceCache.clear();
    }
}
