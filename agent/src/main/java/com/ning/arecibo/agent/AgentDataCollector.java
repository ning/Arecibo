package com.ning.arecibo.agent;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.datasource.DataSource;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.agent.datasource.DataSourceType;
import com.ning.arecibo.agent.status.Status;
import com.ning.arecibo.agent.status.StatusType;
import com.ning.arecibo.agent.transform.CounterRateTransform;
import com.ning.arecibo.agent.transform.RateTransform;
import com.ning.arecibo.agent.transform.Transform;
import com.ning.arecibo.event.MonitoringEvent;
import com.ning.arecibo.eventlogger.EventPublisher;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.Pair;

public final class AgentDataCollector implements Runnable
{
	private static final Logger log = Logger.getLogger(AgentDataCollector.class);

    public static final String GROUP_DELIMITER_START = "{";

	public static final int COLLECTOR_INIT_RESET = 0;
	public static final int COLLECTOR_INIT_STARTED = 1;
	public static final int COLLECTOR_INIT_COMPLETED = 2;

	private final EventPublisher eventPublisher;
	private final String hostName;

	private final String deployedEnv;
	private final String deployedVersion;
	private final String deployedType;
	private final String deployedConfigSubPath;
	
	private final String collectorHashKey;

	private final UUID uuid;                                    // unique id for the events generated for this collector
	
	private final AgentDataCollectorManager collectorManager;

	private final HashMap<String, Config> configsByHashKey;
	private final Map<String, Status> status;                   
	private final DataSource dataSource;

	private volatile Map<String, _TransformableAttribute> expandedAttributes;      // attribute --> class

	private final AtomicInteger collectorInitialization;
	private final int scheduledPollingIntervalSeconds;
    private final long scheduledPollingIntervalMillis;
	private final String eventType;                                   // defined when live

	private volatile boolean abort = false;
	
	private volatile int cyclesToSkip = 0;
	private volatile int currentCycleSkipCount = 0;
    private volatile long referenceExecutionStartTime = 0L;
    private volatile long lastPollingEndTimeNanos = 0L;
    private volatile long lastPollingDurationNanos = 0L;
    private volatile long lastPollingIntervalNanos = 0L;

    public AgentDataCollector(DataSource dataSource,
								Config config, 
								EventPublisher eventPublisher, 
								UUID uuid, 
								String hostName,
								AgentDataCollectorManager collectorManager)
	{
		this.eventPublisher = eventPublisher;
		if (dataSource == null || config == null || eventPublisher == null) {
			throw new IllegalArgumentException();
		}

		this.dataSource = dataSource;
		this.uuid = uuid;
		this.hostName = hostName;
		this.collectorManager = collectorManager;

		this.eventType = config.getEventType();
		this.deployedEnv = config.getDeployedEnv();
		this.deployedVersion = config.getDeployedVersion();
		this.deployedType = config.getDeployedType();
		this.deployedConfigSubPath = config.getDeployedConfigSubPath();
		this.scheduledPollingIntervalSeconds = config.getPollingIntervalSeconds();

        this.scheduledPollingIntervalMillis = 1000L * (long)this.scheduledPollingIntervalSeconds;

		this.configsByHashKey = new HashMap<String, Config>();
		this.configsByHashKey.put(config.getConfigHashKey(), config);                 // may be wildcarded
		this.status = new HashMap<String, Status>();
		
		this.collectorHashKey = config.getCollectorHashKey();

		this.collectorInitialization = new AtomicInteger(COLLECTOR_INIT_RESET);
	}

    public String getHostName() {
        return this.hostName;
    }

    public String getEventType() {
        return this.eventType;
    }
	
	public synchronized boolean add(Config config)
	{
		if (this.configsByHashKey.containsKey(config.getConfigHashKey())) {
			return false;
		}
		this.configsByHashKey.put(config.getConfigHashKey(), config);                 // may be wildcarded
		
		// clear the downstream components such as transformers, etc
		this.collectorInitialization.set(COLLECTOR_INIT_RESET);
				
		return true;
	}
	
