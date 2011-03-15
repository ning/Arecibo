package com.ning.arecibo.dashboard.dao;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.ning.arecibo.collector.RemoteCollector;
import com.ning.arecibo.collector.ResolutionTagGenerator;
import com.ning.arecibo.collector.contentstore.DbEntryUtil;
import com.ning.arecibo.dashboard.guice.CollectorCacheMaxWaitMS;
import com.ning.arecibo.dashboard.guice.CollectorCacheSize;
import com.ning.arecibo.dashboard.guice.CollectorCacheTimeToLiveMS;
import com.ning.arecibo.dashboard.guice.CollectorHostOverride;
import com.ning.arecibo.dashboard.guice.CollectorRMIPortOverride;
import com.ning.arecibo.dashboard.guice.CollectorServiceName;
import com.ning.arecibo.dashboard.guice.DashboardConstants;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.util.LRUCache;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.UUIDUtil;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceListener;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.arecibo.util.xml.XStreamUtils;
import com.thoughtworks.xstream.XStream;

public final class DashboardCollectorDAO
{
	private final static Logger log = Logger.getLogger(DashboardCollectorDAO.class);
	
	//TODO: Should be injected, set to 1 minute
	private static long BASE_REDUCTION_MILLIS = 60000L;

	private final static TimeZone localTZ = TimeZone.getDefault();
	private final static long localOffset = localTZ.getOffset(System.currentTimeMillis());
	private final static long defaultWindowSize = 30L;
	private final static long defaultTimeWindow = TimeUnit.MILLISECONDS.convert(defaultWindowSize, TimeUnit.MINUTES);

    private final AtomicLong queryCounter = new AtomicLong();
    private final AtomicLong queryTotalMs = new AtomicLong();
	private final AtomicLong dbQueryCounter = new AtomicLong();
	private final AtomicLong dbQueryTotalMs = new AtomicLong();
    private final AtomicLong collectorQueryCounter = new AtomicLong();
    private final AtomicLong collectorQueryTotalMs = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong cacheHitWaitTime = new AtomicLong();
    private final AtomicLong cacheEventsLoaded = new AtomicLong();
    private final AtomicLong cacheEventsReused = new AtomicLong();
    private final AtomicLong cacheTimeouts = new AtomicLong();
    private final AtomicLong cacheRequestFailuresDueToGC = new AtomicLong();

	private final AtomicReference<RemoteCollector> collector = new AtomicReference<RemoteCollector>();
	private volatile ResolutionTagGenerator resTagGenerator;
	private volatile int[] reductionFactors;
	private volatile Map<Integer,String> resolutionTags;

	private final IDBI dbi;
	private final String collectorHostOverride;
	private final int collectorRMIPortOverride;
	private final int collectorCacheSize;
    private final long collectorCacheMaxWaitMS;
    private final long collectorCacheTimeToLiveMS;
	private final XStream xstream = XStreamUtils.getXStreamNoStringCache();

	private final LRUCache<String, CachedEvents> lruCache;

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    @Inject
	public DashboardCollectorDAO(@Named(DashboardConstants.DASHBOARD_COLLECTOR_DB) IDBI dbi,
	                    ServiceLocator cluster,
                        @CollectorServiceName String collectorServiceName,
	                    @CollectorCacheSize int collectorCacheSize,
                        @CollectorCacheMaxWaitMS long collectorCacheMaxWaitMS,
                        @CollectorCacheTimeToLiveMS long collectorCacheTimeToLiveMS,
	                    @CollectorHostOverride String collectorHostOverride,
	                    @CollectorRMIPortOverride int collectorRMIPortOverride)
	{
		this.dbi = dbi;
		this.collectorCacheSize = collectorCacheSize;
        this.collectorCacheMaxWaitMS = collectorCacheMaxWaitMS;
        this.collectorCacheTimeToLiveMS = collectorCacheTimeToLiveMS;
		this.collectorHostOverride = collectorHostOverride;
		this.collectorRMIPortOverride = collectorRMIPortOverride;
		
		this.reductionFactors = null;
		this.resolutionTags = null;

		this.lruCache = new LRUCache<String, CachedEvents>(this.collectorCacheSize);
	
		if (collectorHostOverride == null || collectorHostOverride.length() == 0) {
			initRemoteCollectorViaBeacon(cluster,collectorServiceName);
		}
		else {
			initRemoteCollectorViaCollectorHostOverride();
		}
		
	}
	
