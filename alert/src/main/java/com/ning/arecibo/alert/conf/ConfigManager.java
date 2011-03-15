package com.ning.arecibo.alert.conf;

import java.lang.Runnable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.sql.Timestamp;

import com.google.inject.Inject;
import com.ning.arecibo.alert.client.AlertActivationStatus;
import com.ning.arecibo.alert.client.AlertStatus;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAOException;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.email.EmailManager;
import com.ning.arecibo.alert.guice.ConfigUpdateInterval;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;
import com.ning.arecibo.alert.objects.AlertIncidentLog;
import com.ning.arecibo.alert.objects.AlertingConfig;
import com.ning.arecibo.alert.objects.ConfigurableObject;
import com.ning.arecibo.alert.objects.ManagingKey;
import com.ning.arecibo.alert.objects.ManagingKeyMapping;
import com.ning.arecibo.alert.objects.NotifConfig;
import com.ning.arecibo.alert.objects.NotifGroup;
import com.ning.arecibo.alert.objects.NotifGroupMapping;
import com.ning.arecibo.alert.objects.NotifMapping;
import com.ning.arecibo.alert.objects.Person;
import com.ning.arecibo.alert.objects.StatefulConfigurableObject;
import com.ning.arecibo.alert.objects.ThresholdConfig;
import com.ning.arecibo.alert.objects.ThresholdContextAttr;
import com.ning.arecibo.alert.objects.ThresholdQualifyingAttr;

import com.ning.arecibo.util.Logger;




public class ConfigManager implements Runnable
{
    private final static Logger log = Logger.getLogger(ConfigManager.class);
    
    private final ConfDataDAO confDataDAO;
    private final AlertManager alertManager;
    private final EmailManager emailManager;
    private final LoggingManager loggingManager;
    private final int configUpdateInterval;

    private final ConcurrentHashMap<Long,AlertingConfig> alertingConfigs;
    private final ConcurrentHashMap<Long,ManagingKey> managingKeys;
    private final ConcurrentHashMap<Long,ManagingKeyMapping> managingKeyMappings;
    private final ConcurrentHashMap<Long,NotifConfig> notifConfigs;
    private final ConcurrentHashMap<Long,NotifGroup> notifGroups;
    private final ConcurrentHashMap<Long,NotifGroupMapping> notifGroupMappings;
    private final ConcurrentHashMap<Long,NotifMapping> notifMappings;
    private final ConcurrentHashMap<Long,Person> persons;
    private final ConcurrentHashMap<Long,ThresholdConfig> thresholdConfigs;
    private final ConcurrentHashMap<Long,ThresholdContextAttr> thresholdContextAttrs;
    private final ConcurrentHashMap<Long,ThresholdQualifyingAttr> thresholdQualifyingAttrs;
    private final ScheduledThreadPoolExecutor executor;

    private final ConcurrentHashMap<Long,List<AlertIncidentLog>> perThresholdAlertIncidentLogs;

    // doesn't need to be volatile, since always accessed under synchronized block
    private ScheduledFuture<?> schedFuture;


    @Inject
    public ConfigManager(ConfDataDAO confDataDAO,
                             AlertManager alertManager,
                             EmailManager emailManager,
                             LoggingManager loggingManager,
                             @ConfigUpdateInterval int configUpdateInterval) {
        this.confDataDAO = confDataDAO;
        this.alertManager = alertManager;
        this.emailManager = emailManager;
        this.loggingManager = loggingManager;
        this.configUpdateInterval = configUpdateInterval;

        this.alertingConfigs = new ConcurrentHashMap<Long, AlertingConfig>();
        this.managingKeys = new ConcurrentHashMap<Long, ManagingKey>();
        this.managingKeyMappings = new ConcurrentHashMap<Long, ManagingKeyMapping>();
        this.notifConfigs = new ConcurrentHashMap<Long, NotifConfig>();
        this.notifGroups = new ConcurrentHashMap<Long, NotifGroup>();
        this.notifGroupMappings = new ConcurrentHashMap<Long, NotifGroupMapping>();
        this.notifMappings = new ConcurrentHashMap<Long, NotifMapping>();
        this.persons = new ConcurrentHashMap<Long,Person>();
        this.thresholdConfigs = new ConcurrentHashMap<Long, ThresholdConfig>();
        this.thresholdContextAttrs = new ConcurrentHashMap<Long,ThresholdContextAttr>();
        this.thresholdQualifyingAttrs = new ConcurrentHashMap<Long,ThresholdQualifyingAttr>();

        this.perThresholdAlertIncidentLogs = new ConcurrentHashMap<Long,List<AlertIncidentLog>>();

        // one thread should be fine
        this.executor = new ScheduledThreadPoolExecutor(1);
    }
    
