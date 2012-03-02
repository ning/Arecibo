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

package com.ning.arecibo.agent.datasource;

import com.google.inject.Inject;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigIteratorFactory;
import com.ning.arecibo.agent.config.jmx.JMXConfigIteratorFactory;
import com.ning.arecibo.agent.config.jmx.JMXDynamicUtils;
import com.ning.arecibo.agent.config.snmp.SNMPConfigIteratorFactory;
import com.ning.arecibo.agent.datasource.http.HTTPResponseCheckDataSource;
import com.ning.arecibo.agent.datasource.jmx.JMXClientCache;
import com.ning.arecibo.agent.datasource.jmx.JMXCompositeDataSource;
import com.ning.arecibo.agent.datasource.jmx.JMXDataSource;
import com.ning.arecibo.agent.datasource.jmx.JMXOperationInvocationDataSource;
import com.ning.arecibo.agent.datasource.jmx.JMXParserManager;
import com.ning.arecibo.agent.datasource.snmp.SNMPDataSource;
import com.ning.arecibo.agent.datasource.tcp.TCPConnectCheckDataSource;
import com.ning.arecibo.agent.datasource.tracer.TracerDataSource;
import com.ning.arecibo.agent.guice.AgentConfig;

//TODO: This whole business of recognizing a config type by scanning for delimiters needs to be overhauled
//	the configs should be more declarative of the config type
public class DataSourceUtils {
	
	private final AgentConfig agentConfig;
	private final JMXClientCache jmxClientCache;
	private final JMXDynamicUtils jmxDynamicUtils;
    private final JMXParserManager jmxParserManager;
	private final IdentityConfigIteratorFactory identityConfigIteratorFactory;
	private final JMXConfigIteratorFactory jmxConfigIteratorFactory;
	private final SNMPConfigIteratorFactory snmpConfigIteratorFactory;

    @Inject
	public DataSourceUtils(AgentConfig agentConfig,
            			   SNMPConfigIteratorFactory snmpConfigIteratorFactory,
            			   JMXClientCache jmxClientCache,
            			   JMXDynamicUtils jmxDynamicUtils,
                           JMXParserManager jmxParserManager,
						   JMXConfigIteratorFactory jmxConfigIteratorFactory,
                           IdentityConfigIteratorFactory identityConfigIteratorFactory) {
		
		this.agentConfig = agentConfig;
        this.jmxConfigIteratorFactory = jmxConfigIteratorFactory;
		this.jmxClientCache = jmxClientCache;
		this.jmxDynamicUtils = jmxDynamicUtils;
        this.jmxParserManager = jmxParserManager;
		this.identityConfigIteratorFactory = identityConfigIteratorFactory;
		this.snmpConfigIteratorFactory = snmpConfigIteratorFactory;
	}
	
	public DataSourceType getDataSourceType(Config config) throws DataSourceException {


		if(SNMPDataSource.matchesConfig(config)) {
			return DataSourceType.SNMP;
		}
		else if(TracerDataSource.matchesConfig(config)) {
			return DataSourceType.Tracer;
		}
		else if(JMXCompositeDataSource.matchesConfig(config)) {
			return DataSourceType.JMXComposite;
		}
		else if(JMXOperationInvocationDataSource.matchesConfig(config)) {
			return DataSourceType.JMXOperationInvocation;
		}
		else if(JMXDataSource.matchesConfig(config)) {
			return DataSourceType.JMX;
		}
        else if(HTTPResponseCheckDataSource.matchesConfig(config)) {
            return DataSourceType.HTTPResponseCheck;
        }
        else if(TCPConnectCheckDataSource.matchesConfig(config)) {
            return DataSourceType.TCPConnectCheck;
        }
		else {
			throw new DataSourceException("Could not determine datasource type from config: " + config.getConfigType());
		}
	}
	
	public DataSourceIteratorFactoryType getDataSourceIteratorType(Config config) throws DataSourceException {
		
		DataSourceType dsType = getDataSourceType(config);
		
		switch(dsType) {
			case JMX:
			case JMXComposite:
			case JMXOperationInvocation:
				return DataSourceIteratorFactoryType.JMX;
				
			case SNMP:
				return DataSourceIteratorFactoryType.SNMP;
				
			default:
				return DataSourceIteratorFactoryType.Identity;
		}
	}
	
	public DataSource getDataSource(Config config) throws DataSourceException
	{
		DataSourceType dsType = getDataSourceType(config);
		switch(dsType) {
			case SNMP:
				return new SNMPDataSource(config, agentConfig.getConnectionTimeout(), agentConfig.getSNMPCompiledMibDir());
			case Tracer:
				return new TracerDataSource(config);
			case JMXComposite:
				return new JMXCompositeDataSource(config, this.jmxClientCache, this.jmxParserManager);
			case JMXOperationInvocation:
				return new JMXOperationInvocationDataSource(config, this.jmxClientCache, this.jmxParserManager);
			case JMX:
				return new JMXDataSource(config, this.jmxClientCache, this.jmxParserManager);
            case HTTPResponseCheck:
                return new HTTPResponseCheckDataSource(config,
                                                       agentConfig.getConnectionTimeout(),
                                                       agentConfig.getHTTPUserAgent(),
                                                       agentConfig.getHTTPProxyHost(),
                                                       agentConfig.getHTTPProxyPort());
            case TCPConnectCheck:
                return new TCPConnectCheckDataSource(config, agentConfig.getConnectionTimeout());
			default:
				throw new DataSourceException("Could not create data source, unknown datasource type: " + dsType);
		}
	}	
	
	public ConfigIteratorFactory getConfigIteratorFactory(Config config) throws DataSourceException {
		
		DataSourceIteratorFactoryType dsifType = getDataSourceIteratorType(config);
		
		switch(dsifType) {
			case JMX:
				return jmxConfigIteratorFactory;
			case SNMP:
				return snmpConfigIteratorFactory;
			default:
				return identityConfigIteratorFactory;
		}
	}
	
	public void startConfigExpansion() 
			throws DataSourceException
	{
		jmxDynamicUtils.startClientCacheRetention();
	}
	
	public void finishConfigExpansion() 
			throws DataSourceException
	{
		jmxDynamicUtils.finishClientCacheRetention();
        snmpConfigIteratorFactory.finishConfigIteration();
	}
}
