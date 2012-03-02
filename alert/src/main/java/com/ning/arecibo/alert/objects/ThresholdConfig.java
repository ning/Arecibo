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

package com.ning.arecibo.alert.objects;

import static com.ning.arecibo.alert.client.AlertActivationStatus.ERROR;
import static com.ning.arecibo.alert.client.AlertActivationStatus.NORMAL;
import static com.ning.arecibo.alert.manage.AlertActivationType.ON_STALE_TO_FRESH;
import static com.ning.arecibo.alert.manage.AlertFreshnessStatus.FRESH;
import static com.ning.arecibo.alert.manage.AlertFreshnessStatus.STALE;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.ning.arecibo.alert.client.AlertActivationStatus;
import com.ning.arecibo.alert.client.AlertStatus;
import com.ning.arecibo.alert.client.AlertType;
import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.confdata.enums.ManagingKeyActionType;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdConfig;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertActivationType;
import com.ning.arecibo.alert.manage.AlertFreshnessStatus;
import com.ning.arecibo.alert.manage.AlertManager;
import com.ning.arecibo.event.BatchedEvent;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;

/*
**  Note: Synchronization is used only where necessary.  Want to prevent unnecessary locking
**  between event handling threads and configuration manager updates.
**  Use principle that updates to instance variables are synchronized.
**  Reading volatile instance variables not synchronized, unless it involves reading multiple interdependent values.
**
**  Updating the ConcurrentSkipListSet's for storing attributes does not need to be synchronized, since
**  any altered state will be removed within the next config update cycle, which always unregisters
**  and re-registers active configs via the alert manager (which calls back to remove any active configs).
 */

public class ThresholdConfig extends ConfDataThresholdConfig implements StatefulConfigurableObject
{
    private final static Logger log = Logger.getLogger(ThresholdConfig.class);
    
    // TODO: this list assumes specific knowledge about the events in the system
    // perhaps this should be specified as configuration params for the email notification
    private final static String[] eventKeysToIncludeInEmail = {
                                                  "timestamp",
                                                  "hostName",
                                                  "deployedType",
                                                  "deployedConfigSubPath",
                                                  "deployedVersion",
                                                  "deployedEnv",
                                                  "pollingInterval"
                                          };
    
    private final static AlertActivationType ACTIVATION_TYPE = ON_STALE_TO_FRESH;
 
    private final ConcurrentSkipListSet<ThresholdContextAttr> contextAttributes;
    private final ConcurrentSkipListSet<ThresholdQualifyingAttr> qualifyingAttributes;
    private final ConcurrentHashMap<String,_ActiveThresholdContext> activeThresholdContexts;
    private final ScheduledThreadPoolExecutor repeatNotificationExecutor;

    private volatile LoggingManager loggingManager = null;
    private volatile AlertingConfig alertingConfig = null;
    private volatile String lastAggQuery = null;
    private volatile String lastContextAttributeSignature = null;
    private volatile LastConfigAction lastConfigAction;
    private volatile ManagingKeyActionType managingAction = ManagingKeyActionType.NO_ACTION;

    public ThresholdConfig() {
        this.contextAttributes = new ConcurrentSkipListSet<ThresholdContextAttr>(ConfigurableObjectComparator.getInstance());
        this.qualifyingAttributes = new ConcurrentSkipListSet<ThresholdQualifyingAttr>(ConfigurableObjectComparator.getInstance());
        this.activeThresholdContexts = new ConcurrentHashMap<String,_ActiveThresholdContext>();
        this.repeatNotificationExecutor = new ScheduledThreadPoolExecutor(10); //TODO: should inject the num threads here
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        super.toStringBuilder(sb);

        if (qualifyingAttributes.size() > 0) {
            sb.append("    linked thresholdQualifyingAttr ids:\n");
            for (ThresholdQualifyingAttr qualifyingAttr : qualifyingAttributes) {
                sb.append(String.format("        %s\n", qualifyingAttr.getLabel()));
            }
        }

        if (contextAttributes.size() > 0) {
            sb.append("    linked thresholdContextAttr ids:\n");
            for (ThresholdContextAttr contextAttr : contextAttributes) {
                sb.append(String.format("        %s\n", contextAttr.getLabel()));
            }
        }
    }

    @Override
    public synchronized boolean isValid(ConfigManager confManager) {
        // make sure our alerting config exists in the conf, and is valid
        if (this.alertingConfigId == null ||
                !ConfigurableObjectUtils.checkNonNullAndValid(confManager.getAlertingConfig(this.alertingConfigId), confManager))
            return false;

        return true;
    }

