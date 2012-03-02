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

package com.ning.arecibo.alert.manage;

import static com.ning.arecibo.alert.client.AlertActivationStatus.ERROR;
import static com.ning.arecibo.alert.client.AlertActivationStatus.NORMAL;
import static com.ning.arecibo.alert.manage.AlertActivationType.ON_FRESH_TO_STALE;
import static com.ning.arecibo.alert.manage.AlertActivationType.ON_STALE_TO_FRESH;
import static com.ning.arecibo.alert.manage.AlertFreshnessStatus.FRESH;
import static com.ning.arecibo.alert.manage.AlertFreshnessStatus.STALE;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.google.inject.Inject;
import com.ning.arecibo.alert.client.AlertActivationStatus;
import com.ning.arecibo.alert.guice.AlertServiceConfig;
import com.ning.arecibo.alert.objects.AlertIncidentLog;
import com.ning.arecibo.alert.objects.ThresholdConfig;
import com.ning.arecibo.client.AggregatorService;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.lang.Aggregator;
import com.ning.arecibo.lang.ExternalPublisher;
import com.ning.arecibo.util.Logger;

/*
** This class contains management semantics for managing alert "activation" based on the "freshness" of recently handled
** events.  For example, threshold events enter "ERROR" activation on transition from "FRESH" to "STALE", and heartbeat
** alerts are the reverse.
** 
** (currently, the implementation of heartbeat alerts has been removed from the alert core, the dual semantics for "freshness"
** and "esclation" are retained here, for now).
 */

/*
** This class is synchronized on a per config basis
 */

public class AlertManager
{
    private final static Logger log = Logger.getLogger(AlertManager.class);
	
	//TODO: Inject these params
	private final static int THREAD_COUNT = 50;
	private final static String AGGREGATOR_NAMESPACE = "dynamic";
	
	private final AggregatorService aggService;
    private final ScheduledThreadPoolExecutor executor;
    private final ConcurrentHashMap<Long,ThresholdConfig> thresholdConfigs;
    private final ConcurrentHashMap<Long,ConcurrentHashMap<String,ScheduledFuture<?>>> schedFutures;
    private final AlertServiceConfig alertServiceConfig;

    @Inject
	public AlertManager(AggregatorService aggService,
	                    AlertServiceConfig alertServiceConfig) {
        this.alertServiceConfig = alertServiceConfig;
	    this.aggService = aggService;
        this.executor = new ScheduledThreadPoolExecutor(THREAD_COUNT);
        this.thresholdConfigs = new ConcurrentHashMap<Long,ThresholdConfig>();
        this.schedFutures = new ConcurrentHashMap<Long,ConcurrentHashMap<String,ScheduledFuture<?>>>();
	}

    public Set<Long> getActiveThresholdConfigIds() {
        return this.thresholdConfigs.keySet();
    }

	public boolean registerThresholdConfig(ThresholdConfig config) {

        synchronized(config) {
            log.info("Registering thresholdConfig: " + config.getId());

            this.thresholdConfigs.put(config.getId(), config);

            Aggregator agg = getAggregator(config);

            try {
                aggService.getAggregatorService().register(agg,
                                                           2 * alertServiceConfig.getConfigUpdateInterval().getPeriod(),
                                                           alertServiceConfig.getConfigUpdateInterval().getUnit());
                return true;
            }
            catch(Exception ex) {
                // could be RemoteException, ServiceNotAvailableException, or any RuntimeException
                log.warn(ex);
                return false;
            }
        }
	}
	
	public boolean reRegisterThresholdConfig(ThresholdConfig config) {

        synchronized(config) {
            log.info("ReRegistering thresholdConfig: " + config.getId());

            Aggregator agg = getAggregator(config);

            try {
                aggService.getAggregatorService().register(agg,
                                                           2 * alertServiceConfig.getConfigUpdateInterval().getPeriod(),
                                                           alertServiceConfig.getConfigUpdateInterval().getUnit());
                return true;
            }
            catch(Exception ex) {
                // could be RemoteException, ServiceNotAvailableException, or any RuntimeException
                log.warn(ex);
                return false;
            }
        }
	}
	
