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

package com.ning.arecibo.agent.datasource.jmx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.commons.lang.StringUtils;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.jmx.JMXConfig;
import com.ning.arecibo.agent.datasource.DataSource;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.agent.datasource.DataSourceType;
import com.ning.arecibo.agent.datasource.ValueParser;
import com.ning.arecibo.agent.datasource.jmx.JMXClient.MBeanDescriptor;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.Pair;

public class JMXDataSource implements DataSource
{
	private static final Logger log = Logger.getLogger(JMXDataSource.class);

	protected final String host;
	protected final int port;
	protected final JMXClientCache jmxClientCache;
    protected final JMXParserManager JMXParserManager;
	protected volatile String jmxClientCacheKey;
	protected volatile JMXClient jmxClient;
	
	protected volatile String objectName;                  // may be modified by dynamic bean
	protected volatile MonitoredMBean monitoredMBean;
	protected volatile Set<String> attributes;
	protected volatile Map<String,String> configHashKeyMap;


    public JMXDataSource(Config config, JMXClientCache jmxClientCache, JMXParserManager JMXParserManager) throws DataSourceException
	{
        if(!(config instanceof JMXConfig)) {
            throw new DataSourceException("JMXDataSource must be initialized with an instance of JMXConfig");
        }

        JMXConfig jmxConfig = (JMXConfig)config;

		this.host = jmxConfig.getHost();
		this.port = jmxConfig.getPort();
		this.objectName = jmxConfig.getObjectName();
		this.jmxClientCache = jmxClientCache;
        this.JMXParserManager = JMXParserManager;
		this.jmxClientCacheKey = null;
		this.jmxClient = null;
	}

    @Override
	public synchronized void initialize()
		throws DataSourceException
	{
		closeResources();
		try {
			Pair<String,JMXClient> cachePair = jmxClientCache.acquireClient(this.host, this.port);
			this.jmxClientCacheKey = cachePair.getFirst();
			this.jmxClient = cachePair.getSecond();
			
			JMXClient.MBeanDescriptor mbeanDescriptor = getMBeanDescriptor(this.jmxClient, this.objectName);
			this.monitoredMBean = new MonitoredMBean(this.objectName, mbeanDescriptor, 1);
			this.attributes = new HashSet<String>();        // reset attributes sent to bean
			this.configHashKeyMap = new HashMap<String,String>();	
		}
		catch (RuntimeException e) {
			log.info("Unable to intialize JMX Client for %s", this.objectName);
			closeAndInvalidateClient();
			
			// throw this out, it will be caught and reported by the caller
			throw new DataSourceException("RuntimeException:",e);
		}
	}

    @Override
    public synchronized boolean isInitialized()
    {
        return jmxClient != null;
    }

    @Override
	public synchronized void closeResources()
		throws DataSourceException
	{
		if (this.jmxClientCacheKey != null) {
			try {
				jmxClientCache.releaseClient(this.jmxClientCacheKey);
			}
			catch (DataSourceException e) {
				// squash this exception, connection might be bad anyway
				log.info(e, "Problem closing JMX Client for %s, invalidating client", this.objectName);
				closeAndInvalidateClient();
			}
			finally {
				this.jmxClientCacheKey = null;
				this.jmxClient = null;
			}
		}
	}
	
	protected synchronized void closeAndInvalidateClient() {
		if (this.jmxClientCacheKey != null) {
			try {
				jmxClientCache.releaseClient(this.jmxClientCacheKey,true);
			}
			catch (DataSourceException e) {
				// squash this exception, connection might be bad anyway
				log.info(e, "Problem closingAndInvalidating JMX Client for %s", this.objectName);
			}
			finally {
				this.jmxClientCacheKey = null;
				this.jmxClient = null;
			}
		}
	}

	synchronized MBeanDescriptor getMBeanDescriptor(JMXClient client, String accessor)
		throws DataSourceException
	{
		JMXClient.MBeanDescriptor mbeanDescriptor;
		try {
			mbeanDescriptor = client.getMBean(accessor);
		}
		catch (Exception e) {
			closeResources();
			throw new DataSourceException(String.format("Unable to instantiate bean: '%s'", accessor), e);
		}
		if (mbeanDescriptor == null) {
			closeResources();
			throw new DataSourceException(String.format("Unable to instantiate bean; null returned for: '%s'", accessor));
		}
		return mbeanDescriptor;
	}

    @Override
    public boolean canExpandConfigs() {
        return false;
    }

    @Override
    public Map<String, Config> expandConfigs(Map<String, Config> configs) {
        throw new IllegalStateException("expandConfigs not supported");
    }

    @Override
	public void prepareConfig(Config config)
	{
        JMXConfig jmxConfig = (JMXConfig)config;
		this.attributes.add(jmxConfig.getAttribute());
		this.configHashKeyMap.put(jmxConfig.getAttribute(), jmxConfig.getConfigHashKey());
	}

    @Override
	public void finalizePreparation()
	{
		// now that set of desired attributes is complete, instruct mbean to return them
		this.monitoredMBean.setRelevantAttributes(this.attributes.toArray(new String[this.attributes.size()]));
        this.JMXParserManager.addParsers(this.monitoredMBean);
	}

    @Override
	public synchronized Map<String, Object> getValues()
		throws DataSourceException
	{
		if (this.jmxClient == null) {
			throw new DataSourceException("Client not initialized.");
		}
		
		try {
			Map<String, Object> rawValues = jmxClient.getAttributeValues(monitoredMBean.getMBeanDescriptor(), monitoredMBean.getRelevantAttributes());
			if (rawValues.size() == 0) {
				throw new DataSourceException(String.format("Problem: bean %s has returned no values for attributes '%s'", 
                        this.monitoredMBean.getName(), StringUtils.join(monitoredMBean.getRelevantAttributes(), ", ")));
			}

			Map<String, Object> resultMap = new HashMap<String, Object>();
			for (Map.Entry<String, Object> entry : rawValues.entrySet()) {
				ValueParser parser = monitoredMBean.getValueParser(entry.getKey());
				if (parser != null) {

					Map<String, Object> vals = parser.parse(monitoredMBean, entry.getKey(), entry.getValue());
					for (Map.Entry<String, Object> e : vals.entrySet()) {
						resultMap.put(configHashKeyMap.get(e.getKey()), e.getValue());
					}
				}
				else {
					resultMap.put(configHashKeyMap.get(entry.getKey()), entry.getValue());
				}
			}
			return resultMap;
		}
		catch(RuntimeException ruEx) {
			log.info("Unable to get values for %s", this.objectName);
			closeAndInvalidateClient();
			
			// throw this out as a DataSourceException
			throw new DataSourceException("RuntimeException:",ruEx);
		}
	}

	protected static boolean matchesAccessorType(String objectName) {
		
		try {
			// see if we can create an ObjectName
			ObjectName.getInstance(objectName);
			return true;
		}
		catch(MalformedObjectNameException moEx) {
			return false;
		}
	}
	
	public static boolean matchesConfig(Config config) {

        if(!(config instanceof JMXConfig))
            return false;

		return matchesAccessorType(((JMXConfig)config).getObjectName());
	}
	
	@Override
	public DataSourceType getDataSourceType() {
		return DataSourceType.JMX;
	}
}
