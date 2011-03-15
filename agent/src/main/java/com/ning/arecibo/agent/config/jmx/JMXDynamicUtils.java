package com.ning.arecibo.agent.config.jmx;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.inject.Inject;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.agent.datasource.jmx.JMXClient;
import com.ning.arecibo.agent.datasource.jmx.JMXClientCache;
import com.ning.arecibo.agent.datasource.jmx.MonitoredMBean;
import com.ning.arecibo.agent.guice.ConnectionTimeout;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.Pair;

public final class JMXDynamicUtils
{
	private static final Logger log = Logger.getLogger(JMXDynamicUtils.class);

    public final static String wildCardDelim = "*";
	
	private final int connectionTimeout;
	private final JMXClientCache jmxClientCache;
	private Stack<String> jmxClientCacheKeys = null;
	
	@Inject
	public JMXDynamicUtils(@ConnectionTimeout int connectionTimeout,
							JMXClientCache jmxClientCache) {
		
		this.connectionTimeout = connectionTimeout;
		this.jmxClientCache = jmxClientCache;
	}
	
	public synchronized void startClientCacheRetention() 
			throws DataSourceException
	{
		// keep track of previously opened connections,
		// so don't need to keep reopening and closing jmxClient connections to the same host
		// if there is an error in the middle, then we need to release all connections
		if(jmxClientCacheKeys != null) {
			throw new DataSourceException("Simultaneous usage of jmx client cache retention not allowed: clientCacheKey stack is already initialized");
		}
		else {
			jmxClientCacheKeys = new Stack<String>();
		}
	}
	
	public synchronized void finishClientCacheRetention()
			throws DataSourceException
	{
		if(jmxClientCacheKeys == null) {
			throw new DataSourceException("Simultaneous usage of jmx client cache retention not allowed: clientCacheKey stack is already null");
		}
		
		while(!jmxClientCacheKeys.empty()) {
			String cacheKey = popCachedClientKey();
			
			try {
				jmxClientCache.releaseClient(cacheKey);
			}
			catch(DataSourceException dsEx) {
				// don't throw this out here, since we just want to close down the stack
				log.info("DataSourceException: " + dsEx);
			}
		}
		
		jmxClientCacheKeys = null;
	}

	private synchronized void pushCachedClientKey(String jmxClientCacheKey) 
			throws DataSourceException
	{
		if(jmxClientCacheKeys == null) {
			throw new DataSourceException("Could not push jmxClientKey: clientCacheKey stack is null");
		}
		
		jmxClientCacheKeys.push(jmxClientCacheKey);
	}
			
	private synchronized String popCachedClientKey()
			throws DataSourceException
	{
		if(jmxClientCacheKeys == null) {
			throw new DataSourceException("Could not pop jmxClientKey: clientCacheKey stack is null");
		}
		else if(jmxClientCacheKeys.size() == 0) {
			throw new DataSourceException("Could not pop jmxClientKey: clientCacheKey stack is empty");
		}
		
		return jmxClientCacheKeys.pop();
	}
				
	public void expandMBeanWildcards(List<JMXConfig> configList) {
		
		expandWildcardedAccessors(configList);
		expandWildcardedAttributes(configList);
	}
	
	public void expandWildcardedAccessors(List<JMXConfig> configList) {
		
		if (configList==null) { return; }
		
		//
		// first process wildcarded accessors
		//
		List<JMXConfig> toAdd = new ArrayList<JMXConfig>();
		List<JMXConfig> toRemove = new ArrayList<JMXConfig>();

		for (JMXConfig config : configList) {
            try {
                toRemove.add(config);
                List<JMXConfig> expandedConfigs = expandWildcardedAccessorsAndEventNames(config);

                if(log.isDebugEnabled() && (
                        (expandedConfigs.size() > 1 ||
                        (expandedConfigs.size() == 1 && !expandedConfigs.get(0).getConfigDescriptor().equals(config.getConfigDescriptor())))))
                    log.debug("Expanded %d MBeans from '%s'. Removing base one.", expandedConfigs.size(), config.getConfigDescriptor());

                toAdd.addAll(expandedConfigs);
            }
            catch(DataSourceException dsEx) {
                toRemove.add(config);
                log.debug("Skipping config %s: %s",config.getConfigHashKey(),dsEx);
            }
		}
		for (JMXConfig config : toRemove) {
			configList.remove(config);
		}
		configList.addAll(toAdd);
	}
	