	public int[] getReductionFactors() {
        if(this.reductionFactors == null)
            return null;
        else
		    return this.reductionFactors.clone();
	}

	private void initRemoteCollectorViaCollectorHostOverride()
	{
		log.info("Using collectorHostOverride at " + collectorHostOverride);
		log.info("Using collectorRMIPortOverride " + collectorRMIPortOverride);

		try {
			Registry r = LocateRegistry.getRegistry(collectorHostOverride, collectorRMIPortOverride);
			RemoteCollector c = (RemoteCollector) r.lookup(RemoteCollector.class.getSimpleName());
			collector.set(c);
			log.info("found collector %s", collector);
			
			initResolutionFactors();
		}
		catch (RemoteException e) {
			log.error(e);
		}
		catch (NotBoundException e) {
			log.error(e);
		}
	}


	private void initRemoteCollectorViaBeacon(ServiceLocator cluster,final String collectorServiceName)
	{

		final Selector selector = new Selector()
		{
			public boolean match(ServiceDescriptor sd)
			{
				return sd.getName().equals(collectorServiceName);
			}
		};

		cluster.registerListener(selector, Executors.newFixedThreadPool(1), new ServiceListener()
		{

			public void onRemove(ServiceDescriptor serviceDescriptor)
			{
				// this seems to be highly unreliable, don't reset anything here, seems to come out of order, and late...
				log.info("Got onRemove beacon event for the collector (ignoring)");
			}

			public void onAdd(ServiceDescriptor sd)
			{
				//TODO: Remove dependence on beacon here, not sure we always get this event
				int port = Integer.parseInt(sd.getProperties().get("arecibo.rmi.port"));
				String host = sd.getProperties().get(EventService.HOST);

				try {
					Registry r = LocateRegistry.getRegistry(host, port);
					RemoteCollector c = (RemoteCollector) r.lookup(RemoteCollector.class.getSimpleName());
					collector.set(c);
					
					initResolutionFactors();
				}
				catch (RemoteException e) {
					log.warn(e);
				}
				catch (NotBoundException e) {
					log.warn(e);
				}
				catch (RuntimeException e) {
					log.warn(e);
				}
			}
		});

		cluster.startReadOnly();
	}

	private void initResolutionFactors() throws RemoteException {

        long startMs = System.currentTimeMillis();

        try {
		    reductionFactors = getCollector().getReductionFactors();
		    resTagGenerator = getCollector().getResolutionTagGenerator();
		    resolutionTags = new HashMap<Integer,String>();
	
		    log.info("Available reduction factors from the collector:");
		    for(int i=0;i<reductionFactors.length;i++) {
			    resolutionTags.put(reductionFactors[i],resTagGenerator.getResolutionTag(reductionFactors[i]));
			    log.info("\t" + resolutionTags.get(reductionFactors[i]));
		    }
        }
        finally {
            // increment twice
            incrementCollectorQueryCount();
            logCollectorQueryTime(startMs);
        }
	}
		
	private RemoteCollector getCollector() throws RemoteException
	{
        RemoteCollector coll = collector.get();
		if (coll == null) {
			throw new RemoteException("Collector not initialized !");
		}
		return coll;
	}

	private HashMap<String, Map<String, Object>> convert(Map<String, MapEvent> map)
	{
		if (map == null) {
			return null;
		}
		HashMap<String, Map<String, Object>> newMap = new HashMap<String, Map<String, Object>>();
		for (Map.Entry<String, MapEvent> entry : map.entrySet()) {
			MapEvent me = entry.getValue();

			for (Map.Entry<String, Object> r : me.getMap().entrySet()) {
				if (CachedEvents.isUsefulValue(r.getValue())) {
					String key = me.getEventType() + "_" + r.getKey();
					newMap.put(key, CachedEvents.convert(me, r.getKey(), r.getValue()));
				}
			}
		}
		return newMap;
	}

    public Map<String, Map<String, Object>> getLastValuesForHost(String host)
            throws DashboardCollectorDAOException
    {
        return getLastValuesForHost(host,null);
    }

