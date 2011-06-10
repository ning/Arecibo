package com.ning.arecibo.agent.datasource;

import com.google.inject.Inject;
import com.ning.arecibo.agent.config.AgentConfig;
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


//TODO: This whole business of recognizing a config type by scanning for delimiters needs to be overhauled
//	the configs should be more declarative of the config type
public class DataSourceUtils {
	
	private int timeout;
	private String snmpCompiledMibDir;
	private final JMXClientCache jmxClientCache;
	private final JMXDynamicUtils jmxDynamicUtils;
    private final JMXParserManager jmxParserManager;
	private final IdentityConfigIteratorFactory identityConfigIteratorFactory;
	private final JMXConfigIteratorFactory jmxConfigIteratorFactory;
	private final SNMPConfigIteratorFactory snmpConfigIteratorFactory;
    private final String httpUserAgentString;
    private final String httpProxyHost;
    private final int httpProxyPort;

    @Inject
	public DataSourceUtils(AgentConfig config,
                           SNMPConfigIteratorFactory snmpConfigIteratorFactory,
                           JMXClientCache jmxClientCache,
                           JMXDynamicUtils jmxDynamicUtils,
                           JMXParserManager jmxParserManager,
                           JMXConfigIteratorFactory jmxConfigIteratorFactory,
                           IdentityConfigIteratorFactory identityConfigIteratorFactory) {
		
		this.timeout = config.getMonitorConnectionTimeout();
		this.snmpCompiledMibDir = config.getMonitorSnmpCompiledMibDir();
        this.jmxConfigIteratorFactory = jmxConfigIteratorFactory;
		this.jmxClientCache = jmxClientCache;
		this.jmxDynamicUtils = jmxDynamicUtils;
        this.jmxParserManager = jmxParserManager;
		this.identityConfigIteratorFactory = identityConfigIteratorFactory;
		this.snmpConfigIteratorFactory = snmpConfigIteratorFactory;
        this.httpUserAgentString = config.getMonitorHttpUserAgent();
        this.httpProxyHost = config.getMonitorHttpProxyHost();
        this.httpProxyPort = config.getMonitorHttpProxyPort();
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
				return new SNMPDataSource(config, this.timeout, this.snmpCompiledMibDir);
			case Tracer:
				return new TracerDataSource(config);
			case JMXComposite:
				return new JMXCompositeDataSource(config, this.timeout, this.jmxClientCache, this.jmxParserManager);
			case JMXOperationInvocation:
				return new JMXOperationInvocationDataSource(config, this.timeout, this.jmxClientCache, this.jmxParserManager);
			case JMX:
				return new JMXDataSource(config, this.timeout, this.jmxClientCache, this.jmxParserManager);
            case HTTPResponseCheck:
                return new HTTPResponseCheckDataSource(config,this.timeout,this.httpUserAgentString,this.httpProxyHost,this.httpProxyPort);
            case TCPConnectCheck:
                return new TCPConnectCheckDataSource(config,this.timeout);
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