	public synchronized boolean configOutOfDate(Config config)
	{
		Config currConfig = this.configsByHashKey.get(config.getConfigHashKey());
		if(currConfig != null) {
			if(!currConfig.equalsConfig(config)) {
				return true;
			}
		}
		
		return false;
	}

	public synchronized int remove(Config config)
	{
		if (!this.configsByHashKey.containsKey(config.getConfigHashKey())) {
			return configsByHashKey.size();
		}

		this.configsByHashKey.remove(config.getConfigHashKey());

		if (this.configsByHashKey.size() == 0) {
			try {
				this.dataSource.closeResources();
			}
			catch(DataSourceException dsEx) {
				log.warn("Got exception closing dataSource client");
				log.info(dsEx);
			}
		}

		// clear the downstream components such as transformers, etc
		this.collectorInitialization.set(COLLECTOR_INIT_RESET);

		return configsByHashKey.size();
	}

	public synchronized void setAbort(boolean abort)
	{
		this.abort = abort;
	}

    // TODO: After converting this to run as a self-rescheduling Runnable, rather than scheduled by an executor
    // at a fixed rate/delay, it's probably possible to simplify the handling for exponential backup (skip cycles) and
    // also for the abort mechanism
	public synchronized void run()
	{

        boolean okToRescheduleNormally = true;
        boolean semaphoreAcquired = false;
        boolean semaphoreReleased = false;
		try {

            // try to acquire semaphore
            if(!(semaphoreAcquired = collectorManager.tryAcquirePerHostSemaphore(this.hostName))) {
                // reset this if we fail, try to find an available slot in the cycle
                referenceExecutionStartTime = 0L;

                collectorManager.incrementFailedSemaphoreAcquisitions();
                return;
            }

			// see if an abort has been requested
			if (this.abort) {
				log.info("Aborting Collector for " + this.collectorHashKey);
				collectorManager.incrementCollectorsAborted();

				// cause this Runnable to be unscheduled from the ScheduledThreadPoolExecutor
				// by throwing a RuntimeException out of here
				throw new _AbortCollectorException("Aborting collector thread by request...");
			}
            else {
                if(referenceExecutionStartTime == 0L) {
                    referenceExecutionStartTime = System.currentTimeMillis();
                }
            }
			
			// see if we need to skip this cycle
			if(this.cyclesToSkip > 0) {
				//log.debug("Skipping Collector cycle for " + this.collectorHashKey + ", " + cyclesToSkip + " intervals remaining");
				this.cyclesToSkip--;
				return;
			}

			// Initialize the data source
			long updateTime = System.currentTimeMillis();
			initializeDatasourceIfNecessary(updateTime);

			if (this.collectorInitialization.get() != COLLECTOR_INIT_COMPLETED) {
				
				this.collectorInitialization.set(COLLECTOR_INIT_RESET);
				updateCycleSkipCount(true);

				log.info("Stopping collector execution due to failed initialization, for " + this.collectorHashKey);
				log.info("Setting collector for re-initialization, after skipping " + this.cyclesToSkip + " intervals, for " + this.collectorHashKey);
				
				return;
			}

			// Fetch the data from the data source
			Map<String, Object> eventValues = null;
			String errorStatusMsg = null;
			try {
				eventValues = getNewValues();

				if (eventValues != null && eventValues.size() > 0) {
					collectorManager.incrementPollingSuccesses();
				}
				else {
					errorStatusMsg = "Null data values returned";
					collectorManager.incrementPollingFailures();
				}
				updateCycleSkipCount(false);
			}
			catch (DataSourceException e) {
				
				// don't log this as warn, don't want to spam splunk
				//   can alert separately on polling failure counts, etc.
                if(e.getCause() != null)
				    log.info(e.getCause().getMessage());
                else
                    log.info(e);
				
				// force re-initialization next cycle
				updateCycleSkipCount(true);
				log.info("Setting collector for re-initialization, after skipping " + this.cyclesToSkip + " intervals, for " + this.collectorHashKey);
				
				errorStatusMsg = e.toString();
				this.collectorInitialization.set(COLLECTOR_INIT_RESET);
				collectorManager.incrementPollingFailures();
			}
            finally {
                // release this semaphore now, prior to time-consuming event publishing, etc.
                collectorManager.releasePerHostSemaphore(this.hostName);
                semaphoreReleased = true;
            }

			// Publish the data
			if (eventValues != null && eventValues.size() > 0) {

                Map<String,Object> publishValues = prepareValuesForPublishing(eventValues);

				publishEvent(new MonitoringEvent(updateTime,
                                                    this.eventType,
                                                    this.uuid,
                                                    this.hostName + collectorManager.getPublishedHostSuffix(),
                                                    this.deployedEnv,
                                                    this.deployedVersion,
                                                    this.deployedType,
                                                    this.deployedConfigSubPath + collectorManager.getPublishedPathSuffix(),
                                                    publishValues));
			}

			// Update the state of each expected attribute
			for (Map.Entry<String, Status> entry : this.status.entrySet()) {
				String eventAttributeType = entry.getKey();
				Status stat = entry.getValue();
				if (errorStatusMsg != null) {
					stat.setLastStatus(StatusType.READ_FAILURE, updateTime, errorStatusMsg, null);
				}
				else {
					if (eventValues != null && eventValues.get(eventAttributeType) != null) {
						stat.setLastStatus(StatusType.READ_SUCCESS, updateTime, null, eventValues.get(eventAttributeType));
					}
					else {
						stat.setLastStatus(StatusType.READ_NULL, updateTime, null, null);
					}
				}

			}
		}
		catch (_AbortCollectorException abEx) {
            okToRescheduleNormally = false;
			log.info("Aborting collector thread by request for: " + this.collectorHashKey);

			// this will cause the executor to suspend scheduling of this Collector
			throw abEx;
		}
		catch (RuntimeException ruEx) {
			// catch all other RuntimeExceptions just in case, so this Collector doesn't get suspended from being scheduled
			log.warn("Got RuntimeException running collector for: " + this.collectorHashKey);
			log.warn(ruEx);
		}
        finally {

            if(semaphoreAcquired && !semaphoreReleased) {
                // make sure we release here, if we return abnormally, and it wasn't released after the call to getValues above
                collectorManager.releasePerHostSemaphore(this.hostName);
            }

            if(okToRescheduleNormally) {
                if(semaphoreAcquired) {
                    long currentTime = System.currentTimeMillis();
                    long nextDelayMillis = scheduledPollingIntervalMillis - ((currentTime - referenceExecutionStartTime) % (scheduledPollingIntervalMillis));
                    collectorManager.rescheduleCollector(this,nextDelayMillis);
                }
                else {
                    collectorManager.rescheduleCollectorAfterSemaphoreRetryDelay(this);
                }
            }
        }
	}
	
