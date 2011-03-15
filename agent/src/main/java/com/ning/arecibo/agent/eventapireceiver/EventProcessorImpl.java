package com.ning.arecibo.agent.eventapireceiver;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.UUID;
import java.util.Map;

import com.google.inject.Inject;
import com.ning.arecibo.agent.AgentDataCollectorManager;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.MonitoringEvent;
import com.ning.arecibo.event.receiver.EventProcessor;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.eventlogger.EventPublisher;

import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;

public class EventProcessorImpl implements EventProcessor
{
    private static final Logger log = Logger.getLogger(EventProcessorImpl.class);
    private final EventPublisher publisher;
    private final AgentDataCollectorManager agentDataCollectorManager;
    private final AtomicLong eventsProcessed = new AtomicLong(0);
    private final AtomicLong eventsFailed = new AtomicLong(0);

    @Inject
    public EventProcessorImpl(EventPublisher publisher,
                              AgentDataCollectorManager agentDataCollectorManager)
    {
        this.publisher = publisher;
        this.agentDataCollectorManager = agentDataCollectorManager;
    }

    public void processEvent(Event evt)
    {
        try {
            if (evt instanceof MapEvent) {
                // handle MapEvents by default
                MapEvent mapEvent = (MapEvent) evt;

                evt = new MonitoringEvent(mapEvent);
            }

            if(evt instanceof MonitoringEvent) {

                // do some ad hoc cleanup (as a hack, for known sources with certain fields)
                // TODO: Modularize this better:
                // 1. cleanup incoming data from dsp collector
                // 2. create alternate aggregation destination for different signatures
                evt = setReasonableDefaultsIfNecessary((MonitoringEvent)evt);
            }

            if(log.isDebugEnabled()) {
                log.debug("Publishing event api event: %s",evt.toString());
            }

            this.publisher.publish(evt);
            eventsProcessed.getAndIncrement();
        }
        catch (IOException e) {
            log.error(e);
            eventsFailed.getAndIncrement();
            throw new RuntimeException(e);
        }
        catch (RuntimeException e) {
            log.error(e);
            eventsFailed.getAndIncrement();
            throw new RuntimeException(e);
        }
    }

    private Event setReasonableDefaultsIfNecessary(MonitoringEvent monEvt) {

        boolean needToUpdate = false;

        UUID uuid = monEvt.getSourceUUID();
        String hostName = monEvt.getHostName();
        String deployedEnv = monEvt.getDeployedEnv();
        String deployedVersion = monEvt.getDeployedVersion();
        String deployedType = monEvt.getDeployedType();
        String deployedConfigSubPath = monEvt.getDeployedConfigSubPath();
        long timestamp = monEvt.getTimestamp();

        if(hostName == null || hostName.equals("")) {
            needToUpdate = true;
            hostName = "eventapi.local";
            uuid = null;
        }
        if(uuid == null) {
            needToUpdate = true;
            uuid = agentDataCollectorManager.getUuidForHost(hostName);
        }
        if(deployedEnv == null || deployedEnv.equals("")) {
            needToUpdate = true;
            deployedEnv = "unspecified";
        }
        if(deployedVersion == null || deployedVersion.equals("")) {
            needToUpdate = true;
            deployedVersion = "unspecified";
        }
        if(deployedType == null || deployedType.equals("")) {
            needToUpdate = true;
            deployedType = "eventapi";
        }
        if(deployedConfigSubPath == null || deployedConfigSubPath.equals("")) {
            needToUpdate = true;
            deployedConfigSubPath = "unspecified";
        }
        if(timestamp <= 0L) {
            needToUpdate = true;
            timestamp = System.currentTimeMillis();
        }

        if(!needToUpdate)
            return monEvt;
        else {
            String eventType = monEvt.getEventType();
            Map<String,Object> keyValues = monEvt.getMap();
            return new MonitoringEvent(timestamp,eventType,uuid,hostName,deployedEnv,deployedVersion,deployedType,deployedConfigSubPath,keyValues);
        }
    }

    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
    public long getEventsProcessed()
    {
        return eventsProcessed.get();
    }

    @MonitorableManaged(monitored = true)
    public long getEventsFailed()
    {
        return eventsFailed.get();
    }
}
