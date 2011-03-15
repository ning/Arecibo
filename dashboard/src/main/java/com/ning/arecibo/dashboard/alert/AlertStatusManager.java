package com.ning.arecibo.dashboard.alert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.ning.arecibo.dashboard.guice.AlertManagerEnabled;

import com.ning.arecibo.util.Logger;


import static com.ning.arecibo.dashboard.context.DashboardContextUtils.UNDEFINED_HOST_NAME;
import static com.ning.arecibo.dashboard.context.DashboardContextUtils.UNDEFINED_PATH_NAME;
import static com.ning.arecibo.dashboard.context.DashboardContextUtils.UNDEFINED_TYPE_NAME;

public class AlertStatusManager implements Runnable
{
    private final static Logger log = Logger.getLogger(AlertStatusManager.class);
    
    public final static String HOST_ATTR = "hostName";
    public final static String TYPE_ATTR = "deployedType";
    public final static String PATH_ATTR = "deployedConfigSubPath";
    private final static String KEY_DELIM = "$";
    
    //TODO: Inject this
    // poll every 10 seconds (will want to inject this later, or change to happen on demand, with minimum refresh interval)
    private final static long CONFIG_UPDATE_INTERVAL = 1000L * 10L;
    private AtomicLong generationCount = new AtomicLong(Long.MIN_VALUE);
 
    private ScheduledThreadPoolExecutor executor;
    
    private final ClusterAwareAlertClient alertClient;
    private final Boolean alertManagerEnabled;

    private final ConcurrentHashMap<String, DashboardAlertStatus> alertsByHost = new ConcurrentHashMap<String, DashboardAlertStatus>();
    private final ConcurrentHashMap<String, DashboardAlertStatus> alertsByType = new ConcurrentHashMap<String, DashboardAlertStatus>();
    private final ConcurrentHashMap<String, DashboardAlertStatus> alertsByPathWithType = new ConcurrentHashMap<String, DashboardAlertStatus>();
    private final ConcurrentHashMap<String, DashboardAlertStatus> alertsOverall = new ConcurrentHashMap<String, DashboardAlertStatus>();
    
    private final ConcurrentHashMap<String,Long> alertMatchMap = new ConcurrentHashMap<String,Long>();
    
    private volatile Boolean alertStatusAvailable = false;


    @Inject
    public AlertStatusManager(@AlertManagerEnabled Boolean alertManagerEnabled,
                              ClusterAwareAlertClient alertClient) {
    	
    	this.alertManagerEnabled = alertManagerEnabled;
        this.alertClient = alertClient;
    }
    
    public synchronized void start()
    {
    	if(!alertManagerEnabled) {
    		log.info("Disabling the alertManager");
    		return;
    	}
    	
        // one thread should be fine
        this.executor = new ScheduledThreadPoolExecutor(1);

        // start the config updater
        this.executor.scheduleAtFixedRate(this,0,CONFIG_UPDATE_INTERVAL,TimeUnit.MILLISECONDS);
    }

    public synchronized void stop()
    {
        if (this.executor != null) {
            this.executor.shutdown();
            this.executor = null;
        }
    }

    // this doesn't need to be synchronized, it's ok if it's caught in between
    public boolean isAlertStatusAvailable() {
        return this.alertStatusAvailable;
    }

    public boolean isHostMetricInAlert(String eventType,String attributeType,String hostName) {
    	
    	String key = getHostKey(eventType,attributeType,hostName);
    	if(alertMatchMap.containsKey(key)) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }

    public boolean isTypeMetricInAlert(String eventType,String attributeType,String typeName) {
    	
    	String key = getTypeKey(eventType,attributeType,typeName);
    	if(alertMatchMap.containsKey(key))
    		return true;
    	else
    		return false;
    }

    public boolean isPathWithTypeMetricInAlert(String eventType,String attributeType,String path,String typeName) {
    	
    	String key = getPathWithTypeKey(eventType,attributeType,path,typeName);
    	if(alertMatchMap.containsKey(key))
    		return true;
    	else
    		return false;
    }

    public boolean isOverallMetricInAlert(String eventType,String attributeType) {

    	String key = getOverallKey(eventType,attributeType);
    	if(alertMatchMap.containsKey(key))
    		return true;
    	else
    		return false;
    }

    public boolean isMetricInAlert(String eventType,String attributeType,String typeName,String path,String hostName) {
        
    	if(hostName != null && !hostName.equals(UNDEFINED_HOST_NAME)) {
    		return isHostMetricInAlert(eventType,attributeType,hostName);
    	}
    	else if(typeName != null && !typeName.equals(UNDEFINED_TYPE_NAME)) {
    		if(path != null && !path.equals(UNDEFINED_PATH_NAME)) {
    			return isPathWithTypeMetricInAlert(eventType,attributeType,path,typeName);
    		}
    		else {
    			return isTypeMetricInAlert(eventType,attributeType,typeName);
    		}
    	}
    	else {
    		return isOverallMetricInAlert(eventType,attributeType);
    	}
    }
    