	public DataSourceType getDataSourceType() {
		return this.dataSource.getDataSourceType();
	}
	
	private void updateCycleSkipCount(boolean okToSkip) {
		if(okToSkip) {
			
			int newSkipCount;
			if(currentCycleSkipCount == 0) {
				newSkipCount = 1;
			}
			else {
				// double the skip count
				newSkipCount = currentCycleSkipCount * 2;
			}
			
			int maxPollingRetryDelay = collectorManager.getMaxPollingRetryDelay();
			int newSkipSeconds = newSkipCount * this.scheduledPollingIntervalSeconds;
			if(newSkipSeconds > maxPollingRetryDelay) {
				newSkipSeconds = maxPollingRetryDelay;
			}
			
			currentCycleSkipCount = (int)Math.floor((double)newSkipSeconds/(double)this.scheduledPollingIntervalSeconds);
			cyclesToSkip = currentCycleSkipCount;
		}
		else {
			currentCycleSkipCount = 0;
			cyclesToSkip = 0;
		}
	}

	private Pair<StatusType, String> attemptDatasourceInitialization()
	{
		StatusType statusValue;
		String msg;
		try {

			this.dataSource.initialize();

			if (this.dataSource.isInitialized()) {
				statusValue = StatusType.INITIALIZATION_SUCCESS;
				this.collectorInitialization.set(COLLECTOR_INIT_COMPLETED);
				msg = "Initialization success";

				collectorManager.incrementCollectorInitSuccesses();
			}
			else {
				statusValue = StatusType.INITIALIZATION_FAILURE;
				this.collectorInitialization.set(COLLECTOR_INIT_RESET);
				collectorManager.incrementCollectorInitFailures();
				msg = "Initialization failure (no exception thrown)";
			}
		}
		catch (DataSourceException dsEx) {
			
			if(this.dataSource.isInitialized()) {
				try {
					this.dataSource.closeResources();
				}
				catch(Exception ex){}
			}
			
			statusValue = StatusType.INITIALIZATION_FAILURE;
			this.collectorInitialization.set(COLLECTOR_INIT_RESET);
			collectorManager.incrementCollectorInitFailures();
			msg = dsEx.toString();
		}
		return new Pair<StatusType, String>(statusValue, msg);
	}