	public Map<String, Map<String, Object>> getLastValuesForHost(String host,String eventType)
			throws DashboardCollectorDAOException
	{
		long startMs = System.currentTimeMillis();
		
		try {
			return convert(getCollector().getLastValuesForHost(getDefaultSince(),host,eventType));
		}
		catch (RemoteException e) {
			throw new DashboardCollectorDAOException("RemoteException:",e);
		}
		catch (RuntimeException e) {
			throw new DashboardCollectorDAOException("RuntimeException:",e);
		}
		finally {
            logCollectorQueryTime(startMs);
		}
	}


    public Map<String, Map<String, Object>> getLastValuesForType(String type)
            throws DashboardCollectorDAOException
    {
        return getLastValuesForType(type,null);
    }

	public Map<String, Map<String, Object>> getLastValuesForType(String type,String eventType)
			throws DashboardCollectorDAOException
	{
		long startMs = System.currentTimeMillis();

		try {
			return convert(getCollector().getLastValuesForType(getDefaultSince(),type,eventType));
		}
		catch (RemoteException e) {
			throw new DashboardCollectorDAOException("RemoteException:",e);
		}
		catch (RuntimeException e) {
			throw new DashboardCollectorDAOException("RuntimeException:",e);
		}
		finally {
            logCollectorQueryTime(startMs);
		}
	}


    public Map<String, Map<String, Object>> getLastValuesForPathWithType(String path, String type)
            throws DashboardCollectorDAOException
    {
        return getLastValuesForPathWithType(path,type,null);
    }

	public Map<String, Map<String, Object>> getLastValuesForPathWithType(String path, String type,String eventType)
			throws DashboardCollectorDAOException
	{
		long startMs = System.currentTimeMillis();

		try {
			return convert(getCollector().getLastValuesForPathWithType(getDefaultSince(),path, type, eventType));
		}
		catch (RemoteException e) {
			throw new DashboardCollectorDAOException("RemoteException:",e);
		}
		catch (RuntimeException e) {
			throw new DashboardCollectorDAOException("RuntimeException:",e);
		}
		finally {
            logCollectorQueryTime(startMs);
		}
	}

    public List<String> getLastEventTypesForHost(String host)
            throws DashboardCollectorDAOException
    {
        long startMs = System.currentTimeMillis();

        try {
            return getCollector().getLastEventTypesForHost(getDefaultSince(), host);
        }
        catch (RemoteException e) {
            throw new DashboardCollectorDAOException("RemoteException:", e);
        }
        catch (RuntimeException e) {
            throw new DashboardCollectorDAOException("RuntimeException:", e);
        }
        finally {
            logCollectorQueryTime(startMs);
        }
    }

    public List<String> getLastEventTypesForType(String type)
            throws DashboardCollectorDAOException
    {
        long startMs = System.currentTimeMillis();

        try {
            return getCollector().getLastEventTypesForType(getDefaultSince(),type);
        }
        catch (RemoteException e) {
            throw new DashboardCollectorDAOException("RemoteException:",e);
        }
        catch (RuntimeException e) {
            throw new DashboardCollectorDAOException("RuntimeException:",e);
        }
        finally {
            logCollectorQueryTime(startMs);
        }
    }

    public List<String> getLastEventTypesForPathWithType(final String path, final String type)
            throws DashboardCollectorDAOException
    {
        long startMs = System.currentTimeMillis();

        try {
            return getCollector().getLastEventTypesForPathWithType(getDefaultSince(),path, type);
        }
        catch (RemoteException e) {
            throw new DashboardCollectorDAOException("RemoteException:",e);
        }
        catch (RuntimeException e) {
            throw new DashboardCollectorDAOException("RuntimeException:",e);
        }
        finally {
            logCollectorQueryTime(startMs);
        }
    }