	public void expandWildcardedAttributes(List<JMXConfig> configList) {
		
		if (configList==null) { return; }
		
		//
		// first process wildcarded accessors
		//
		List<JMXConfig> toAdd = new ArrayList<JMXConfig>();
		List<JMXConfig> toRemove = new ArrayList<JMXConfig>();

		for (JMXConfig config : configList) {
			// if config still has dynamic field, it must have a dynamic attribute at this point
			// config can't have a dynamic key name without a dynamic attribute, but vice-versa is possible
			if (config.isDynamic()) {
				try {
					toRemove.add(config);
					List<JMXConfig> expandedConfigs = expandWildcardedAttributesAndKeyNames(config);
					
					if(log.isDebugEnabled() && 
							(expandedConfigs.size() > 1 ||
							(expandedConfigs.size() == 1 && !expandedConfigs.get(0).getConfigDescriptor().equals(config.getConfigDescriptor()))))
						log.debug("Expanded %d attributes from '%s'. Removing base one.", expandedConfigs.size(), config.getConfigDescriptor());
					
					toAdd.addAll(expandedConfigs);
				}
				catch(DataSourceException dsEx) {
					toRemove.add(config);
					log.debug(dsEx,"DataSourceException:");
				}
			}
		}
		for (JMXConfig config : toRemove) {
			configList.remove(config);
		}
		configList.addAll(toAdd);
	}
	
	private List<JMXConfig> expandWildcardedAccessorsAndEventNames(JMXConfig config)
		throws DataSourceException
	{
		List<JMXConfig> newConfigs = new ArrayList<JMXConfig>();
	
		String jmxClientCacheKey = null;
		JMXClient jmxClient = null;
		
		try {
			Pair<String,JMXClient> cachePair = jmxClientCache.acquireClient(config.getHost(), config.getPort(), connectionTimeout);
			
			jmxClientCacheKey = cachePair.getFirst();
			jmxClient = cachePair.getSecond();
			
			pushCachedClientKey(jmxClientCacheKey);
			
			
			List<JMXClient.MBeanDescriptor> beanDescriptors = jmxClient.getMBeans(config.getObjectName());
			
			if (beanDescriptors == null || beanDescriptors.size() == 0) {
				return newConfigs;
			}
			
			for (JMXClient.MBeanDescriptor bdesc : beanDescriptors) {
				String accessor = bdesc.getObjectName().toString();
				String newEventName = expandAccessorName(config, accessor);
				
				if(newEventName == null || newEventName.equals(""))
					continue;

                // escape illegal symbol chars
				newEventName = newEventName.replaceAll("[-+/=]", "_");
                
                // compress white space
                newEventName = newEventName.replaceAll("[\\s]", "");

				if(log.isDebugEnabled() && !newEventName.equals(config.getEventType()))
					log.debug("Found dynamic bean: '%s' --> '%s'", accessor, newEventName);
				
				newConfigs.add(new JMXConfig(config,newEventName,accessor));
			}
		}
		catch (RuntimeException e) {
			// jmxClient.getMBeans can throw a RuntimeException
            // note: this can be caused by a java.rmi.ServerError, caused by a java.lang.OutOfMemoryError on the remote system
            // so we don't want to log this with WARN status, because it will end up getting reported in splunk as if this
            // monitoring agent were OutOfMemory, and not the remote thing it's monitoring.
            // TODO: add logic to handle this OutOfMemory condition on the client, and have it be something we can alert on
            log.info("Unable to intialize JMX Client or expand dynamic accessor '%s:%s': %s", config.getHost(),config.getObjectName(), e.getMessage());
			log.info(e,"RuntimeException:");
			
			// release client with invalidate flag here
			if(jmxClientCacheKey != null) {
				popCachedClientKey();
				jmxClientCache.releaseClient(jmxClientCacheKey,true);
			}
			
			throw new DataSourceException("RuntimeException:",e);
		}
		catch (ConfigException e) {
			log.warn("Problem creating new configs for dynamic accessor '%s' : %s", config.getObjectName(), e.getMessage());
			log.info(e,"ConfigException:");
		}

		return newConfigs;
	}
	