    public synchronized void start()
    {
        // do needed initialization prior to starting config management loop
        preConfigInitialization();

        // do first pass of the config management loop
        this.run();

        // do needed initialization cleanup after starting config management loop
        postConfigInitialization();
        
        // start the config updater
        this.schedFuture = this.executor.scheduleWithFixedDelay(this,this.configUpdateInterval,this.configUpdateInterval,TimeUnit.SECONDS);
    }

    public synchronized void stop()
    {
        this.executor.shutdown();
        this.schedFuture = null;
    }
    
    public synchronized void update() {
        // try to synchronously update config, without waiting for next update interval delay,
        // cancel current thread then reschedule, with no delay, ex post haste
        if(this.schedFuture == null || this.schedFuture.cancel(false)) {
            this.schedFuture = this.executor.scheduleWithFixedDelay(this,0,this.configUpdateInterval,TimeUnit.SECONDS);
        }
    }

    private void preConfigInitialization() {
        initializeActiveThresholdAlertIncidents();
    }

    private void postConfigInitialization() {
        clearStaleActiveThresholdAlertIncidents();
    }

    public EmailManager getEmailManager() {
        return this.emailManager;
    }

    public AlertingConfig getAlertingConfig(Long id) {
        return alertingConfigs.get(id);
    }

    public ManagingKey getManagingKey(Long id) {
        return managingKeys.get(id);
    }

    public ManagingKeyMapping getManagingKeyMapping(Long id) {
        return managingKeyMappings.get(id);
    }

    public NotifConfig getNotifConfig(Long id) {
        return notifConfigs.get(id);
    }

    public NotifGroup getNotifGroup(Long id) {
        return notifGroups.get(id);
    }

    public NotifGroupMapping getNotifGroupMapping(Long id) {
        return notifGroupMappings.get(id);
    }

    public NotifMapping getNotifMapping(Long id) {
        return notifMappings.get(id);
    }

    public Person getPerson(Long id) {
        return persons.get(id);
    }

    public ThresholdConfig getThresholdConfig(Long id) {
        return thresholdConfigs.get(id);
    }
    
    public ThresholdContextAttr getThresholdContextAttr(Long id) {
        return thresholdContextAttrs.get(id);
    }
    
    public ThresholdQualifyingAttr getThresholdQualifyingAttr(Long id) {
        return thresholdQualifyingAttrs.get(id);
    }

    public List<Long> getThresholdConfigIds() {
    	ArrayList<Long> configIdList = new ArrayList<Long>();
    	configIdList.addAll(thresholdConfigs.keySet());
    	
    	return configIdList;
    }

    public List<AlertIncidentLog> checkActiveThresholdAlertIncidents(Long thresholdConfigId) {
        return perThresholdAlertIncidentLogs.remove(thresholdConfigId);
    }

    public List<AlertStatus> getAlertStatus(AlertActivationStatus activationStatusFilter) {

        // for now, only allow status filter of ERROR
        if(activationStatusFilter == null || !activationStatusFilter.equals(AlertActivationStatus.ERROR))
            throw new IllegalArgumentException("getAlertStatus not implemented yet for AlertActivationStatus: " + activationStatusFilter);

        Set<Long> activeThresholdConfigIds = alertManager.getActiveThresholdConfigIds();
        return ThresholdConfig.getAlertStatus(activeThresholdConfigIds,this,activationStatusFilter);
    }
    