    @Override
    public synchronized boolean configure(ConfigManager confManager, AlertManager alertManager,LoggingManager loggingManager) {

        this.loggingManager = loggingManager;

        // if this method returns false, the ConfigManager will call unconfigure immediately, to reset the 'LastConfigAction' flag
        
        this.alertingConfig = confManager.getAlertingConfig(this.alertingConfigId);
        if(this.alertingConfigId == null ||
                !ConfigurableObjectUtils.checkNonNullAndLog(this.alertingConfig,this.alertingConfigId,"alertConfig",confManager))
            return false;

        // make sure alert is still valid
        if (!this.alertingConfig.isValid(confManager)) {
            log.warn("Related alertingConfig is not valid, not configuring");
            return false;
        }

        this.alertingConfig.addThresholdConfig(this);

        this.lastConfigAction = LastConfigAction.CONFIGURE;

        return true;
    }

    @Override
    public synchronized boolean unconfigure(ConfigManager confManager, AlertManager alertManager) {

        if (this.alertingConfig != null) {
            this.alertingConfig.removeThresholdConfig(this);
            this.alertingConfig = null;
        }

        for (ThresholdQualifyingAttr qualifyingAttribute : qualifyingAttributes) {
            qualifyingAttribute.unconfigure(confManager, alertManager);
        }

        for (ThresholdContextAttr contextAttribute : contextAttributes) {
            contextAttribute.unconfigure(confManager, alertManager);
        }

        return alertManager.unregisterThresholdConfig(this);
    }

    @Override
    public synchronized boolean update(ConfigManager confManager, AlertManager alertManager, ConfigurableObject newConfig) {

        // note, a threshold config can have it's alertingConfig changed (unlike most other object references, so need to handle)...
        // a mapping between threshold and alerting configs probably doesn't make sense, since should only allow 1 alerting config
        // for a threshold config, etc...(but not out of the question)

        // if this method returns false, the ConfigManager will call unconfigure immediately, to reset the 'LastConfigAction' flag

        ThresholdConfig updateConfig = (ThresholdConfig) newConfig;
        
        // see if alertingConfigId is changing, and we need to cleanup existing
        boolean needToRemoveCurrent = false;
        boolean needToAddNew = false;
        if (this.alertingConfigId == null) {
            if(updateConfig.alertingConfigId != null) {
                needToAddNew = true;
            }
        }
        else if(updateConfig.alertingConfigId == null) {
            needToRemoveCurrent = true;
        }
        else if(!this.alertingConfigId.equals(updateConfig.alertingConfigId)) {
            needToRemoveCurrent = true;
            needToAddNew = true;
        }

        if(needToRemoveCurrent) {
            this.alertingConfig.removeThresholdConfig(this);
            this.alertingConfig = null;
        }

        this.alertingConfigId = updateConfig.alertingConfigId;

        if(needToAddNew) {
            this.alertingConfig = confManager.getAlertingConfig(this.alertingConfigId);

            // check that our alertinf config exists and is valid
            if(!ConfigurableObjectUtils.checkNonNullAndLog(this.alertingConfig,this.alertingConfigId,"alertConfig",confManager))
                return false;

            this.alertingConfig.addThresholdConfig(this);
        }

        // make sure alertingConfig is still valid
        if (!ConfigurableObjectUtils.checkNonNullAndValid(confManager.getAlertingConfig(this.alertingConfigId), confManager)) {
            log.warn("Related alertingConfig is not valid, not updating: %s",this.alertingConfigId);
            return false;
        }

        this.lastConfigAction = LastConfigAction.UPDATE;

        return ConfigurableObjectUtils.updateConfigurableObject((ConfDataObject)this,(ConfDataObject)newConfig);
    }