    public List<DashboardAlertStatus> getMetricsInAlert() {
        
    	ArrayList<DashboardAlertStatus> retList = new ArrayList<DashboardAlertStatus>();
        Collection<DashboardAlertStatus> alertStatii = alertsOverall.values();
        
        for(DashboardAlertStatus alertStatus:alertStatii) {
            retList.add(alertStatus);
        }
        
        return retList;
    }
    
    public int getNumMetricsInAlert() {
        return alertsOverall.size();
    }
    
    public List<DashboardAlertStatus> getMetricsInAlert(String eventType,String attributeType,String typeName,String path,String hostName) {
        
    	ArrayList<DashboardAlertStatus> retList = new ArrayList<DashboardAlertStatus>();
    	String eventAttributeSubKey = getEventAttributeSubKey(eventType,attributeType);
    	
    	if(hostName != null && !hostName.equals(UNDEFINED_HOST_NAME)) {
    	    
    	    String hostSubKey = getHostSubKey(hostName);
    	    
    	    Set<String> keys = alertsByHost.keySet();
    	    for(String key:keys) {
    	        if(key.contains(eventAttributeSubKey) && key.contains(hostSubKey)) {
    	            DashboardAlertStatus alertStatus = alertsByHost.get(key);
    	            if(alertStatus != null)
    	                retList.add(alertStatus);
    	        }
    	    }
    	    
    	    
    	}
    	else if(typeName != null && !typeName.equals(UNDEFINED_TYPE_NAME)) {
    		if(path != null && !path.equals(UNDEFINED_PATH_NAME)) {
    		    
    		    String pathWithTypeSubKey = getPathWithTypeSubKey(path,typeName);
    		    
        	    Set<String> keys = alertsByPathWithType.keySet();
        	    for(String key:keys) {
        	        if(key.contains(eventAttributeSubKey) && key.contains(pathWithTypeSubKey)) {
        	            DashboardAlertStatus alertStatus = alertsByPathWithType.get(key);
        	            if(alertStatus != null)
        	                retList.add(alertStatus);
        	        }
        	    }
    	    
    		}
    		else {
    		    
    		    String typeSubKey = getTypeSubKey(typeName);
    		    
        	    Set<String> keys = alertsByType.keySet();
        	    for(String key:keys) {
        	        if(key.contains(eventAttributeSubKey) && key.contains(typeSubKey)) {
        	            DashboardAlertStatus alertStatus = alertsByType.get(key);
        	            if(alertStatus != null)
        	                retList.add(alertStatus);
        	        }
        	    }
    	    
    		}
    	}
    	else {
    	    Set<String> keys = alertsOverall.keySet();
    	    for(String key:keys) {
    	        if(key.contains(eventAttributeSubKey)) {
    	            DashboardAlertStatus alertStatus = alertsOverall.get(key);
    	            if(alertStatus != null)
    	                retList.add(alertStatus);
    	        }
    	    }
    	}
    	
    	if(retList.size() == 0)
    	    return null;
    	
    	return retList;
    }