	public boolean unregisterThresholdConfig(ThresholdConfig config) {

        synchronized(config) {
            log.info("Unregistering thresholdConfig: " + config.getId());

            unscheduleAllFreshnessMonitors(config);
            thresholdConfigs.remove(config.getId());

            try {
                aggService.getAggregatorService().unregister(AGGREGATOR_NAMESPACE + "/" + config.getAggregatorName());
                return true;
            }
            catch(Exception ex) {
                // could be RemoteException, ServiceNotAvailableException, or any RuntimeException
                log.warn(ex);
                return false;
            }
        }
	}

    private Aggregator getAggregator(ThresholdConfig thresholdAlertConfig) {
        String name = thresholdAlertConfig.getAggregatorName();
        String query = thresholdAlertConfig.getAggregatorQuery();
        String eventType = thresholdAlertConfig.getAggregatorEventType();

        Aggregator agg = new Aggregator(AGGREGATOR_NAMESPACE,name,eventType)
                .setStatement(query)
                .setOutputEvent(name)
                .addOutputProcessor(new ExternalPublisher("AreciboAlertService"));

        return agg;
    }

	private void addScheduledFuture(ScheduledFuture<?> schedFuture, ThresholdConfig config,String contextIdentifier) {
	    ConcurrentHashMap<String,ScheduledFuture<?>> configSchedFutures = schedFutures.get(config.getId());
	    if(configSchedFutures == null) {
	        configSchedFutures = new ConcurrentHashMap<String,ScheduledFuture<?>>();
	        schedFutures.put(config.getId(),configSchedFutures);
	    }
	    
	    configSchedFutures.put(contextIdentifier,schedFuture);
	}
	
	private ScheduledFuture<?> getScheduledFuture(ThresholdConfig config,String contextIdentifier) {
	    ConcurrentHashMap<String,ScheduledFuture<?>> configSchedFutures = schedFutures.get(config.getId());
	    if(configSchedFutures == null)
	        return null;
	    
	    return configSchedFutures.get(contextIdentifier);
	}
	
	private void removeScheduledFuture(ThresholdConfig config,String contextIdentifier) {
	    ConcurrentHashMap<String,ScheduledFuture<?>> configSchedFutures = schedFutures.get(config.getId());
	    if(configSchedFutures == null)
	        return;
	    
	    configSchedFutures.remove(contextIdentifier);
	    if(configSchedFutures.size() == 0) {
	        schedFutures.remove(config.getId());
	    }
	}
	
	private void scheduleFreshnessMonitor(ThresholdConfig config,String contextIdentifier) {
		_FreshnessMonitor mon = new _FreshnessMonitor(this,config,contextIdentifier);
		ScheduledFuture<?> schedFuture = this.executor.schedule(mon, config.getFreshnessIntervalMs(), TimeUnit.MILLISECONDS);
		addScheduledFuture(schedFuture,config,contextIdentifier);
	}
	
	private void unscheduleFreshnessMonitor(ThresholdConfig config,String contextIdentifier) {
		
		ScheduledFuture<?> schedFuture = getScheduledFuture(config,contextIdentifier);
		if(schedFuture != null) {
			schedFuture.cancel(false);
			removeScheduledFuture(config,contextIdentifier);
		}
	}
	
	private void unscheduleAllFreshnessMonitors(ThresholdConfig config) {
		
		// remove all scheduled futures for this config
		ConcurrentHashMap<String,ScheduledFuture<?>> configSchedFutures = schedFutures.get(config.getId());
    	if(configSchedFutures == null)
        	return;
    	
        for(ScheduledFuture<?> schedFuture:configSchedFutures.values()) {
            schedFuture.cancel(false);
        }

        forceFreshnessStatusToStale(config);
    	
    	schedFutures.remove(config.getId());
	}