    @Override
    public synchronized boolean postUpdateConfigure(ConfigManager confManager, AlertManager alertManager) {

        try {
            if(lastConfigAction == null)
                return true;
            else if(lastConfigAction.equals(LastConfigAction.CONFIGURE)) {
                managingAction = checkCurrentManagingKeyAction(confManager);

                if(managingAction.getLevel() < ManagingKeyActionType.DISABLE.getLevel()) {

                    if(alertManager.registerThresholdConfig(this)) {
                        // see if we have any previously active alert incidents we need to instantiate now.
                        // note, since this is still happening under a synchronized lock for this config,
                        // this will prevent any race conditions between the just registered threshold config
                        // with the alert manager, and the instantiation here
                        List<AlertIncidentLog> aiLogs = confManager.checkActiveThresholdAlertIncidents(this.getId());
                        if(aiLogs != null) {
                            for(AlertIncidentLog aiLog:aiLogs) {
                                alertManager.propagatePreExistingThresholdEvent(this.getId(),aiLog);
                            }
                        }
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                else {
                    log.info("managingAction 'DISABLE' in effect for 'ThresholdConfig' " + id);
                    return true;
                }
            }
            else if(lastConfigAction.equals(LastConfigAction.UPDATE)) {

                ManagingKeyActionType newManagingAction = checkCurrentManagingKeyAction(confManager);
                boolean managingActionChanged = !managingAction.equals(newManagingAction);

                if(managingActionChanged) {
                    log.info("managingAction for 'ThresholdConfig' " + id + " changed from " + managingAction + " to " + newManagingAction);
                    managingAction = newManagingAction;
                }

                if(managingAction.equals(ManagingKeyActionType.DISABLE)) {
                    if(managingActionChanged) {
                        return alertManager.unregisterThresholdConfig(this);
                    }
                    else
                        return true;
                }
                else {
                    // need to see if the agg query would be updated (either by values here, or other object changes)
                    String newAggQuery = this.getAggregatorQuery(false);
                    boolean queryUpdated = this.lastAggQuery != null && newAggQuery != null && !newAggQuery.equals(this.lastAggQuery);

                    // also need to check if contextAttributeSignature has changed
                    // if so, we need to force to force an unregister, to clean up
                    // any saved state with the previous contextAttributeSignature
                    // (contextIdentifiers depend on the current contextAttributeSignature).
                    String newContextAttributeSignature = this.getContextAttributeSignature();
                    boolean contextSignatureUpdated = this.lastContextAttributeSignature != null &&
                                                        newContextAttributeSignature != null &&
                                                        !newContextAttributeSignature.equals(this.lastContextAttributeSignature);

                    if (queryUpdated || contextSignatureUpdated || managingActionChanged) {
                        // unregister, then register again on changed params
                        alertManager.unregisterThresholdConfig(this);
                        return alertManager.registerThresholdConfig(this);
                    }
                    else {
                        // reRegister current query by default, just in case it got lost with an aggregator outage
                        return alertManager.reRegisterThresholdConfig(this);
                    }
                }
            }
            else {
                // shouldn't get here
                return false;
            }
        }
        finally {
            this.lastConfigAction = null;
        }
    }

    public AlertActivationType getAlertActivationType() {
        return ACTIVATION_TYPE;
    }

    // no synchronization needed
    public void addThresholdContextAttr(ThresholdContextAttr contextAttribute) {
        if(!this.contextAttributes.contains(contextAttribute))
            this.contextAttributes.add(contextAttribute);
    }

    // no synchronization needed
    public void removeThresholdContextAttr(ThresholdContextAttr contextAttribute) {
        this.contextAttributes.remove(contextAttribute);
    }

    // no synchronization needed
    public void addThresholdQualifyingAttr(ThresholdQualifyingAttr qualifyingAttribute) {
        if(!this.qualifyingAttributes.contains(qualifyingAttribute))
            this.qualifyingAttributes.add(qualifyingAttribute);
    }

    // no synchronization needed
    public void removeThresholdQualifyingAttr(ThresholdQualifyingAttr qualifyingAttribute) {
        this.qualifyingAttributes.remove(qualifyingAttribute);
    }

    // no synchronization needed
    public Set<ThresholdQualifyingAttr> getThresholdQualifyingAttrs() {
        return this.qualifyingAttributes;
    }

    // no synchronization needed
    public Long getFreshnessIntervalMs() {
    	return this.clearingIntervalMs;
    }

    // no synchronization needed
    public AlertFreshnessStatus getAlertFreshnessStatus(Event evt) {
        _ActiveThresholdContext atc = this.activeThresholdContexts.get(getContextIdentifier(evt));
        AlertFreshnessStatus status = (atc == null) ? null : atc.getAlertFreshnessStatus();
        
        // we only store the fresh ones for threshold alerts
        if(status == null)
            return STALE;
        else
            return status;
    }

    public synchronized void setAlertFreshnessStatus(AlertFreshnessStatus status) {

        // update all
        for(String contextIdentifier:this.activeThresholdContexts.keySet()) {
            setAlertFreshnessStatus(status,contextIdentifier);
        }
    }

    public synchronized void setAlertFreshnessStatus(AlertFreshnessStatus status,String contextIdentifier) {

        log.debug("Setting status = " + status + ", for contextIdentifier = " + contextIdentifier);
        _ActiveThresholdContext atc = this.activeThresholdContexts.get(contextIdentifier);

        // we only store the fresh ones for threshold alerts
        if(status == FRESH) {
            if(atc == null) {
                atc = new _ActiveThresholdContext(contextIdentifier);
                this.activeThresholdContexts.put(contextIdentifier,atc);
            }
            atc.setAlertFreshnessStatus(status);
        }
        else {
            if(atc != null) {
                atc.setAlertFreshnessStatus(null);
                atc.setLastEventValue(null);
                atc.setSampleWindowQueue(null);

                if(atc.isEmpty()) {
                    this.activeThresholdContexts.remove(contextIdentifier);
                }
            }
        }
    }

    public synchronized void updateAlertActivationStatus(AlertActivationStatus status,boolean suppressNotification) {
        for(String contextIdentifier:this.activeThresholdContexts.keySet()) {
            updateAlertActivationStatus(status,null,contextIdentifier,null,suppressNotification);
        }
    }

    public synchronized void updateAlertActivationStatus(AlertActivationStatus status,String contextIdentifier) {
        updateAlertActivationStatus(status,contextIdentifier,false);
    }

    public synchronized void updateAlertActivationStatus(AlertActivationStatus status,String contextIdentifier,boolean suppressNotification) {
        updateAlertActivationStatus(status,null,contextIdentifier,null,suppressNotification);
    }

    public synchronized void updateAlertActivationStatus(AlertActivationStatus status,AlertIncidentLog aiLog,boolean suppressNotification) {
        updateAlertActivationStatus(status,null,aiLog.getContextIdentifier(),aiLog,suppressNotification);
    }

    public synchronized void updateAlertActivationStatus(AlertActivationStatus status,Event evt,String contextIdentifier) {
        updateAlertActivationStatus(status,evt,contextIdentifier,null,false);
    }

    public synchronized void scheduleRepeatNotification(final Event evt,final String contextIdentifier) {

        AlertingConfig localAlertingConfig = this.alertingConfig;
        Long notifRepeatIntervalMs = null;

        if(localAlertingConfig != null && (notifRepeatIntervalMs = localAlertingConfig.getNotifRepeatIntervalMsIfEnabled()) != null) {

            repeatNotificationExecutor.schedule(new Runnable() {

                public void run() {

                    synchronized(ThresholdConfig.this) {
                        // check status
                        AlertActivationStatus status = getAlertActivationStatus(evt);
                        if(status.equals(ERROR)) {
                            sendAlertNotification(status,evt);
                            scheduleRepeatNotification(evt,contextIdentifier);
                        }
                    }
                }
                
            },notifRepeatIntervalMs,TimeUnit.MILLISECONDS);
        }
    }
    
    public synchronized String getAggregatorQuery() {
        return getAggregatorQuery(true);
    }

    public synchronized boolean checkMinThresholdSamplesReached(Event evt) {

        if(this.minThresholdSamples == null || this.minThresholdSamples <= 1 ||
                this.maxSampleWindowMs == null || this.maxSampleWindowMs <= 0)
            return true;

        String contextIdentifier = getContextIdentifier(evt);

        _ActiveThresholdContext atc = this.activeThresholdContexts.get(contextIdentifier);

        ConcurrentLinkedQueue<Event> queue = (atc == null) ? null : atc.getSampleWindowQueue();
        if(queue == null) {
            // start a new queue
            queue = new ConcurrentLinkedQueue<Event>();

            if(atc == null) {
                atc = new _ActiveThresholdContext(contextIdentifier);
                this.activeThresholdContexts.put(contextIdentifier,atc);
            }
            atc.setSampleWindowQueue(queue);
        }
        queue.add(evt);

        // remove all entries older than the sample window
        // assume the events are in chronological sorted order! stop when find one within sample window
        long sampleWindowStartMillis = System.currentTimeMillis() - this.maxSampleWindowMs;
        while(queue.size() > 0) {

            Event headEvt = queue.peek();
            if(headEvt == null || headEvt.getTimestamp() > sampleWindowStartMillis)
                break;

            // remove it
            queue.poll();
        }

        if(queue.size() >= this.minThresholdSamples)
            return true;
        else
            return false;
    }

    // no synchronization needed
    public Long getTimeInCurrentActivationStatus(String contextIdentifier) {
        _ActiveThresholdContext atc = this.activeThresholdContexts.get(contextIdentifier);
        Long currentActivationStartTime = (atc == null) ? null : atc.getCurrentActivationStatusStartTime();

        if(currentActivationStartTime == null)
            return null;

        return System.currentTimeMillis() - currentActivationStartTime;
    }

    // no synchronization needed
    public Object getLastAlertValue(String contextIdentifier) {
        _ActiveThresholdContext atc = this.activeThresholdContexts.get(contextIdentifier);
        return (atc == null) ? null : atc.getLastEventValue();
    }

    // no synchronization needed
    public String getAggregatorName() {
        return "Threshold_Alert_" + id;
    }

    // no synchronization needed
    public String getAggregatorEventType() {
        return this.monitoredEventType;
    }

    public synchronized String getContextIdentifier(Event evt) {

        final Long localId = this.id;
        if(localId == null)
            // shouldn't happen
            return null;

        final String idString = localId.toString();
        
        if(contextAttributes.size() == 0)
            return idString;
        
        Map<String,Object> evtMap = getEventMap(evt);
        if(evtMap == null)
            return idString;
            
        ArrayList<String> matchList = null;
        
        for(ThresholdContextAttr contextAttribute:contextAttributes) {
            if(evtMap.containsKey(contextAttribute.getAttributeType())) {
               if(matchList == null)
                   matchList = new ArrayList<String>();
               matchList.add(contextAttribute.getAttributeType());
            }
        }
        
        // if no contextAttrs, then return the default
        if(matchList == null || matchList.size() == 0) {
            return idString;
        }
        
        // make sure we have guaranteed ordering
        Collections.sort(matchList);
        
        StringBuilder sb = new StringBuilder();
        sb.append(idString);
        
        for(String matchKey:matchList) {
            Object value = evtMap.get(matchKey);
            
            sb.append("&");
            sb.append(matchKey);
            sb.append("=");
            sb.append(value.toString());
        }
        
        return sb.toString();
    }


    /*
    ** public static method, which synchronizes on a per config basis
     */
    public static List<AlertStatus> getAlertStatus(Set<Long> thresholdIds,ConfigManager confManager,AlertActivationStatus activationStatusFilter) {

        ArrayList<AlertStatus> retList = new ArrayList<AlertStatus>();

        for(Long thresholdConfigId:thresholdIds) {

            ThresholdConfig thresholdConfig = confManager.getThresholdConfig(thresholdConfigId);

            if (thresholdConfig == null) {
                // could happen due to concurrency
                continue;
            }

            // synchronize per config
            synchronized(thresholdConfig) {

                Map<String, AlertActivationStatus> statusMap = thresholdConfig.getAlertActivationStatusMap(activationStatusFilter);

                if (statusMap != null && statusMap.size() > 0) {

                    Set<String> statusKeys = statusMap.keySet();
                    for (String statusKey : statusKeys) {

                        AlertStatus alertStatus = new AlertStatus(statusKey,
                                AlertType.THRESHOLD,
                                statusMap.get(statusKey),
                                thresholdConfig.getMonitoredEventType(),
                                thresholdConfig.getMonitoredAttributeType());

                        Object lastValue = thresholdConfig.getLastAlertValue(statusKey);
                        if (lastValue != null)
                            alertStatus.addAuxAttribute(thresholdConfig.getMonitoredAttributeType(), lastValue.toString());

                        Set<ThresholdQualifyingAttr> qualifiers = thresholdConfig.getThresholdQualifyingAttrs();

                        if (qualifiers != null) {
                            for (ThresholdQualifyingAttr qualifier : qualifiers) {
                                alertStatus.addAuxAttribute(qualifier.getAttributeType(), qualifier.getAttributeValue());
                            }
                        }

                        Map<String, String> instanceAttributes = thresholdConfig.getAttributesFromContextIdentifier(statusKey);
                        if (instanceAttributes != null) {
                            Set<String> instanceAtts = instanceAttributes.keySet();
                            for (String instanceAtt : instanceAtts) {
                                alertStatus.addAuxAttribute(instanceAtt, instanceAttributes.get(instanceAtt));
                            }
                        }

                        alertStatus.addAuxAttribute("thresholdConfigId", thresholdConfig.getLabel());
                        alertStatus.addAuxAttribute("shortDescription", thresholdConfig.getShortDescription());

                        Long timeInAlert = thresholdConfig.getTimeInCurrentActivationStatus(statusKey);
                        if (timeInAlert != null)
                            alertStatus.addAuxAttribute("timeInAlert", timeInAlert.toString());

                        retList.add(alertStatus);
                    }
                }
            }
        }

        return retList;
    }




    /*
    ** Private methods
    **  for these, needed synchronization is assumed from the caller
     */
    private ManagingKeyActionType checkCurrentManagingKeyAction(ConfigManager confManager) {

        if(this.alertingConfig == null)
            return ManagingKeyActionType.NO_ACTION;

        List<ManagingKey> managingKeys = this.alertingConfig.getCurrentManagingKeys(confManager);

        ManagingKeyActionType action = ManagingKey.getAction(managingKeys);
        return action;
    }

    private String getShortDescription() {

        if(this.maxThresholdValue != null && this.minThresholdValue != null) {
            return String.format("Require > %s and < %s",this.minThresholdValue,this.maxThresholdValue);
        }
        else if(this.maxThresholdValue != null) {
            return String.format("Require < %s",this.maxThresholdValue);
        }
        else {
            return String.format("Require > %s",this.minThresholdValue);
        }
    }

    private Map<String,String> getAttributesFromContextIdentifier(String contextIdentifier) {

        Map<String,String> retMap = new HashMap<String,String>();

        StringTokenizer st = new StringTokenizer(contextIdentifier,"&=");

        if(st.hasMoreTokens()) {
            String id = st.nextToken();
            retMap.put("thresholdConfigId", id);
        }

        while(st.hasMoreTokens()) {
            String attribute = st.nextToken();
            if(st.hasMoreTokens()) {
                String value = st.nextToken();
                retMap.put(attribute,value);
            }
        }

        return retMap;
    }

    private Map<String,Object> getEventMap(Event evt) {

        // assume it's always a BatchEvent
        // (can change later if this assumption no longer valid)
        if(evt != null && evt instanceof BatchedEvent) {
            // assume there's only ever 1 event in the batch
            BatchedEvent batchedEvt = (BatchedEvent)evt;
            for(Event subEvt:batchedEvt.getEvents()) {

                if(subEvt instanceof MapEvent) {
                    MapEvent mEvt = (MapEvent)subEvt;

                    Map<String,Object> mEvtMap = mEvt.getMap();

                    return mEvtMap;
                }
            }
        }
        return null;
    }

    private Object getEventValue(Event evt,String key) {
        Map<String,Object> map = getEventMap(evt);
        if(map == null)
            return null;

        return map.get(key);
    }

    private String getAggregatorQuery(boolean updateLastAggQuery) {

        String event = getAggregatorEventType();
        String thresholdAttribute = this.getMonitoredAttributeType();

        String thresholdString = null;
        if(minThresholdValue != null && maxThresholdValue != null)
            thresholdString = "(" + thresholdAttribute + " <= " + minThresholdValue + " or " + thresholdAttribute + " >= " + maxThresholdValue + ")";
        else if(minThresholdValue != null)
            thresholdString = thresholdAttribute + " <= " + minThresholdValue;
        else if(maxThresholdValue != null)
            thresholdString = thresholdAttribute + " >= " + maxThresholdValue;

        String qualifierString = getQualifyingAttributeQueryClause(" and ","");

        Long clearingIntervalSec = clearingIntervalMs / (1000L);

        String aggQuery = " select * from " + event + "(" + thresholdString + qualifierString + ").win:time(" + clearingIntervalSec + " sec)";

        if(updateLastAggQuery) {
            lastAggQuery = aggQuery;

            // do this for bookkeeping
            lastContextAttributeSignature = getContextAttributeSignature();
        }

        return aggQuery;
    }

    private String getQualifyingAttributeQueryClause(String prependString,String appendString) {

        StringBuilder sb = new StringBuilder("");
        boolean foundOne = false;
        for(ThresholdQualifyingAttr qualifyingAttribute:qualifyingAttributes) {
            if(foundOne)
                sb.append(" and ");
            else
                foundOne = true;

            sb.append(String.format("%s = '%s'",qualifyingAttribute.getAttributeType(),qualifyingAttribute.getAttributeValue()));
        }

        if(foundOne) {
            sb.insert(0, prependString);
            sb.append(appendString);
        }

        return sb.toString();
    }

    private String getContextAttributeSignature() {

        StringBuilder sb = new StringBuilder("");
        for(ThresholdContextAttr contextAttribute:contextAttributes) {
            sb.append(String.format("%s%s",contextAttribute,":"));
        }

        return sb.toString();
    }

    private void sendAlertNotification(AlertActivationStatus status,Event evt) {
        sendAlertNotification(status,evt,null);
    }

    private void sendAlertNotification(AlertActivationStatus status,Event evt,String contextIdentifier) {

        if(this.alertingConfig == null)
            return;

        final Notification notification = getNotification(status,evt,contextIdentifier);

        if (notification != null) {
            // this will get sent asynchronously by the EmailManager, so no io blocking for this thread
            this.alertingConfig.sendNotification(notification);
        }
    }

    private Notification getNotification(AlertActivationStatus status, Event evt, String contextIdentifier) {

        String subject;
        String descString;

        if(evt != null && contextIdentifier == null)
            contextIdentifier = getContextIdentifier(evt);

        // prepare subject, and description string
        if(status == ERROR) {
            subject = "[Arecibo Alert]: " + this.getLabel();
            if(this.getMaxThresholdValue() != null && this.getMinThresholdValue() != null) {
                descString = "Threshold violated, value not inside range > " + this.getMinThresholdValue() + " and < " + this.getMaxThresholdValue();
            }
            else if(this.getMaxThresholdValue() != null) {
                descString = "Maximum threshold violated >= " + this.getMaxThresholdValue();
            }
            else {
                descString = "Minimum threshold violated <= " + this.getMinThresholdValue();
            }
        }
        else if(alertingConfig.getNotifOnRecovery()) {
            subject = "[Arecibo Alert Cleared]: " + this.getLabel();
            if(this.getMaxThresholdValue() != null && this.getMinThresholdValue() != null) {
                descString = "Value now within threshold range > " + this.getMinThresholdValue() + " and < " + this.getMaxThresholdValue();
            }
            else if(this.getMaxThresholdValue() != null) {
                descString = "Value now < " + this.getMaxThresholdValue();
            }
            else {
                descString = "Value now > " + this.getMinThresholdValue();
            }
        }
        else {
            log.info("Not sending recovery notification since notifOnRecovery not enabled, for " + contextIdentifier);
            return null;
        }

        // add contextString to subject
        String contextString = getContextStringFromIdentifier(contextIdentifier);
        subject += contextString;


        // build message body
        StringBuilder sb = new StringBuilder();

        // add selector info
        sb.append("\n");
        sb.append(String.format("EventType: %s\n",this.monitoredEventType));
        sb.append(String.format("AttributeType: %s\n",this.monitoredAttributeType));
        for(ThresholdQualifyingAttr qualifyingAttribute:qualifyingAttributes) {
            sb.append(String.format("%s: %s [qualifying_attribute]\n",qualifyingAttribute.getAttributeType(),qualifyingAttribute.getAttributeValue()));
        }
        sb.append("\n");


        // add status
        if(status == ERROR) {
            // use the lastEventValue
            sb.append(String.format("%s: %s\n",monitoredAttributeType,getLastAlertValue(contextIdentifier)));
        }
        else {
            sb.append(String.format("Clearing interval passed: %d secs\n",(this.getClearingIntervalMs() / 1000L)));
        }
        sb.append(String.format("%s\n",descString));

        // look for context info from the event, if available
        if(evt != null && evt instanceof BatchedEvent) {
            // assume there's only ever 1 event in the batch
            BatchedEvent batchedEvt = (BatchedEvent)evt;
            for(Event subEvt:batchedEvt.getEvents()) {

                if(subEvt instanceof MapEvent) {
                    MapEvent mEvt = (MapEvent)subEvt;

                    Map<String,Object> mEvtMap = mEvt.getMap();
                    Set<String> evtKeys = mEvtMap.keySet();

                    // print event keys to include
                    sb.append("\n");

                    for(String eventKey:eventKeysToIncludeInEmail) {
                        if(eventKey.equals("timestamp")) {
                            Timestamp ts = null;
                            if(status == NORMAL) {
                                // use current timestamp
                                ts = new Timestamp(System.currentTimeMillis());
                            }
                            else {
                                // need to get timestamp from base event, not subEvt
                                Long tsMillis = batchedEvt.getTimestamp();
                                if(tsMillis != null)
                                    ts = new Timestamp(tsMillis);
                            }

                            if(ts != null)
                                sb.append(String.format("%s: %s\n",eventKey,ts));
                        }
                        else {
                            Object eventVal = mEvtMap.get(eventKey);
                            if(eventVal != null)
                                sb.append(String.format("%s: %s\n",eventKey,eventVal.toString()));
                        }
                    }
                }
                else {
                    sb.append(String.format("Unexpected event type (expected MapEvent): %s",subEvt.getClass().getName()));
                }
            }
        }
        else if (evt != null){
            sb.append(String.format("Unexpected event type (expected BatchedEvent): %s",evt.getClass().getName()));
        }
        else if(contextString != null) { //evt == null
            sb.append("\n");

            // generate context items from contextString
            List<String> contextItems = getContextComponentsFromIdentifier(contextIdentifier);
            for(String contextItem:contextItems) {
                sb.append(contextItem);
            }
        }

        return new Notification(subject, sb.toString());
    }

    private String getContextStringFromIdentifier(String contextIdentifier) {
        List<String> list = getContextComponentsFromIdentifier(contextIdentifier);
        StringBuilder sb = new StringBuilder();

        for(String item:list) {
            sb.append(String.format(", %s",item));
        }

        return sb.toString();
    }

    private List<String> getContextComponentsFromIdentifier(String contextIdentifier) {
        
        if(contextIdentifier == null)
            return Collections.emptyList();

        List<String> list = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(contextIdentifier,"&=");

        // skip initial id
        if(st.hasMoreTokens())
            st.nextToken();

        // go through name value pairs
        while(st.hasMoreTokens()) {
            String attribute = st.nextToken();
            if(st.hasMoreTokens()) {
                String value = st.nextToken();

                list.add(String.format("%s: %s",attribute,value));
            }
        }

        return list;
    }

    private void updateAlertActivationStatus(AlertActivationStatus status,Event evt,String contextIdentifier,AlertIncidentLog aiLog,boolean suppressNotification) {

        if(contextIdentifier == null) {
            if(evt == null) {
                throw new IllegalStateException("Both contextIdentifier & evt were null in updateAlertActivationStatus");
            }

            contextIdentifier = getContextIdentifier(evt);
        }

        _ActiveThresholdContext atc = this.activeThresholdContexts.get(contextIdentifier);

        // we only store status==ERROR for threshold alerts
        AlertActivationStatus currStatus = (atc == null) ? null : atc.getAlertActivationStatus();
        if(currStatus == null) {
            currStatus = NORMAL;
        }

        boolean statusChanged;
        if(!status.equals(currStatus)) {
            statusChanged = true;
            log.info("Updating alert status for alert config " + contextIdentifier + " from " + currStatus + " to " + status);
        }
        else
            statusChanged = false;


        // update status and lastEventValueMap
        Object valueObj = null;
        if(status == ERROR) {

            if(atc == null) {
                atc = new _ActiveThresholdContext(contextIdentifier);
                this.activeThresholdContexts.put(contextIdentifier,atc);
            }

            if(evt != null) {
                valueObj = getEventValue(evt,this.monitoredAttributeType);
                if(valueObj != null) {
                    atc.setLastEventValue(valueObj);
                }
            }

            if(statusChanged) {
                atc.setAlertActivationStatus(status);
                atc.setCurrentActivationStatusStartTime(System.currentTimeMillis());

                // aiLog will usually be null here, except if passed in as part of a
                // re-initialization on startup
                if(aiLog == null) {
                    // create new log entry
                    aiLog = new AlertIncidentLog();
                    aiLog.setThresholdConfigId(this.getId());
                    aiLog.setContextIdentifier(contextIdentifier);
                    aiLog.setStartTime(new Timestamp(System.currentTimeMillis()));
                    aiLog.setShortDescription(this.getShortDescription());

                    if(valueObj != null && valueObj instanceof Number)
                        aiLog.setInitialAlertEventValue(((Number)valueObj).doubleValue());

                    aiLog = loggingManager.insertAlertIncidentLogEntry(aiLog);
                }

                atc.setAlertIncidentLog(aiLog);
            }
        }
        else if(statusChanged && atc != null) {

            // update existing entry
            aiLog = atc.getAlertIncidentLog();
            if(aiLog != null) {
                aiLog.setClearTime(new Timestamp(System.currentTimeMillis()));
                loggingManager.updateAlertIncidentLogEntry(aiLog);
            }

            atc.setAlertActivationStatus(null);
            atc.setCurrentActivationStatusStartTime(null);
            atc.setAlertIncidentLog(null);

            if(atc.isEmpty()) {
                this.activeThresholdContexts.remove(contextIdentifier);
            }
        }

        if(statusChanged && !suppressNotification && this.managingAction.getLevel() < ManagingKeyActionType.QUIESCE.getLevel()) {
            sendAlertNotification(status,evt,contextIdentifier);
            scheduleRepeatNotification(evt,contextIdentifier);
        }
    }

    private Map<String, AlertActivationStatus> getAlertActivationStatusMap(AlertActivationStatus status) {

        if(status == null)
            return null;

        Map<String, AlertActivationStatus> map = new HashMap<String, AlertActivationStatus>();
        for(_ActiveThresholdContext atc:this.activeThresholdContexts.values()) {
            if(atc.getAlertActivationStatus() != null && atc.getAlertActivationStatus().equals(status)) {
                map.put(atc.getContextIdentifier(),atc.getAlertActivationStatus());
            }
        }

        return map;
    }

    private AlertActivationStatus getAlertActivationStatus(Event evt) {
        _ActiveThresholdContext atc = this.activeThresholdContexts.get(getContextIdentifier(evt));

        AlertActivationStatus status = (atc == null) ? null : atc.getAlertActivationStatus();

        // we only store status==ERROR for threshold alerts
        if(status == null)
            return NORMAL;
        else
            return status;
    }




    /*
    ** a convenience class for maintaining freshness & activation state
    **   getters are allowed to be called outside synchronization
    **   setters should be synchronized (can be at the config level)
     */
    private class _ActiveThresholdContext {
        private final String contextIdentifier;
        private volatile AlertFreshnessStatus alertFreshnessStatus;
        private volatile AlertActivationStatus alertActivationStatus;
        private volatile Object lastEventValue;
        private volatile Long currentActivationStatusStartTime;
        private volatile ConcurrentLinkedQueue<Event> sampleWindowQueue;
        private volatile AlertIncidentLog aiLog;

        public _ActiveThresholdContext(String contextIdentifier) {
            this.contextIdentifier = contextIdentifier;
        }

        public String getContextIdentifier() {
            return this.contextIdentifier;
        }

        public AlertActivationStatus getAlertActivationStatus() {
            return this.alertActivationStatus;
        }

        public void setAlertActivationStatus(AlertActivationStatus alertActivationStatus) {
            this.alertActivationStatus = alertActivationStatus;
        }

        public AlertFreshnessStatus getAlertFreshnessStatus() {
            return this.alertFreshnessStatus;
        }

        public void setAlertFreshnessStatus(AlertFreshnessStatus alertFreshnessStatus) {
            this.alertFreshnessStatus = alertFreshnessStatus;
        }

        public Long getCurrentActivationStatusStartTime() {
            return this.currentActivationStatusStartTime;
        }

        public void setCurrentActivationStatusStartTime(Long currentActivationStatusStartTime) {
            this.currentActivationStatusStartTime = currentActivationStatusStartTime;
        }

        public Object getLastEventValue() {
            return this.lastEventValue;
        }

        public void setLastEventValue(Object lastEventValue) {
            this.lastEventValue = lastEventValue;
        }

        public ConcurrentLinkedQueue<Event> getSampleWindowQueue() {
            return this.sampleWindowQueue;
        }

        public void setSampleWindowQueue(ConcurrentLinkedQueue<Event> sampleWindowQueue) {
            this.sampleWindowQueue = sampleWindowQueue;
        }

        public AlertIncidentLog getAlertIncidentLog() {
            return this.aiLog;
        }

        public void setAlertIncidentLog(AlertIncidentLog aiLog) {
            this.aiLog = aiLog;
        }

        public boolean isEmpty() {
            if(this.alertFreshnessStatus == null &&
                    this.alertActivationStatus == null &&
                    this.lastEventValue == null &&
                    this.currentActivationStatusStartTime == null &&
                    this.sampleWindowQueue == null &&
                    this.aiLog == null)
                return true;
            else
                return false;
        }
    }
}