    public synchronized void run() {
        
        try {
            List<DashboardAlertStatus> alertStatusList = alertClient.getAlertStatus(generationCount.get());
            
            if(alertStatusList != null && alertStatusList.size() > 0) {
                for(DashboardAlertStatus alertStatus:alertStatusList) {

            		addToHostMapIfApplicable(alertStatus);
            		addToTypeMapIfApplicable(alertStatus);
            		addToPathWithTypeMapIfApplicable(alertStatus);
            		addToOverallMap(alertStatus);
                }
            }
            
            purgePreviousGenerations(generationCount.getAndIncrement());
            
            alertStatusAvailable = true;
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx,"Got RuntimeException retrieving alert status data");
            
            try {
                alertStatusAvailable = false;
                purgePreviousGenerations(generationCount.getAndIncrement());
            }
            catch(RuntimeException ruEx2) {
                log.warn(ruEx2);
            }
        }
    }
    
    private String getHostKey(String eventType,String attributeType,String hostName) {
    	return KEY_DELIM + eventType + KEY_DELIM + attributeType + KEY_DELIM + hostName + KEY_DELIM;
    }
    
    private String getTypeKey(String eventType,String attributeType,String typeName) {
    	return KEY_DELIM + eventType + KEY_DELIM + attributeType + KEY_DELIM + typeName + KEY_DELIM;
    }
    
    private String getPathWithTypeKey(String eventType,String attributeType,String path,String typeName) {
    	return KEY_DELIM + eventType + KEY_DELIM + attributeType + KEY_DELIM + path + KEY_DELIM + typeName + KEY_DELIM;
    }
    
    private String getOverallKey(String eventType,String attributeType) {
    	return getEventAttributeSubKey(eventType,attributeType);
    }
    
    private String getHostSubKey(String hostName) {
        return KEY_DELIM + hostName + KEY_DELIM;
    }
    
    private String getPathWithTypeSubKey(String path,String typeName) {
        return KEY_DELIM + path + KEY_DELIM + typeName + KEY_DELIM;
    }
    
    private String getTypeSubKey(String typeName) {
        return KEY_DELIM + typeName + KEY_DELIM;
    }
    
    private String getEventAttributeSubKey(String eventType,String attributeType) {
    	return KEY_DELIM + eventType + KEY_DELIM + attributeType + KEY_DELIM;
    }
    
    private synchronized boolean addToHostMapIfApplicable(DashboardAlertStatus alertStatus) {
    	
    	String hostName;
    	if((hostName = alertStatus.getAttribute(HOST_ATTR)) != null) {
    		String eventType = alertStatus.getEventType();
    		String attributeType = alertStatus.getAttributeType();
    		String key = getHostKey(eventType,attributeType,hostName);
    		
    		String fullKey = key + KEY_DELIM + alertStatus.getAlertId();
    		alertsByHost.put(fullKey,alertStatus);
    		alertMatchMap.put(key, alertStatus.getGenerationCount());
    		
    		return true;
    	}
    	return false;
    }
    
    private synchronized boolean addToTypeMapIfApplicable(DashboardAlertStatus alertStatus) {
    	
    	String typeName;
    	if((typeName = alertStatus.getAttribute(TYPE_ATTR)) != null) {
    		
    		String eventType = alertStatus.getEventType();
    		String attributeType = alertStatus.getAttributeType();
    		String key = getTypeKey(eventType,attributeType,typeName);
    		
    		String fullKey = key + KEY_DELIM + alertStatus.getAlertId();
    		alertsByType.put(fullKey,alertStatus);
    		alertMatchMap.put(key, alertStatus.getGenerationCount());
    		
    		return true;
    	}
    	return false;
    }
    
    private synchronized boolean addToPathWithTypeMapIfApplicable(DashboardAlertStatus alertStatus) {
    	
    	String typeName;
    	String path;
    	if((path = alertStatus.getAttribute(PATH_ATTR)) != null &&
    			(typeName = alertStatus.getAttribute(TYPE_ATTR)) != null) {
    		
    		String eventType = alertStatus.getEventType();
    		String attributeType = alertStatus.getAttributeType();
    		String key = getPathWithTypeKey(eventType,attributeType,path,typeName);
    		
    		String fullKey = key + KEY_DELIM + alertStatus.getAlertId();
    		alertsByPathWithType.put(fullKey,alertStatus);
    		alertMatchMap.put(key, alertStatus.getGenerationCount());
    		
    		return true;
    	}
    	return false;
    }
    
    private synchronized boolean addToOverallMap(DashboardAlertStatus alertStatus) {
    	
		String eventType = alertStatus.getEventType();
		String attributeType = alertStatus.getAttributeType();
		String key = getOverallKey(eventType,attributeType);
		
    	String fullKey = key + KEY_DELIM + alertStatus.getAlertId();
		alertsOverall.put(fullKey,alertStatus);
    	alertMatchMap.put(key, alertStatus.getGenerationCount());
    		
		return true;
    }
    
    private synchronized void purgePreviousGenerations(long genCount) {
    	
    	
    	Set<String> keys = alertMatchMap.keySet();
    	for(String key:keys) {
    		Long matchGenCount = alertMatchMap.get(key);
    		if(matchGenCount != genCount) {
    			alertMatchMap.remove(key);
    		}
    	}
    	
    	keys = alertsByHost.keySet();
    	for(String key:keys) {
    		DashboardAlertStatus alertStatus = alertsByHost.get(key);
    		if(alertStatus.getGenerationCount() != genCount) {
    			alertsByHost.remove(key);
    		}
    	}
    	
    	keys = alertsByType.keySet();
    	for(String key:keys) {
    		DashboardAlertStatus alertStatus = alertsByType.get(key);
    		if(alertStatus.getGenerationCount() != genCount)
    			alertsByType.remove(key);
    	}
    	
    	keys = alertsByPathWithType.keySet();
    	for(String key:keys) {
    		DashboardAlertStatus alertStatus = alertsByPathWithType.get(key);
    		if(alertStatus.getGenerationCount() != genCount)
    			alertsByPathWithType.remove(key);
    	}
    	
    	keys = alertsOverall.keySet();
    	for(String key:keys) {
    		DashboardAlertStatus alertStatus = alertsOverall.get(key);
    		if(alertStatus.getGenerationCount() != genCount)
    			alertsOverall.remove(key);
    	}
    	
    	
    }
}