	private List<JMXConfig> expandWildcardedAttributesAndKeyNames(JMXConfig config)
		throws DataSourceException
	{
		List<JMXConfig> newConfigs = new ArrayList<JMXConfig>();
		
		String jmxClientCacheKey = null;
		JMXClient jmxClient = null;
		
		try {
			Pair<String,JMXClient> cachePair = jmxClientCache.acquireClient(config.getHost(), config.getPort(), connectionTimeout);
			
			jmxClientCacheKey = cachePair.getFirst();
			jmxClient = cachePair.getSecond();
			
			pushCachedClientKey(jmxClientCacheKey);
			
			JMXClient.MBeanDescriptor mbeanDescriptor = getMBeanDescriptor(jmxClient, config.getObjectName());
			
			if(mbeanDescriptor == null)
				return newConfigs;
			
			MonitoredMBean monitoredMBean = new MonitoredMBean(config.getObjectName(), mbeanDescriptor, 1);
		
			String attr = config.getAttribute();

			// 2. attributes
			String attrRE = attr.replace(wildCardDelim, "(.+)");
			Pattern pattern = Pattern.compile(attrRE);
			for (String foundAttr : monitoredMBean.getRelevantAttributes()) {
				Matcher matcher = pattern.matcher(foundAttr);
				if (matcher.matches()) {
					String expAttr = matcher.group(0);
					String expEventAttrType = config.getEventAttributeType();   // wildcarded original (not expanded at first)
					if (expEventAttrType.equals(wildCardDelim)) {   // if *, use whole name, not just the matched part
						expEventAttrType = expAttr;
					}
					else {  // do matched replacement: *TotalMillies --> GetsTotalMillies (see, use just matched part)
						for (int i = 1; i <= matcher.groupCount(); i++) {
							expEventAttrType = expEventAttrType.replaceFirst("\\" + wildCardDelim, matcher.group(i));
						}
					}
					
					if(log.isDebugEnabled() && 
							(!attr.equals(expAttr) || !config.getEventAttributeType().equals(expEventAttrType)))
						log.debug("Dyno %s --> %s ; %s --> %s", attr, expAttr, config.getEventAttributeType(), expEventAttrType);
					
					newConfigs.add(new JMXConfig(config,config.getEventType(),expEventAttrType,config.getObjectName(),expAttr));
				}
			}
		}
		catch (RuntimeException e) {
			// jmxClient.getMBeans can throw a RuntimeException
            // jmxClient.getMBeans can throw a RuntimeException
            // note: this can be caused by a java.rmi.ServerError, caused by a java.lang.OutOfMemoryError on the remote system
            // so we don't want to log this with WARN status, because it will end up getting reported in splunk as if this
            // monitoring agent were OutOfMemory, and not the remote thing it's monitoring.
            // TODO: add logic to handle this OutOfMemory condition on the client, and have it be something we can alert on
			log.info("Unable to intialize JMX Client or expand dynamic accessor '%s:%s': %s", config.getHost(),config.getObjectName(), e.getMessage());
			log.info(e,"RuntimeException:");
			
			// release client with invalidate flag here
			if(jmxClientCacheKey != null) {
				popCachedClientKey();
				jmxClientCache.releaseClient(jmxClientCacheKey,true);
			}
			
			throw new DataSourceException("RuntimeException:",e);
		}
		catch (ConfigException e) {
			log.warn("Problem creating new configs for dynamic accessor '%s' : %s", config.getObjectName(), e.getMessage());
			log.info(e,"ConfigException:");
		}

		return newConfigs;	
	}
	
	private static String expandAccessorName(JMXConfig config, String expObjectName)
	{
		try {
			String wildCardedObjectName = config.getObjectName();

            // add matching for any wild-cards
			String objectNameRE = wildCardedObjectName.replace(wildCardDelim, "(.+)");

            //  escape pesky '$' (might need to make this more generic for all Regex meaningful symbols)
            objectNameRE = objectNameRE.replace("$","\\$");
            
			Pattern pattern = Pattern.compile(objectNameRE);
			Matcher matcher = pattern.matcher(expObjectName);
			if (!matcher.matches()) {
				log.warn("ObjectName mismatch, for config objectName '%s' and found objectName '%s' for host %s", objectNameRE, expObjectName, config.getHost());
				return "";  // the mismatch will be caught later by checkWildcardNaming() in Config
			}
			String expEventType = config.getEventType();   // wildcarded original (not expanded at first)

            // do matched replacement: *TotalMillies --> GetsTotalMillies (see, use just matched part)
            for (int i = 1; i <= matcher.groupCount(); i++) {
                expEventType = expEventType.replaceFirst("\\" + wildCardDelim, matcher.group(i));
            }

			if(log.isDebugEnabled() && 
					(!wildCardedObjectName.equals(expObjectName) || !config.getEventType().equals(expEventType)))
				log.debug("DynoAcc %s --> %s ; %s --> %s", wildCardedObjectName, expObjectName, config.getEventType(), expEventType);
			
			return expEventType;
		}
		catch(Exception ex) {
			// if some illegal character, etc., bail on trying to expand it for now
			return config.getEventType();
		}
	}
	
	private static JMXClient.MBeanDescriptor getMBeanDescriptor(JMXClient jmxClient, String objectName)
	{
		JMXClient.MBeanDescriptor mbeanDescriptor;
		mbeanDescriptor = jmxClient.getMBean(objectName);
		
		return mbeanDescriptor;
	}
}
