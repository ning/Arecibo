package com.ning.arecibo.dashboard.galaxy;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.google.inject.Inject;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.galaxy.GalaxyCorePicker;
import com.ning.arecibo.util.galaxy.GalaxyCoreStatus;

public class GalaxyStatusManager implements Runnable
{
    private final static Logger log = Logger.getLogger(GalaxyStatusManager.class);
    
    // poll every 5 minutes (might want to inject this later)
    private final static long CONFIG_UPDATE_INTERVAL = 1000L * 60L * 5L;
 
    private final GalaxyCorePicker corePicker;
    private ScheduledThreadPoolExecutor executor;
    private ConcurrentHashMap<String,GalaxyCoreStatus> coreStatusMap;
    
    @Inject
    public GalaxyStatusManager(GalaxyCorePicker corePicker) {
        this.corePicker = corePicker;
        this.coreStatusMap = new ConcurrentHashMap<String,GalaxyCoreStatus>();
    }
    
    public synchronized void start()
    {
        // one thread should be fine
        this.executor = new ScheduledThreadPoolExecutor(1);
        
        // start the config updater
        this.executor.scheduleWithFixedDelay(this,0,CONFIG_UPDATE_INTERVAL,TimeUnit.MILLISECONDS);
    }

    public synchronized void stop()
    {
        if (this.executor != null) {
            this.executor.shutdown();
            this.executor = null;
        }
    }

    public String getGlobalZone(String hostName) {
        GalaxyCoreStatus status = coreStatusMap.get(hostName);
        
        if(status == null)
            return null;
        
        return status.getGlobalZoneHostName();
    }
    
    public String getCoreType(String hostName) {
        GalaxyCoreStatus status = coreStatusMap.get(hostName);
        
        if(status == null)
            return null;
        
        return status.getCoreType();
    }
    
    public String getConfigPath(String hostName) {
        GalaxyCoreStatus status = coreStatusMap.get(hostName);
        
        if(status == null)
            return null;
        
        return status.getConfigPath();
    }
    
    public String getConfigSubPath(String hostName) {
        String configPath = getConfigPath(hostName);
        
        if(configPath == null)
            return null;
        
        // look for the 4th component in the path (actually 5th, the leading '/' results in empty string)
        String[] parts = configPath.split("/");
        if(parts.length < 5)
            return null;
        else
            return parts[4];
    }
    
    public void run() {
        
        // update the current list of cores, in concurrently safe way
        // don't want to block access in anyway, it's ok if the coreStatusMap
        // temporarily contains stale hosts
        
        try {
            log.info("Updating the status list of available cores from galaxy");
            List<GalaxyCoreStatus> statii = corePicker.getCores();
            
            if(statii == null) {
                log.info("Retrieved no available cores from galaxy");
                coreStatusMap.clear();
            }
            else {
                log.info("Retrieved list of " + statii.size() + " available cores from galaxy");
            	
                // build a keyed list of cores
            	Set<String> newKeys = new HashSet<String>();
            	for(GalaxyCoreStatus status:statii) {
                	newKeys.add(status.getZoneHostName());
            	}
            
            	// get list of prev keys
            	Set<String> prevKeys = coreStatusMap.keySet();
            	Iterator<String> prevKeyIter = prevKeys.iterator();
            
            	// throw out any that are no longer apparently active
            	while(prevKeyIter.hasNext()) {
                	String prevKey = prevKeyIter.next();
                	if(!newKeys.contains(prevKey)) {
                    	prevKeyIter.remove();
                	}
            	}
            
            	// now add all the new core statii in
            	for(GalaxyCoreStatus status:statii) {
                	coreStatusMap.put(status.getZoneHostName(), status);
            	}
            }
        }
        catch(IOException ioEx) {
            log.warn(ioEx,"Got IOException retrieving galaxy core data");
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx,"Got RuntimeException retrieving galaxy core data");
        }
    }
}