    private void forceFreshnessStatusToStale(ThresholdConfig config) {

        log.info("Forcing freshness status to stale for config: " + config.getId());

        AlertActivationStatus status;
        if (config.getAlertActivationType() == ON_FRESH_TO_STALE) {
            status = ERROR;
        }
        else {
            status = NORMAL;
        }

        log.info("Threshold config " + config.getId() + " forced transition to " + status + ", updating status: " + status);
        config.setAlertFreshnessStatus(STALE);
        config.updateAlertActivationStatus(status,true);
    }

    public void handleThresholdEvent(Long configId, Event evt) {

        ThresholdConfig config = thresholdConfigs.get(configId);
        if(config == null) {
            log.info("Ignoring received event for unknown configId: " + configId);
            return;
        }

        synchronized(config) {

            final String contextIdentifier = config.getContextIdentifier(evt);

            log.info("Handling threshold alert event for thresholdConfig: " + contextIdentifier);

            AlertActivationStatus status;
            if(config.getAlertActivationType() == ON_STALE_TO_FRESH) {
                status = ERROR;
            }
            else {
                status = NORMAL;
            }

            if(config.getAlertFreshnessStatus(evt) == STALE) {

                if(!config.checkMinThresholdSamplesReached(evt)) {
                    log.info("Not transitioning state for thresholdConfig, insufficient samples received: " + contextIdentifier);
                    return;
                }

                log.info("Threshold config " + contextIdentifier + " transitioned to fresh, updating status: " + status);
                config.setAlertFreshnessStatus(FRESH,contextIdentifier);
                config.updateAlertActivationStatus(status,evt,contextIdentifier);

                scheduleFreshnessMonitor(config,contextIdentifier);
            }
            else {
                config.updateAlertActivationStatus(status,evt,contextIdentifier);

                // unschedule so can reschedule with updated interval
                unscheduleFreshnessMonitor(config,contextIdentifier);
                scheduleFreshnessMonitor(config,contextIdentifier);
            }
        }
    }

    public void propagatePreExistingThresholdEvent(Long configId, AlertIncidentLog aiLog) {
        // simulate 'handleThresholdEvent()' method

		ThresholdConfig config = thresholdConfigs.get(configId);
		if(config == null) {
			log.info("Ignoring pre-existing threshold incident for unknown configId: " + configId);
			return;
		}
		
		synchronized(config) {

			log.info("Handling pre-existing threshold event for thresholdConfig: " + aiLog.getContextIdentifier());

            AlertActivationStatus status;
            if(config.getAlertActivationType() == ON_STALE_TO_FRESH) {
                status = ERROR;
            }
            else {
                status = NORMAL;
            }

			log.info("Threshold config " + aiLog.getContextIdentifier() + " propagated to fresh, updating status: " + status);
			config.setAlertFreshnessStatus(FRESH,aiLog.getContextIdentifier());
			config.updateAlertActivationStatus(status,aiLog,true);

            scheduleFreshnessMonitor(config,aiLog.getContextIdentifier());
		}
	}
	
    private class _FreshnessMonitor implements Runnable {
		private final AlertManager alertManager;
		private final ThresholdConfig config;
        private final String contextIdentifier;
		
		public _FreshnessMonitor(AlertManager alertManager, ThresholdConfig config,String contextIdentifier) {
		    this.alertManager = alertManager;
			this.config = config;
            this.contextIdentifier = contextIdentifier;
		}
		
		public void run() {

			try {
				synchronized(config) {

					removeScheduledFuture(config,contextIdentifier);
				
					AlertActivationStatus status;
					if(config.getAlertActivationType() == ON_FRESH_TO_STALE) {
						status = ERROR;
					}
					else {
						status = NORMAL;
					}
				
					log.info("Threshold config " + contextIdentifier + " transitioned to stale, updating status: " + status);
					config.setAlertFreshnessStatus(STALE,contextIdentifier);
					config.updateAlertActivationStatus(status,contextIdentifier);
				}
			}
			catch(RuntimeException ruEx) {
				// if anything unexexpected happens, don't want it to go unnoticed
				log.warn(ruEx,"Got RuntimeException in _FreshnessMonitor");
				log.info(ruEx);
			}
		}
	}
}