    public String getAlertStatusString() {
        StringBuilder sb = new StringBuilder();
        
        List<AlertStatus> statusList = getAlertStatus(AlertActivationStatus.ERROR);
        
        for(AlertStatus status:statusList) {
            sb.append(String.format("Alert Type: %s, Event Type: %s, Attribute Type: %s, Activation Status: %s\n",
                        status.getAlertType(),status.getEventType(),status.getAttributeType(),status.getActivationStatus()));
        }
        
        return sb.toString();
    }
    
    public String getConfigStatus() {
        
        StringBuilder sb = new StringBuilder();
        
        getConfDataObjectStatus(AlertingConfig.TYPE_NAME, alertingConfigs,sb);
        getConfDataObjectStatus(NotifConfig.TYPE_NAME, notifConfigs,sb);
        getConfDataObjectStatus(NotifGroup.TYPE_NAME, notifGroups,sb);
        getConfDataObjectStatus(NotifGroupMapping.TYPE_NAME, notifGroupMappings,sb);
        getConfDataObjectStatus(NotifMapping.TYPE_NAME, notifMappings,sb);
        getConfDataObjectStatus(Person.TYPE_NAME,persons,sb);
        getConfDataObjectStatus(ManagingKey.TYPE_NAME,managingKeys,sb);
        getConfDataObjectStatus(ManagingKeyMapping.TYPE_NAME,managingKeyMappings,sb);
        getConfDataObjectStatus(ThresholdConfig.TYPE_NAME, thresholdConfigs,sb);
        getConfDataObjectStatus(ThresholdContextAttr.TYPE_NAME,thresholdContextAttrs,sb);
        getConfDataObjectStatus(ThresholdQualifyingAttr.TYPE_NAME,thresholdQualifyingAttrs,sb);

        return sb.toString();
    }
    
    private <T extends ConfDataObject> String getConfDataObjectStatus(String typeName,Map<Long,T> typeMap,StringBuilder sb) {
        
        if(typeMap.size() > 0) {
            sb.append(String.format("\n\nCurrent '%s' configs:\n",typeName));
            for(T t:typeMap.values()) {
        	    t.toStringBuilder(sb);
        	}
        }
        else {
            sb.append(String.format("\n\nNo instances of '%s' configured\n",typeName));
        }
        
        return sb.toString();
    }

