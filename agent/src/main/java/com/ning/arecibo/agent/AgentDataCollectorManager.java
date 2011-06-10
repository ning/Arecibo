package com.ning.arecibo.agent;

import com.google.inject.Inject;
import com.ning.arecibo.agent.config.AgentConfig;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.config.ConfigInitializer;
import com.ning.arecibo.agent.config.ConfigIterator;
import com.ning.arecibo.agent.config.ConfigIteratorFactory;
import com.ning.arecibo.agent.config.exclusion.ExclusionConfig;
import com.ning.arecibo.agent.datasource.DataSource;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.agent.datasource.DataSourceType;
import com.ning.arecibo.agent.datasource.DataSourceUtils;
import com.ning.arecibo.agent.status.Status;
import com.ning.arecibo.agent.status.StatusSummary;
import com.ning.arecibo.agent.status.StatusType;
import com.ning.arecibo.eventlogger.EventPublisher;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.UUIDUtil;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import org.weakref.jmx.Managed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class AgentDataCollectorManager
{
	private static final Logger log = Logger.getLogger(AgentDataCollectorManager.class);

	private final static AtomicLong activeConfigs = new AtomicLong();
	private final static AtomicInteger activeConfigHosts = new AtomicInteger();
	private final static AtomicLong activeCollectors = new AtomicLong();
	private final static AtomicLong activeSnmpCollectors = new AtomicLong();
    private final static AtomicLong excludedConfigs = new AtomicLong();
    private final static AtomicLong configsExceedingMax = new AtomicLong();
	private final static AtomicLong configsRemovedCounter = new AtomicLong();
	private final static AtomicLong configsAddedCounter = new AtomicLong();
    private final static AtomicLong failedSemaphoreAcquisitions = new AtomicLong();

	// these are collected from individual collectors
	private final static AtomicLong pollingFailureCounter = new AtomicLong();
	private final static AtomicLong pollingSuccessCounter = new AtomicLong();
	private final static AtomicLong collectorInitFailureCounter = new AtomicLong();
	private final static AtomicLong collectorInitSuccessCounter = new AtomicLong();
	private final static AtomicLong collectorAbortedCounter = new AtomicLong();

	private final Random random = new Random(System.currentTimeMillis());

	private final int numThreads;
	private final int maxActiveConfigs;
	private final int configUpdateInterval;
	private final int configUpdateInitialDelayRange;
	private final int maxPollingRetryDelay;
    private final int perHostSemaphoreConcurrency;
    private final long perHostSemaphoreMaxWaitMillis;
    private final long perHostSemaphoreRetryMillis;
    private final String publishedHostSuffix;
    private final String publishedPathSuffix;
	private final EventPublisher eventPublisher;
	private final ConfigInitializer initializer;
	private final DataSourceUtils dataSourceUtils;
	
	private ScheduledThreadPoolExecutor executor;
	private final Map<String, AgentDataCollector> collectorMap;
	private final Map<String, Config> configMap;
    private final Map<String, Semaphore> perHostSemaphoreMap;


    @Inject
	public AgentDataCollectorManager(AgentConfig config,
                                     EventPublisher eventPublisher,
                                     ConfigInitializer initializer,
                                     DataSourceUtils dataSourceUtils)
	{
		this.numThreads = config.getMonitorThreadPoolSize();
		this.maxActiveConfigs = config.getMonitorMaxActiveConfigs();
		this.configUpdateInterval = config.getMonitorConfigUpdateInterval();
		this.configUpdateInitialDelayRange = config.getMonitorConfigUpdateInitialDelayRange();
		this.maxPollingRetryDelay = config.getMonitorMaxPollingRetryDelay();
        this.perHostSemaphoreConcurrency = config.getMonitorPerHostSemaphoreConcurrency();
        this.perHostSemaphoreMaxWaitMillis = config.getMonitorPerHostSemaphoreMaxWaitMillis();
        this.perHostSemaphoreRetryMillis = config.getMonitorPerHostSemaphoreRetryMillis();
        this.publishedHostSuffix = config.getMonitorPublishedHostSuffix();
        this.publishedPathSuffix = config.getMonitorPublishedPathSuffix();

		this.eventPublisher = eventPublisher;
		this.initializer = initializer;
		this.dataSourceUtils = dataSourceUtils;

		this.collectorMap = new ConcurrentHashMap<String, AgentDataCollector>();
		this.configMap = new ConcurrentHashMap<String, Config>();
        this.perHostSemaphoreMap = new ConcurrentHashMap<String, Semaphore>();
	}

    private List<Config> getExclusionList() throws ConfigException
    {
        List<Config> exclusionList = initializer.getExclusionList();

        return exclusionList;
    }

	private List<Config> getConfigurationList() throws ConfigException
	{
		List<Config> configList = initializer.getCurrentConfigList();

		return configList;
	}

	private AgentDataCollector addOrUpdateCollector(Config config)
	{
		String collectorKey = config.getCollectorHashKey();
		AgentDataCollector collector = this.collectorMap.get(collectorKey);

		if (collector != null) {

			if(collector.add(config)) {
			    String configKey = config.getConfigHashKey();
			    this.configMap.put(configKey, config);

			    activeConfigs.incrementAndGet();
			    configsAddedCounter.incrementAndGet();

			    log.info("Adding config to existing collector: " + config.getConfigDescriptor());
				return null;
			}
			else {
				return null;
			}
		}
		else {
			
			// create a new datasource
			DataSource dataSource;
			try {
				dataSource = getDataSource(config);
			}
			catch (DataSourceException dsEx) {
				// shouldn't happen
				log.warn(dsEx);
				return null;
			}
			
			// create a new collector
			final UUID uuidForHost = getUuidForHost(config.getHost());  // uuids are unique per host
	
			collector = new AgentDataCollector(dataSource, config, this.eventPublisher, uuidForHost, config.getHost(),this);
			this.collectorMap.put(collectorKey, collector);   // we created a new one, so add it into the map
	
			String configKey = config.getConfigHashKey();
			this.configMap.put(configKey, config);
			
			if(dataSource.getDataSourceType().equals(DataSourceType.SNMP)) {
				activeSnmpCollectors.incrementAndGet();
			}
	
			activeCollectors.incrementAndGet();
			activeConfigs.incrementAndGet();
			configsAddedCounter.incrementAndGet();
	
			log.info("Adding new collector for config: " + config.getConfigDescriptor());

            // create a new perHost semaphore, if needed
            Semaphore sem = perHostSemaphoreMap.get(config.getHost());
            if(sem == null) {
                sem = new Semaphore(perHostSemaphoreConcurrency,true);
                perHostSemaphoreMap.put(config.getHost(),sem);
                log.info("Adding new polling semaphore for host %s, with concurrency %d",config.getHost(),perHostSemaphoreConcurrency);
            }

			return collector;
		}
	}

	private void removeOrUpdateCollector(Config config) {

		String collectorKey = config.getCollectorHashKey();	 // collectors are unique by host & accessor
		AgentDataCollector collector = this.collectorMap.get(collectorKey);

		if(collector == null) {
            // should not happen
            log.info("Could not find collector from which to remove config: " + config.getConfigDescriptor());
		    return;
		}

		if(collector.remove(config) == 0) {
		    log.info("Removing collector for config: " + config.getConfigDescriptor());

		    this.collectorMap.remove(collectorKey);

		    // notify the collector to exit on its own
			collector.setAbort(true);

			if(collector.getDataSourceType().equals(DataSourceType.SNMP)) {
				activeSnmpCollectors.decrementAndGet();
			}
	
			activeCollectors.decrementAndGet();
		}
		else {
		    log.info("Removing config from existing collector: " + config.getConfigDescriptor());
		}

		this.configMap.remove(config.getConfigHashKey());
		
		activeConfigs.decrementAndGet();
		configsRemovedCounter.incrementAndGet();
	}

    public boolean tryAcquirePerHostSemaphore(String host) {
        Semaphore sem = perHostSemaphoreMap.get(host);
        try {
            return sem.tryAcquire(perHostSemaphoreMaxWaitMillis,TimeUnit.MILLISECONDS);
        } catch (InterruptedException ieEx){
            // shouldn't happen
            return false;
        }
    }

    public void releasePerHostSemaphore(String host) {
        Semaphore sem = perHostSemaphoreMap.get(host);
        sem.release();
    }

    public void rescheduleCollectorAfterSemaphoreRetryDelay(AgentDataCollector collector) {
        executor.schedule(collector,perHostSemaphoreRetryMillis,TimeUnit.MILLISECONDS);
    }

	public UUID getUuidForHost(String host)
	{
		return UUIDUtil.md5UUID(host);
	}

	private DataSource getDataSource(Config config) throws DataSourceException
	{
		return dataSourceUtils.getDataSource(config);
	}

	public synchronized void start()
	{
		this.executor = new ScheduledThreadPoolExecutor(this.numThreads);

		// start the config updater
		this.executor.scheduleWithFixedDelay(new _CollectorConfigUpdater(),random.nextInt(configUpdateInitialDelayRange),configUpdateInterval,TimeUnit.SECONDS);
	}

	public synchronized void stop()
	{
		if (this.executor != null) {
			this.executor.shutdown();
			this.executor = null;
		}
	}

	public List<Status> getStatus()
	{
		List<Status> lastStatus = new ArrayList<Status>();
		for (AgentDataCollector collector : collectorMap.values()) {
			Collection<Status> statuses = collector.getStatusList();
			if (statuses != null) {
				lastStatus.addAll(statuses);
			}
		}
		return lastStatus;
	}

	private void increment(Map<String, Integer> map, String key)
	{
		Integer val = map.get(key);
		if (val == null) {
			map.put(key, 1);
			return;
		}
		map.put(key, val + 1);
	}

	public List<StatusSummary> getStatusSummary()
	{
		List<Status> lastStatae = getStatus();
		// accumulate by zone
		Map<String, Set<String>> accessors = new HashMap<String, Set<String>>();
		Map<String, Integer> queryCounts = new HashMap<String, Integer>();
		Map<String, Integer> successCounts = new HashMap<String, Integer>();
		for (Status status : lastStatae) {
			String zone = status.getHost();
			if (status.getLastStatus() == StatusType.READ_SUCCESS) {
				increment(successCounts, zone);
			}
			increment(queryCounts, zone);
			if (accessors.get(zone) == null) {
				accessors.put(zone, new HashSet<String>());
			}
			accessors.get(zone).add(status.getEventName());
		}

		long summaryTime = System.currentTimeMillis();
		List<StatusSummary> statusSummary = new ArrayList<StatusSummary>();
		for (String zone : accessors.keySet()) {
			final int acc = accessors.get(zone).size();
			Integer numSuccesses = successCounts.get(zone);
			final int successes = (numSuccesses != null) ? numSuccesses : 0;	// null if no successes
			final int queries = queryCounts.get(zone);
			statusSummary.add(new StatusSummary(summaryTime, zone, acc, successes, queries));
		}

		return statusSummary;
	}

    public String getPublishedHostSuffix() {
        return publishedHostSuffix;
    }

    public String getPublishedPathSuffix() {
        return publishedPathSuffix;
    }

    private boolean testConfigForExclusion(Config config,List<Config> exclusionList) {
        if(exclusionList == null)
            return false;

        for(Config exclusion:exclusionList) {
            ExclusionConfig exConfig = (ExclusionConfig)exclusion;
            if(exConfig.testForExclusion(config))
                return true;
        }

        return false;
    }

    private void expandConfigs(List<Config> configList,List<Config> exclusionList)
	{
		try {
			dataSourceUtils.startConfigExpansion();
		}
		catch(DataSourceException dsEx) {
			log.warn(dsEx,"Could not start config expansion:");
			return;
		}

        try {
            List<Config> toAdd = new ArrayList<Config>();
            List<Config> toRemove = new ArrayList<Config>();
            int totalNewCount = 0;
            long totalExcludedCount = 0;
            long totalExceedingMaxCount = 0;

            for(Config config:configList) {

                if(totalNewCount >= this.maxActiveConfigs) {
                    totalExceedingMaxCount++;
                    toRemove.add(config);
                    continue;
                }

                if(testConfigForExclusion(config,exclusionList)) {
                    log.debug("Excluding baseConfig for %s",config.getConfigDescriptor());
                    totalExcludedCount++;
                    toRemove.add(config);
                    continue;
                }

                ConfigIteratorFactory ciFactory;
                ConfigIterator configIterator;

                try {
                    ciFactory = dataSourceUtils.getConfigIteratorFactory(config);
                    configIterator = ciFactory.getConfigIterator(config);
                }
                catch(DataSourceException dsEx) {
                    configIterator = null;
                    toRemove.add(config);
                    log.warn("Got exception getting config iterator. Removing baseConfig for %s: %s",config.getConfigDescriptor(),dsEx);
                    log.info(dsEx,"DataSourceException:");
                }

                if(configIterator != null) {

                    int newCount = 0;

                    try {
                        Config newConfig;
                        while((newConfig = configIterator.getNextConfig()) != null) {
                            toAdd.add(newConfig);
                            newCount++;
                            totalNewCount++;

                            // see if this config is new, log if so
                            if(configMap.get(newConfig.getConfigHashKey()) == null) {
                                if(newConfig.getConfigHashKey().equals(config.getConfigHashKey())) {
                                    log.info("Validated config '%s'",newConfig.getConfigHashKey());
                                }
                                else {
                                    log.info("Expanded config '%s' from baseConfig '%s'",newConfig.getConfigHashKey(),config.getConfigHashKey());
                                }
                            }

                            // bail if we've reached our max
                            if(totalNewCount >= this.maxActiveConfigs) {
                                log.info("maxActiveConfigs reached (" + this.maxActiveConfigs + "), skipping further config expansion");
                                break;
                            }
                        }

                        toRemove.add(config);

                        if(log.isDebugEnabled()) {
                            log.debug("Expanded/Validated %d config(s) from baseConfig '%s'.", newCount, config.getConfigDescriptor());
                        }
                    }
                    catch(DataSourceException dsEx) {
                        toRemove.add(config);
                        log.warn("Got exception getting next config from iterator. Removing baseConfig for %s: %s",config.getConfigDescriptor(),dsEx);
                        log.info(dsEx,"DataSourceException:");
                    }
                }
            }

            for (Config config : toRemove) {
                configList.remove(config);
            }
            configList.addAll(toAdd);

            excludedConfigs.set(totalExcludedCount);
            configsExceedingMax.set(totalExceedingMaxCount);
        }
        finally {
            try {
                // try to make sure this cleanup happens, even if we got a RuntimeException above
                dataSourceUtils.finishConfigExpansion();
            }
            catch(DataSourceException dsEx) {
                log.warn(dsEx,"Could not finish config expansion:");
            }
        }
	}

    public void rescheduleCollector(AgentDataCollector collector,long delayMillis) {
        executor.schedule(collector,delayMillis,TimeUnit.MILLISECONDS);
    }

    private class _CollectorConfigUpdater implements Runnable {

	    public void run() {

	        try {
	            log.info("Running config updater");

                List<Config> exclusionList = null;
	        	List<Config> newConfigList = null;
	        	try {
                    exclusionList = getExclusionList();
	        	    newConfigList = getConfigurationList();
	        	}
	            catch(ConfigException configEx) {
	                log.warn("Got ConfigException in CollectorConfigUpdater: %s",configEx);
	                log.info(configEx,"ConfigException:");
	                
	                // exit without altering current config, e.g. if problem with galaxy call, don't want to invalidate anything
	                return;
	            }

                // Expand table configs
                if(newConfigList != null) {
	                log.info("Examining configs, initial config list length = %d", newConfigList.size());
		        	expandConfigs(newConfigList,exclusionList);
		        	log.info("After validation/expansion, config list length = %d", newConfigList.size());
                }

                // hash all the new config keys
	        	// (and count number of unique hosts)
	        	HashSet<String> newConfigKeys = new HashSet<String>();
	        	HashSet<String> newConfigHosts = new HashSet<String>();
	        	if(newConfigList != null) {
	        	    for(Config newConfig:newConfigList) {
	        	        String configKey = newConfig.getConfigHashKey();
	        	        newConfigKeys.add(configKey);

	        	        String configHost = newConfig.getHost();
	        	        // this will only update if it's a new one
	        	        newConfigHosts.add(configHost);
	        	    }
	            }
	        	activeConfigHosts.set(newConfigHosts.size());

	        	// remove any currently scheduled configs that are no longer needed
	        	Set<String> currConfigKeys = configMap.keySet();
	        	Iterator<String> iter = currConfigKeys.iterator();
	        	while(iter.hasNext()) {
	        	    String currConfigKey = iter.next();
	        	    if(!newConfigKeys.contains(currConfigKey)) {
	        	        removeOrUpdateCollector(configMap.get(currConfigKey));
	        	        iter.remove();
	        	    }
	        	}

	        	// add or update collectors as needed
	        	Map<String,AgentDataCollector> newCollectors = new HashMap<String,AgentDataCollector>();
	        	if (newConfigList != null) {
					// make sure we add all the configs to existing collectors, before starting any new collectors
					for(Config config:newConfigList) {
				    	AgentDataCollector newCollector = addOrUpdateCollector(config);
						if(newCollector != null) {
						    newCollectors.put(config.getCollectorHashKey(),newCollector);
						}
					}

					Set<String> newCollectorKeys = newCollectors.keySet();
					for(String newCollectorKey:newCollectorKeys) {
					    AgentDataCollector newCollector = newCollectors.get(newCollectorKey);
					    log.info("Scheduling new collector: '%s'", newCollectorKey);
					    int intervalInSec = newCollector.getScheduledPollingIntervalSeconds();
						executor.schedule(newCollector, random.nextInt(intervalInSec), TimeUnit.SECONDS);
					}
				}
	    	}

	        catch(RuntimeException ruEx) {
	        	log.warn(ruEx,"Got RuntimeException in CollectorConfigUpdater:");
	        }
	    }
	}

	public int getMaxPollingRetryDelay() {
		return maxPollingRetryDelay;
	}

	@MonitorableManaged(monitored = true)
	public long getActiveCollectors() {
	    return activeCollectors.get();
	}

	@MonitorableManaged(monitored = true)
	public long getActiveSnmpCollectors() {
	    return activeSnmpCollectors.get();
	}

	@MonitorableManaged(monitored = true)
	public long getActiveConfigs() {
	    return activeConfigs.get();
	}

	@MonitorableManaged(monitored = true)
    public int getActiveConfigHosts() {
        return activeConfigHosts.get();
    }

	@MonitorableManaged(monitored = true)
	public long getExcludedConfigs() {
	    return excludedConfigs.get();
	}

	@MonitorableManaged(monitored = true)
    public long getConfigsExceedingMax() {
        return configsExceedingMax.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
	public long getConfigsAdded() {
	    return configsAddedCounter.get();
	}

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
	public long getConfigsRemoved() {
	    return configsRemovedCounter.get();
	}

	// these are updated by individual collectors
	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
	public long getPollingFailures() {
		return pollingFailureCounter.get();
	}
    
    public void incrementPollingFailures() {
    	pollingFailureCounter.incrementAndGet();
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
	public long getPollingSuccesses() {
		return pollingSuccessCounter.get();
	}

    public void incrementPollingSuccesses() {
    	pollingSuccessCounter.incrementAndGet();
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
	public long getCollectorInitFailures() {
		return collectorInitFailureCounter.get();
	}

    public void incrementCollectorInitFailures() {
    	collectorInitFailureCounter.incrementAndGet();
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
	public long getCollectorInitSuccesses() {
		return collectorInitSuccessCounter.get();
	}

    public void incrementCollectorInitSuccesses() {
    	collectorInitSuccessCounter.incrementAndGet();
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
	public long getCollectorsAborted() {
		return collectorAbortedCounter.get();
	} 
    
    public void incrementCollectorsAborted() {
    	collectorAbortedCounter.incrementAndGet();
    }

    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getFailedPollingSemaphoreAcquisitions() {
        return failedSemaphoreAcquisitions.get();
    }

    public void incrementFailedSemaphoreAcquisitions() {
        failedSemaphoreAcquisitions.incrementAndGet();
    }

    @MonitorableManaged(monitored = true)
    public long getAvgPollingDurationNanos() {

        long avg = 0L;
        int count = 0;
        for(AgentDataCollector collector:collectorMap.values()) {
            avg += collector.getLastPollingDurationNanos();
            count++;
        }

        if(count > 0)
            avg /= (long)count;

        return avg;
    }

    @MonitorableManaged(monitored = true)
    public double getAvgPollingIntervalNanos() {

        long avg = 0L;
        int count = 0;
        for(AgentDataCollector collector:collectorMap.values()) {
            avg += collector.getLastPollingIntervalNanos();
            count++;
        }

        if(count > 0)
            avg /= (long)count;

        return avg;
    }

    @Managed
    public Map<String,Long> getLastPollingDurationNanosPerCollector() {

        Map<String,Long> map = new HashMap<String,Long>();

        for(AgentDataCollector collector:collectorMap.values()) {
            String key = collector.getHostName() + "->" + collector.getEventType();
            Long value = collector.getLastPollingDurationNanos();
            map.put(key,value);
        }

        return map;
    }

    @Managed
    public Map<String,Long> getLastPollingIntervalNanosPerCollector() {

        Map<String,Long> map = new HashMap<String,Long>();

        for(AgentDataCollector collector:collectorMap.values()) {
            String key = collector.getHostName() + "->" + collector.getEventType();
            Long value = collector.getLastPollingIntervalNanos();
            map.put(key,value);
        }

        return map;
    }
}