	private void initializeDatasourceIfNecessary(long updateTime)
	{
		if (this.collectorInitialization.compareAndSet(COLLECTOR_INIT_RESET, COLLECTOR_INIT_STARTED)) {
			log.info("Initializing Data Source for " + this.collectorHashKey);

			// clear all status
			this.status.clear();

			// initialize client
			Pair<StatusType, String> statusPair = attemptDatasourceInitialization();

			StatusType statusVal = statusPair.getFirst();
			String msg = statusPair.getSecond();

			if (statusVal != StatusType.INITIALIZATION_SUCCESS) {
				setAllStatus(updateTime, StatusType.INITIALIZATION_FAILURE, "Initialization failure:  " + msg);
				return;
			}

			// continue by doing any config expansions
			Map<String, Config> workingConfigs;
            if(this.dataSource.canExpandConfigs()) {
                try {
                    workingConfigs = this.dataSource.expandConfigs((Map<String,Config>)this.configsByHashKey.clone());
                    if(workingConfigs == null)
                        msg = "Got empty expandedConfigs";
                }
                catch(DataSourceException dsEx) {
                    msg = dsEx.toString();
                    workingConfigs = null;
                }
            }
            else {
                workingConfigs = this.configsByHashKey;
            }

			if (workingConfigs == null) {
				setAllStatus(updateTime, StatusType.INITIALIZATION_FAILURE, msg);
				return;
			}

			this.expandedAttributes = new HashMap<String, _TransformableAttribute>();
			for (Config config : workingConfigs.values()) {
				addAttributeTransforms(config);
			}

			// Successful initialization. Now set up parsers, et al for the attributes
			try {
				prepareConfigsForMonitoring();
			}
			catch(DataSourceException dsEx) {
				msg = dsEx.toString();
				setAllStatus(updateTime, StatusType.INITIALIZATION_FAILURE, msg);
				return;
			}

			// set the status of all of the attributes managed by this collector to indicate client init success
			for (String attr : this.expandedAttributes.keySet()) {
				_TransformableAttribute transformableAttribute = this.expandedAttributes.get(attr);
				for (String eventAttributeType : transformableAttribute.getTransforms().keySet()) {
					Config config = transformableAttribute.getConfig();
					Status stat = new Status(config,eventAttributeType);
					stat.setLastStatus(statusVal, updateTime, msg, null);
					this.status.put(eventAttributeType, stat);
				}
			}
		}
	}