	private String createCacheKey(Object... args)
	{
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			for (Object o : args) {
				md5.update(o.toString().getBytes());
			}
			byte[] d = md5.digest();
			return UUIDUtil.md5ToString(d);
		}
		catch (NoSuchAlgorithmException e) {
		}
		return "";
	}

	private Long roundToMin(Long timeWindow)
	{
		if (timeWindow == null) {
			return null;
		}
		return (long) (timeWindow / 60000L);
	}
	
	public List<Map<String, Object>> getValuesForHostEvent(final String host, final String eventType, final String key,
										final ResolutionRequest resRequest, final Long timeWindow, final Long ... timeFromArg)
			throws DashboardCollectorDAOException
	{
		if(reductionFactors == null)
			return null;
		
		final Long timeFrom;
		if(timeFromArg.length >= 1)
			timeFrom = timeFromArg[0];
		else
			timeFrom = null;
		
		List<Integer> reductions = resRequest.getPreferredRequestSequence(BASE_REDUCTION_MILLIS,timeWindow,reductionFactors);
		for(Integer reduction:reductions) {
			
			String resolutionTag = resolutionTags.get(reduction);
			List<Map<String, Object>> retValues = getValuesForHostEvent(host,eventType,key,resolutionTag,timeWindow,timeFrom);
			
			if(retValues != null && retValues.size() > 1) {
				//log.debug("Found data at reduction: %d (%s), %d values for event %s retrieved",reduction,resolutionTag,retValues.size(),eventType);
				
				resRequest.setSelectedReduction(reduction);
				return retValues;
			}
			else {
				//log.debug("Found NO data at reduction: %d (%s), for event %s retrieved",reduction,resolutionTag,eventType);
			}
		}
		
		return null;
	}
			

	
	private List<Map<String, Object>> getValuesForHostEvent(final String host, final String eventType, final String key, 
																			final String reductionTag, final Long timeWindow, final Long timeFrom)
			throws DashboardCollectorDAOException
	{
		HashMap<String,Object> defines = new HashMap<String,Object>();
		HashMap<String,Object> bindings = new HashMap<String,Object>();
		
		defines.put("reductionTag",reductionTag);
		bindings.put("host", host);
		bindings.put("event_type", eventType + "_host" + reductionTag);
		
		if (timeFrom == null) {
			bindings.put("from", new Timestamp(System.currentTimeMillis() - timeWindow - localOffset));	
			
			return withTimeMeasured(":host_event_values", key, createCacheKey(host, eventType, reductionTag, timeWindow, roundToMin(System.currentTimeMillis())),defines,bindings);
		}

		defines.put("between", true);
		bindings.put("to", new Timestamp(timeFrom - localOffset + timeWindow));
		bindings.put("from", new Timestamp(timeFrom - localOffset));	
		
		return withTimeMeasured(":host_event_values", key, createCacheKey(host, eventType, reductionTag, timeWindow, roundToMin(timeFrom)),defines,bindings);
	}

	public List<Map<String, Object>> getValuesForTypeEvent(final String type, final String eventType, final String key,
																			final ResolutionRequest resRequest, final Long timeWindow, final Long ... timeFromArg)
			throws DashboardCollectorDAOException	
	{
		if(reductionFactors == null)
			return null;
		
		final Long timeFrom;
		if(timeFromArg.length >= 1)
			timeFrom = timeFromArg[0];
		else
			timeFrom = null;
		
		List<Integer> reductions = resRequest.getPreferredRequestSequence(BASE_REDUCTION_MILLIS,timeWindow,reductionFactors);
		for(Integer reduction:reductions) {
			List<Map<String, Object>> retValues = getValuesForTypeEvent(type,eventType,key,resolutionTags.get(reduction),timeWindow,timeFrom);
			
			if(retValues != null && retValues.size() > 1) {
				resRequest.setSelectedReduction(reduction);
				return retValues;
			}
		}
		
		return null;
	}	
	
	private List<Map<String, Object>> getValuesForTypeEvent(final String type, final String eventType, final String key, 
																			final String reductionTag, final Long timeWindow, final Long timeFrom)
			throws DashboardCollectorDAOException
	{
		HashMap<String,Object> defines = new HashMap<String,Object>();
		HashMap<String,Object> bindings = new HashMap<String,Object>();
		
		defines.put("reductionTag",reductionTag);
		bindings.put("type", type);
		bindings.put("event_type", eventType + "_type" + reductionTag);
		
		if (timeFrom == null) {
			bindings.put("from", new Timestamp(System.currentTimeMillis() - timeWindow - localOffset));		
			
			return withTimeMeasured(":type_event_values", key, createCacheKey(type, eventType, reductionTag, timeWindow, roundToMin(System.currentTimeMillis())),defines,bindings);
		}

		defines.put("between", true);
		bindings.put("to", new Timestamp(timeFrom - localOffset + timeWindow));
		bindings.put("from", new Timestamp(timeFrom - localOffset));	
		
		return withTimeMeasured(":type_event_values", key, createCacheKey(type, eventType, reductionTag, timeWindow, roundToMin(timeFrom)),defines,bindings);
	}

	public List<Map<String, Object>> getValuesForPathWithTypeEvent(final String path, final String type, final String eventType, final String key,
																			final ResolutionRequest resRequest, final Long timeWindow, final Long ... timeFromArg)
			throws DashboardCollectorDAOException	
	{
		if(reductionFactors == null)
			return null;
		
		final Long timeFrom;
		if(timeFromArg.length >= 1)
			timeFrom = timeFromArg[0];
		else
			timeFrom = null;
		
		List<Integer> reductions = resRequest.getPreferredRequestSequence(BASE_REDUCTION_MILLIS,timeWindow,reductionFactors);
		for(Integer reduction:reductions) {
			List<Map<String, Object>> retValues = getValuesForPathWithTypeEvent(path,type,eventType,key,resolutionTags.get(reduction),timeWindow,timeFrom);
			
			if(retValues != null && retValues.size() > 1) {
				resRequest.setSelectedReduction(reduction);
				return retValues;
			}
		}
		
		return null;
	}	
	
	private List<Map<String, Object>> getValuesForPathWithTypeEvent(final String path, final String type, final String eventType, final String key, 
																					final String reductionTag, final Long timeWindow, final Long timeFrom)
			throws DashboardCollectorDAOException
	{
		HashMap<String,Object> defines = new HashMap<String,Object>();
		HashMap<String,Object> bindings = new HashMap<String,Object>();
		
		defines.put("reductionTag",reductionTag);
		bindings.put("path", path);
		bindings.put("type", type);
		bindings.put("event_type", eventType + "_path" + reductionTag);
		
		if (timeFrom == null) {
			bindings.put("from", new Timestamp(System.currentTimeMillis() - timeWindow - localOffset));	
			
			return withTimeMeasured(":path_event_values", key, createCacheKey(path, type, eventType, reductionTag, timeWindow, roundToMin(System.currentTimeMillis())),defines,bindings);
		}
		
		defines.put("between", true);
		bindings.put("to", new Timestamp(timeFrom - localOffset + timeWindow));
		bindings.put("from", new Timestamp(timeFrom - localOffset));	
		
		return withTimeMeasured(":path_event_values", key, createCacheKey(path, type, eventType, reductionTag, timeWindow, roundToMin(timeFrom)),defines,bindings);
	}

	private List<Map<String, Object>> withTimeMeasured(final String template, final String key, String cacheKey,
									final Map<String,Object> defines, final Map<String, Object> bindings) throws DashboardCollectorDAOException
	{
		long startMs = System.currentTimeMillis();
		try {
			return withHandle(template, key, cacheKey, defines, bindings);
		}
		catch (RuntimeException e) {
			throw new DashboardCollectorDAOException("Problem retrieving data from DB", e);
		}
		finally {
			logQueryTime(startMs);
		}
	}

	private List<Map<String, Object>> withHandle(final String template, final String key, final String cacheKey, 
												final Map<String,Object> defines, final Map<String, Object> bindings)
			throws DashboardCollectorDAOException
	{
		CachedEvents cachedEvents = null;
		boolean isNew = false;
		synchronized(lruCache) {
			cachedEvents = lruCache.get(cacheKey);
			if ( cachedEvents == null || cachedEvents.isDataRefGarbageCollected() ) {
				cachedEvents = new CachedEvents(cacheKey);
				lruCache.put(cacheKey, cachedEvents);
				isNew = true ;
			}
		}

		if ( isNew ) {
            incrementCacheMisses();

			log.debug("running query for cacheKey %s", cacheKey);

            long startQueryTime = System.currentTimeMillis();
			List<MapEvent> results = getEvents(template, defines, bindings);
            logDbQueryTime(startQueryTime);
            
			cachedEvents.setDataRef(results);

            updateCacheEventsLoaded(results.size());

			scheduler.schedule(new Runnable()
			{
				public void run()
				{
					synchronized(lruCache) {
						lruCache.remove(cacheKey);
					}
				}
			}, collectorCacheTimeToLiveMS, TimeUnit.MILLISECONDS);
			return CachedEvents.getValues(key, results);
		}
		else {
            incrementCacheHits();

			try {
                log.debug("waiting for cacheKey %s", cacheKey);

                long startWaitTime = System.currentTimeMillis();
				cachedEvents.waitFor(collectorCacheMaxWaitMS, TimeUnit.MILLISECONDS);
                logCacheHitWaitTime(startWaitTime);
            }
            catch (InterruptedException e) {
                throw new DashboardCollectorDAOException("Got InterruptedException",e);
            }

            List<MapEvent> results = cachedEvents.getEvents() ;
            if ( results != null ) {

                if(log.isDebugEnabled()) {
                    log.debug("got cached values for cacheKey %s, %d events", cacheKey, results.size());
                }

                updateCacheEventsReused(results.size());
                return CachedEvents.getValues(key, results);
            }
            else if(cachedEvents.isDataRefGarbageCollected()) {
                synchronized(lruCache) {
                    lruCache.remove(cacheKey);
                }
                incrementCacheRequestFailuresDueToGC();
                throw new DashboardCollectorDAOException(String.format("softReference for cacheKey %s got garbage collected",cacheKey));
            }
            else {
                incrementCacheTimeouts();
                throw new DashboardCollectorDAOException(String.format("timeout waiting for cachedKey = %s", cacheKey));
            }
		}
	}

	private List<MapEvent> getEvents(final String template, final Map<String, Object> defines, final Map<String, Object> bindings)
			throws DashboardCollectorDAOException
	{
		try {
			List<MapEvent> results = dbi.withHandle(new HandleCallback<List<MapEvent>>()
			{
				public List<MapEvent> withHandle(Handle handle) throws DashboardCollectorDAOException
				{
					Query query = handle.createQuery(getClass().getPackage().getName() + template);
					for(String key:defines.keySet()) {
						query.define(key, defines.get(key));
					}
					for (String key:bindings.keySet()) {
						query.bind(key, bindings.get(key));
					}
					try {
						return query.map(new ResultSetMapper<MapEvent>()
						{
							public MapEvent map(int i, ResultSet rs, StatementContext statementContext) throws SQLException
							{
								try {
									return (MapEvent) xstream.fromXML(DbEntryUtil.getEntryFromRow(rs));
								}
								catch (IOException e) {
									throw new SQLException(e);
								}
							}
						}).list();
					}
					catch(RuntimeException ruEx) {
						throw new DashboardCollectorDAOException("RuntimeException:",ruEx);
					}
				}
			});
			
			return results;
		}
		catch(RuntimeException ruEx) {
			throw new DashboardCollectorDAOException("RuntimeException:",ruEx);
		}
		
	}
	
	public List<String> getHostsForType(final String type)
			throws DashboardCollectorDAOException
	{
        long startMs = System.currentTimeMillis();

		try {
			return new ArrayList<String>(getCollector().getHosts(getDefaultSince(), type));
		}
		catch (Exception e) {
			throw new DashboardCollectorDAOException("Problem retrieving data from Collector", e);
		}
        finally {
            logCollectorQueryTime(startMs);
        }
	}

	public List<String> getHostsForPathWithType(final String path, final String type)
			throws DashboardCollectorDAOException
	{
        long startMs = System.currentTimeMillis();

		try {
			return new ArrayList<String>(getCollector().getHosts(getDefaultSince(), type, path));
		}
		catch (Exception e) {
			throw new DashboardCollectorDAOException("Problem retrieving data from Collector", e);
		}
        finally {
            logCollectorQueryTime(startMs);
        }
	}

	public List<String> getHostsOverall()
			throws DashboardCollectorDAOException
	{
        long startMs = System.currentTimeMillis();

		try {
			return new ArrayList<String>(getCollector().getHosts(getDefaultSince()));
		}
		catch (Exception e) {
			throw new DashboardCollectorDAOException("Problem retrieving data from Collector", e);
		}
        finally {
            logCollectorQueryTime(startMs);
        }
	}

	public List<String> getTypesOverall()
			throws DashboardCollectorDAOException
	{
        long startMs = System.currentTimeMillis();

		try {
			return new ArrayList<String>(getCollector().getTypes(getDefaultSince()));
		}
		catch (Exception e) {
			throw new DashboardCollectorDAOException("Problem retrieving data from Collector", e);
		}
        finally {
            logCollectorQueryTime(startMs);
        }
	}

	public List<String> getTypesForPath(final String path)
			throws DashboardCollectorDAOException
	{
        long startMs = System.currentTimeMillis();

		try {
			return new ArrayList<String>(getCollector().getTypes(getDefaultSince(), path));
		}
		catch (Exception e) {
			throw new DashboardCollectorDAOException("Problem retrieving data from Collector", e);
		}
        finally {
            logCollectorQueryTime(startMs);
        }
	}

	public List<String> getPathsForType(final String type)
			throws DashboardCollectorDAOException
	{
        long startMs = System.currentTimeMillis();

		try {
			return new ArrayList<String>(getCollector().getPaths(getDefaultSince(), type));
		}
		catch (Exception e) {
			throw new DashboardCollectorDAOException("Problem retrieving data from Collector", e);
		}
        finally {
            logCollectorQueryTime(startMs);
        }
	}

	public List<String> getPathsOverall()
			throws DashboardCollectorDAOException
	{
        long startMs = System.currentTimeMillis();

		try {
			return new ArrayList<String>(getCollector().getPaths(getDefaultSince()));
		}
		catch (Exception e) {
			throw new DashboardCollectorDAOException("Problem retrieving data from Collector", e);
		}
        finally {
            logCollectorQueryTime(startMs);
        }
	}

	public long getDefaultSince()
	{
		return System.currentTimeMillis() - defaultTimeWindow;
	}


    private void logQueryTime(long startMs)
    {
        incrementQueryCount();
        updateQueryTotalMs(System.currentTimeMillis() - startMs);
    }

    private void logDbQueryTime(long startMs)
    {
        incrementDbQueryCount();
        updateDbQueryTotalMs(System.currentTimeMillis() - startMs);
    }

    private void logCollectorQueryTime(long startMs)
    {
        incrementCollectorQueryCount();
        updateCollectorQueryTotalMs(System.currentTimeMillis() - startMs);
    }

    private void logCacheHitWaitTime(long startMs)
    {
        updateCacheHitWaitTime(System.currentTimeMillis() - startMs);
    }

    private void incrementCacheHits()
    {
        cacheHits.incrementAndGet();
    }

    private void incrementCacheMisses()
    {
        cacheMisses.incrementAndGet();
    }

    private void incrementCacheTimeouts()
    {
        cacheTimeouts.incrementAndGet();
    }

    private void incrementCacheRequestFailuresDueToGC()
    {
        cacheRequestFailuresDueToGC.incrementAndGet();
    }

    private void updateCacheEventsLoaded(long numLoaded) {
        cacheEventsLoaded.addAndGet(numLoaded);
    }

    private void updateCacheEventsReused(long numLoaded) {
        cacheEventsReused.addAndGet(numLoaded);
    }

    private void incrementQueryCount()
    {
        queryCounter.incrementAndGet();
    }

    private void updateQueryTotalMs(long updateMs)
    {
        queryTotalMs.addAndGet(updateMs);
    }

    private void incrementDbQueryCount()
    {
        dbQueryCounter.incrementAndGet();
    }

    private void updateDbQueryTotalMs(long updateMs)
    {
        dbQueryTotalMs.addAndGet(updateMs);
    }

	private void incrementCollectorQueryCount()
	{
		collectorQueryCounter.incrementAndGet();
	}

    private void updateCollectorQueryTotalMs(long updateMs)
    {
        collectorQueryTotalMs.addAndGet(updateMs);
    }

	private void updateCacheHitWaitTime(long updateMs)
	{
		cacheHitWaitTime.addAndGet(updateMs);
	}

	@MonitorableManaged(monitored = true)
    public int getCacheSize() {
        return lruCache.size();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getCacheHits()
    {
        return cacheHits.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getCacheMisses()
    {
        return cacheMisses.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getCacheHitWaitTime()
    {
        return cacheHitWaitTime.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.VALUE, MonitoringType.COUNTER, MonitoringType.RATE })
    public long getCacheTimeouts()
    {
        return cacheTimeouts.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.VALUE, MonitoringType.COUNTER, MonitoringType.RATE })
    public long getCacheRequestFailuresDueToGC()
    {
        return cacheRequestFailuresDueToGC.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getCacheEventsLoaded()
    {
        return cacheEventsLoaded.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getCacheEventsReused()
    {
        return cacheEventsReused.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getQueryCount()
    {
        return queryCounter.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getQueryTotalMs()
    {
        return queryTotalMs.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getDbQueryCount()
    {
        return dbQueryCounter.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getDbQueryTotalMs()
    {
        return dbQueryTotalMs.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
	public long getCollectorQueryCount()
	{
		return collectorQueryCounter.get();
	}

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
	public long getCollectorQueryTotalMs()
	{
		return collectorQueryTotalMs.get();
	}
}
