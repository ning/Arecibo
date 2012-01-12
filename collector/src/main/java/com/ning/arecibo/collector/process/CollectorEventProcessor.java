package com.ning.arecibo.collector.process;

import com.google.inject.Inject;
import com.ning.arecibo.collector.dao.CollectorDAO;
import com.ning.arecibo.event.BatchedEvent;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.receiver.EventProcessor;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.esper.MiniEsperEngine;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;

import java.util.concurrent.atomic.AtomicLong;

public class CollectorEventProcessor implements EventProcessor
{
    private final static Logger log = Logger.getLogger(CollectorEventProcessor.class);

    private final CollectorDAO collectorDAO;
    private final AtomicLong eventsReceived = new AtomicLong(0L);
    private final AtomicLong eventBatchesReceived = new AtomicLong(0L);
    private final AtomicLong invalidEventsDiscarded = new AtomicLong(0L);
    private final AtomicLong eventsDiscarded = new AtomicLong(0L);
    private final AtomicLong eventBatchesDiscarded = new AtomicLong(0L);

    private final MiniEsperEngine<Integer> eventsPerBatchStats;

    @Inject
    public CollectorEventProcessor(CollectorDAO collectorDAO)
    {
        this.collectorDAO = collectorDAO;
        this.eventsPerBatchStats = new MiniEsperEngine<Integer>("EventsPerBatchStats", Integer.class);
    }

    public void processEvent(Event evt)
    {
        //log.debug("Received Event : %s", evt);

        try {

            if (evt instanceof MapEvent) {
                if (!validateEvent((MapEvent) evt)) {
                    eventsReceived.getAndIncrement();
                    eventsPerBatchStats.send(1);
                    invalidEventsDiscarded.getAndIncrement();
                    eventsDiscarded.getAndIncrement();
                    eventBatchesDiscarded.getAndIncrement();
                    return;
                }
            }

            eventBatchesReceived.getAndIncrement();
            if (evt instanceof BatchedEvent) {
                int eventsPerBatch = ((BatchedEvent) evt).getEvents().size();
                eventsReceived.getAndAdd(eventsPerBatch);
                eventsPerBatchStats.send(eventsPerBatch);

                if (collectorDAO.getOkToProcessEvents()) {
                    collectorDAO.insertBuffered(((BatchedEvent) evt).getEvents());
                }
                else {
                    eventsDiscarded.getAndAdd(eventsPerBatch);
                    eventBatchesDiscarded.getAndIncrement();
                }
            }
            else {
                eventsReceived.getAndIncrement();
                eventsPerBatchStats.send(1);

                if (collectorDAO.getOkToProcessEvents()) {
                    collectorDAO.insertBuffered(evt);
                }
                else {
                    eventsDiscarded.getAndIncrement();
                    eventBatchesDiscarded.getAndIncrement();
                }
            }
        }
        catch (RuntimeException ruEx) {
            log.warn(ruEx);
        }
    }

    private boolean validateEvent(MapEvent mEvt)
    {
        // we don't want to insert MapEvents with no data
        Object datapointsObj = mEvt.getValue("datapoints");
        if (datapointsObj == null) {
            return false;
        }

        try {
            Number datapoints = (Number) datapointsObj;
            if (datapoints.intValue() > 0) {
                return true;
            }
            else {
                return false;
            }
        }
        catch (ClassCastException ccEx) {
            return false;
        }
    }

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventsReceived()
    {
        return eventsReceived.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventBatchesReceived()
    {
        return eventBatchesReceived.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventsDiscarded()
    {
        return eventsDiscarded.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getEventBatchesDiscarded()
    {
        return eventBatchesDiscarded.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = {MonitoringType.COUNTER, MonitoringType.RATE})
    public long getInvalidEventsDiscarded()
    {
        return invalidEventsDiscarded.get();
    }

    @MonitorableManaged(monitored = true)
    public double getEventsPerBatchReceived()
    {
        return eventsPerBatchStats.getAverage();
    }
}