	private void setAllStatus(long updateTime, StatusType statusVal, String msg)
	{
		// failure, so report for all attributes. These are unexpanded (possibly wildcarded) attributes
		for (String descriptor : configsByHashKey.keySet()) {
			Config config = configsByHashKey.get(descriptor);
			Status stat = new Status(config,config.getEventAttributeType());
			stat.setLastStatus(statusVal, updateTime, msg, null);
			this.status.put(descriptor, stat);
		}
	}

	private void prepareConfigsForMonitoring()
		throws DataSourceException
	{
		// add parsers, etc
		for (Map.Entry<String, _TransformableAttribute> entry : expandedAttributes.entrySet()) {
			this.dataSource.prepareConfig(entry.getValue().getConfig());
		}

		this.dataSource.finalizePreparation();
	}

	private void addAttributeTransforms(Config config)
	{
		String eventAttributeType = config.getEventAttributeType();	// use eventAttributeType so there can be >1 transform per attribute
		String attr = config.getConfigHashKey();
		if (this.expandedAttributes.containsKey(attr)) {
			log.warn("Replacing transform for '%s' (eventAttributeType '%s').", eventAttributeType, config.getEventAttributeType());
		}

		// we assume all rate calculations are for Counters (may need to revisit later)
		if (config.isBothValueAndRate()) {
			// add non-rate
			_TransformableAttribute transformableAttr = new _TransformableAttribute(eventAttributeType, new Transform(), config);
			
			// add rate...
			if(config.isCounter())
				transformableAttr.addTransform(eventAttributeType + "Rate", new CounterRateTransform());
			else
				transformableAttr.addTransform(eventAttributeType + "Rate", new RateTransform());
			
			this.expandedAttributes.put(attr, transformableAttr);
			return;
		}

		if (config.isRate()) {
			
			_TransformableAttribute transformableAttr;
			if(config.isCounter())
				transformableAttr = new _TransformableAttribute(eventAttributeType, new CounterRateTransform(), config);
			else
				transformableAttr = new _TransformableAttribute(eventAttributeType, new RateTransform(), config);
			
			this.expandedAttributes.put(attr, transformableAttr);
			return;
		}

        // default to non-rate transform
		_TransformableAttribute transformableAttr = new _TransformableAttribute(eventAttributeType, new Transform(), config);
		this.expandedAttributes.put(attr, transformableAttr);

	}

	private void publishEvent(MonitoringEvent event)
	{
		try {
			this.eventPublisher.publish(event, EventPublisher.PublishMode.ASYNCHRONOUS);
		}
		catch (IOException e) {
			log.info("Problem publishing event", e);
		}
	}

    private Map<String, Object> prepareValuesForPublishing(Map<String, Object> eventValues) {
        // do any needed tweaks before sending values over the wire
        // want to keep the original around for reporting status, etc.

        HashMap<String,Object> publishValues = new HashMap<String,Object>();

        for(Map.Entry<String,Object> entry:eventValues.entrySet()) {
            
            if(entry.getValue() instanceof String) {
                // remove any group values (member values should be present as individual entries)
                if(((String)entry.getValue()).startsWith(GROUP_DELIMITER_START))
                    continue;
            }

            publishValues.put(entry.getKey(),entry.getValue());
        }

        return publishValues;
    }

	private Map<String, Object> getNewValues()  throws DataSourceException
	{
        long pollStartTimeNanos = System.nanoTime();

		Map<String, Object> newValues;
		
		// get values from dataSource
		newValues = this.dataSource.getValues();

		if (newValues == null) {
			return null;
		}

		Map<String, Object> eventValues = new HashMap<String, Object>();
		for (Map.Entry<String, Object> entry : newValues.entrySet()) {
			
			Object  value = entry.getValue();
			
			// don't allow nulls
			if(value == null) {
                continue;
			}
			
			// don't allow NaN's
			if(value instanceof Number) {
				if(Double.compare(((Number)value).doubleValue(),Double.NaN) == 0) {
                    continue;
				}
			}

            // apply transformations
			applyTransforms(eventValues, entry.getKey(), entry.getValue());
		}

        // update polling stats
        updatePollingStats(pollStartTimeNanos,eventValues);

		return eventValues;
	}