    private void initializeActiveThresholdAlertIncidents() {
        try {
            List<AlertIncidentLog> alertIncidentLogs = confDataDAO.selectByColumn("clear_time",(Object)null,"alert_incident_log",AlertIncidentLog.class);

            if(alertIncidentLogs == null) {
                log.info("Found 0 active alert incidents to re-instantiate");
                return;
            }


            log.info("Found %d possible active alert incidents to re-instantiate",alertIncidentLogs.size());

            for(AlertIncidentLog aiLog:alertIncidentLogs) {
                Long thresholdConfigId = aiLog.getThresholdConfigId();
                if(thresholdConfigId == null)
                    continue;

                List<AlertIncidentLog> perThresholdList = perThresholdAlertIncidentLogs.get(thresholdConfigId);
                if(perThresholdList == null) {
                    perThresholdList = new ArrayList<AlertIncidentLog>();
                    perThresholdAlertIncidentLogs.put(aiLog.getThresholdConfigId(),perThresholdList);
                }

                perThresholdList.add(aiLog);
            }
        }
        catch(ConfDataDAOException sysmDAOEx) {
            log.warn(sysmDAOEx,"Got ConfDataDAOException retrieving active alert incidents");
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx,"Got RuntimeException retrieving active alert incidents");
        }
    }

    private void clearStaleActiveThresholdAlertIncidents() {

        try {
            Timestamp clearTime = new Timestamp(System.currentTimeMillis());
            for(List<AlertIncidentLog> aiLogList:perThresholdAlertIncidentLogs.values()) {
                for(AlertIncidentLog aiLog:aiLogList) {
                    log.info("Updating clear_time for stale alert incident log entry: " + aiLog.getLabel());
                    aiLog.setClearTime(clearTime);
                    confDataDAO.update(aiLog);
                }
            }

            perThresholdAlertIncidentLogs.clear();
        }
        catch(ConfDataDAOException sysmDAOEx) {
            log.warn(sysmDAOEx,"Got ConfDataDAOException updating stale active alert incidents");
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx,"Got RuntimeException updating stale active alert incidents");
        }
    }

    public synchronized void run() {
        
        try {
            log.info("Updating alert configuration");

            // for now just load/update these entire tables into memory
            List<AlertingConfig> alertingConfigList =
                confDataDAO.selectAll(AlertingConfig.TYPE_NAME, AlertingConfig.class);

            List<NotifConfig> notifConfigList =
                confDataDAO.selectAll(NotifConfig.TYPE_NAME, NotifConfig.class);

            List<NotifGroup> notifGroupList =
                confDataDAO.selectAll(NotifGroup.TYPE_NAME, NotifGroup.class);

            List<NotifGroupMapping> notifGroupMappingList =
                confDataDAO.selectAll(NotifGroupMapping.TYPE_NAME, NotifGroupMapping.class);

            List<NotifMapping> notifMappingList =
                confDataDAO.selectAll(NotifMapping.TYPE_NAME, NotifMapping.class);

            List<ManagingKey> managingKeyList =
                confDataDAO.selectAll(ManagingKey.TYPE_NAME,ManagingKey.class);

            List<ManagingKeyMapping> managingKeyMappingList =
                confDataDAO.selectAll(ManagingKeyMapping.TYPE_NAME,ManagingKeyMapping.class);

            List<Person> personList =
                confDataDAO.selectAll(Person.TYPE_NAME,Person.class);

            List<ThresholdConfig> thresholdConfigList =
                confDataDAO.selectAll(ThresholdConfig.TYPE_NAME, ThresholdConfig.class);

            List<ThresholdContextAttr> thresholdContextAttrList =
                confDataDAO.selectAll(ThresholdContextAttr.TYPE_NAME,ThresholdContextAttr.class);

            List<ThresholdQualifyingAttr> thresholdQualifyingAttrList =
                confDataDAO.selectAll(ThresholdQualifyingAttr.TYPE_NAME,ThresholdQualifyingAttr.class);


            
            // the order here is important, since objects depend on each other
            // for each one-to-many relationship, the 'one' object needs to precede the 'many' object
            updateConfigMap(personList,persons,Person.TYPE_NAME);
            updateConfigMap(managingKeyList,managingKeys,ManagingKey.TYPE_NAME);
            updateConfigMap(notifGroupList,notifGroups,NotifGroup.TYPE_NAME);
            updateConfigMap(alertingConfigList,alertingConfigs,AlertingConfig.TYPE_NAME);
            updateConfigMap(thresholdConfigList,thresholdConfigs,ThresholdConfig.TYPE_NAME);
            updateConfigMap(thresholdContextAttrList,thresholdContextAttrs,ThresholdContextAttr.TYPE_NAME);
            updateConfigMap(thresholdQualifyingAttrList,thresholdQualifyingAttrs,ThresholdQualifyingAttr.TYPE_NAME);
            updateConfigMap(managingKeyMappingList,managingKeyMappings,ManagingKeyMapping.TYPE_NAME);
            updateConfigMap(notifConfigList,notifConfigs,NotifConfig.TYPE_NAME);
            updateConfigMap(notifMappingList,notifMappings,NotifMapping.TYPE_NAME);
            updateConfigMap(notifGroupMappingList,notifGroupMappings,NotifGroupMapping.TYPE_NAME);

            // for stateful configurable objects
            postUpdateConfigMap(thresholdConfigs,ThresholdConfig.TYPE_NAME);
        }
        catch(ConfDataDAOException sysmDAOEx) {
            log.warn(sysmDAOEx,"Got ConfDataDAOException retrieving alert config data");
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx,"Got RuntimeException retrieving alert config data");
        }
    }
    
    private <T extends ConfigurableObject> void updateConfigMap(List<T> newConfigList,Map<Long,T> currConfigMap,String typeName) {
        
        if(newConfigList == null) {
            log.info("Retrieved no available instances of '" + typeName + "'");
            
            Set<Long> keys = currConfigMap.keySet();
            Iterator<Long> iter = keys.iterator();
            while(iter.hasNext()) {
                Long key = iter.next();
                T config = currConfigMap.get(key);
                
                log.info("Removing previously configured entry of '" + typeName + "': " + key);
                iter.remove();
                
                if(!config.unconfigure(this,alertManager)) {
                    log.warn("Failed to unconfigure removed instance of '" + typeName + "': " + key);
                }
            }
        }
        else {
            log.info("Retrieved list of " + newConfigList.size() + " instances of '" + typeName + "'");
                
            // build a keyed list
        	Set<Long> newKeys = new HashSet<Long>();
        	for(T newConfig:newConfigList) {
        	    if(newConfig.isValid(this)) {
        	        newKeys.add(newConfig.getId());
        	    }
        	    else {
        	        log.info("Not updating invalid instance of '" + typeName + "': " + newConfig.getLabel());
        	    }
        	}
        
        	// get list of curr keys
        	Set<Long> currKeys = currConfigMap.keySet();
        	Iterator<Long> currKeyIter = currKeys.iterator();
        
        	// throw out any that are no longer apparently active/valid
        	while(currKeyIter.hasNext()) {
            	Long currKey = currKeyIter.next();
            	T currConfig = currConfigMap.get(currKey);
            	if(!newKeys.contains(currKey)) {
            	    log.info("Removing previously configured instance of '" + typeName + "': " + currKey);
                	currKeyIter.remove();
                	
                	if(!currConfig.unconfigure(this,alertManager)) {
                	    log.warn("Failed to unconfigure removed instance of '" + typeName + "': " + currKey);
                	}
            	}
        	}
        
        	// now add or update the configs
        	for(T newConfig:newConfigList) {
        	    if(newKeys.contains(newConfig.getId())) {
        	        if(currConfigMap.containsKey(newConfig.getId())) {
        	        	// update entry, in case it has changed
        	        	T currConfig = currConfigMap.get(newConfig.getId());
        	        	if(!currConfig.update(this,alertManager,newConfig)) {
        	        	    log.warn("Failed to update current instance of '" + typeName + "', removing: " + newConfig.getId());
        	        	    currConfigMap.remove(currConfig.getId());
                	
        	        	    if(!currConfig.unconfigure(this,alertManager)) {
                	    		log.warn("Failed to unconfigure removed instance of '" + typeName + "': " + newConfig.getId());
                			}
        	        	}
        	    	}
        	    	else {
        	        	if(newConfig.configure(this,alertManager,loggingManager)) {
        	        	    log.info("Adding new instance of '" + typeName + "': " + newConfig.getId());
        	        		currConfigMap.put(newConfig.getId(), newConfig);
        	        	}
        	        	else {
        	        	    newConfig.unconfigure(this,alertManager);
        	        	    log.warn("Failed to configure new instance of '" + typeName + "': " + newConfig.getId());
        	        	}
        	    	}
        	    }
        	}
        }
    }

    private <T extends StatefulConfigurableObject> void postUpdateConfigMap(Map<Long,T> currConfigMap,String typeName) {

        log.info("Doing postUpdateConfig for instances of '" + typeName + "'");
        for(T config:currConfigMap.values()) {
            if(!config.postUpdateConfigure(this,alertManager)) {
                log.warn("Failed postUpdateConfigure step for instance of '" + typeName + "': " + config.getId());
            }
        }
    }
}