	private void applyTransforms(Map<String, Object> eventValues, final String configHashKey, final Object value)
	{
		//String attribute = configHashKey.replaceAll("\\(.*?\\)$", "");	 // strip off (xxx) added by parsers

		if(this.expandedAttributes != null) {
		    _TransformableAttribute transformableAttr = this.expandedAttributes.get(configHashKey);
			if (transformableAttr == null) {
				log.warn("No record found for attribute '%s' of eventType '%s'. Skipping.", configHashKey, this.eventType);
				return;
			}

    		Map<String, Transform> transforms = transformableAttr.getTransforms();
    		if (transforms == null) {
    			log.warn("No transformations found for attribute '%s' of eventType '%s'. Skipping.", configHashKey, this.eventType);
    			return;
    		}

    		for (Map.Entry<String, Transform> transformEntry : transforms.entrySet()) {

                if(value instanceof Map) {
                    // apply transform to each entry in the map
                    Map<String,Object> valueMap = (Map<String,Object>)value;
                    String valueDescription = "{";
                    for(Map.Entry<String,Object> valueEntry : valueMap.entrySet()) {
                        Object processedValue = transformEntry.getValue().process(valueEntry.getValue());
                        if(processedValue != null) {
                            eventValues.put(transformEntry.getKey() + "_" + valueEntry.getKey(), processedValue);
                            valueDescription += valueEntry.getKey() + "=" + processedValue;
                        }
                    }
                    valueDescription += "}";
                    eventValues.put(transformEntry.getKey(),valueDescription);
                }
                else {
                    Object processedValue = transformEntry.getValue().process(value);
                    if(processedValue != null) {
    			        eventValues.put(transformEntry.getKey(), processedValue);
                    }
                }
    		}
		}
	}
	
	public Collection<Status> getStatusList()
	{
		return this.status.values();
	}

    private void updatePollingStats(long pollStartTimeNanos, Map<String, Object> eventValues) {

        //long pollEndTime = System.currentTimeMillis();
        long pollEndTimeNanos = System.nanoTime();
        if (lastPollingEndTimeNanos > 0L) {
            lastPollingIntervalNanos = pollEndTimeNanos - lastPollingEndTimeNanos;
            eventValues.put("pollingInterval", lastPollingIntervalNanos);
        }

        lastPollingEndTimeNanos = pollEndTimeNanos;
        lastPollingDurationNanos = lastPollingEndTimeNanos - pollStartTimeNanos;
    }

    public long getLastPollingDurationNanos()
    {
        return this.lastPollingDurationNanos;
    }

    public long getLastPollingIntervalNanos()
    {
        return this.lastPollingIntervalNanos;
    }

	public int getScheduledPollingIntervalSeconds()
	{
		return this.scheduledPollingIntervalSeconds;
	}


    class _TransformableAttribute
	{
		private final Map<String, Transform> transforms;
		private Config config;

		public _TransformableAttribute(String eventAttributeType, Transform transform, Config config)
		{
			this.config = config;
			this.transforms = new HashMap<String, Transform>();
			addTransform(eventAttributeType, transform);
		}

        public void addTransform(String eventAttributeType, Transform transform)
		{
			this.transforms.put(eventAttributeType, transform);
		}

		public Config getConfig()
		{
			return config;
		}

		public Map<String, Transform> getTransforms()
		{
			return transforms;
		}
	}
	
	public class _AbortCollectorException extends RuntimeException
	{
		String message;

		public _AbortCollectorException(String message)
		{
			this.message = message;
		}
	}
}
