package com.ning.arecibo.aggregator.esper;

import java.util.concurrent.atomic.AtomicLong;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;

public class EsperStatsManager {

    private final AtomicLong numEventsSentToEsper;
    private final AtomicLong numEventsEmittedByEsper;
    private final AtomicLong numEventUpdatesEmittedByEsper;
    private final AtomicLong lastNumEventsSentToEsper;
    private final AtomicLong lastNumEventsEmittedByEsper;

    public EsperStatsManager() {
        numEventsSentToEsper = new AtomicLong(0L);
        numEventsEmittedByEsper = new AtomicLong(0L);
        numEventUpdatesEmittedByEsper = new AtomicLong(0L);
        lastNumEventsSentToEsper = new AtomicLong(0L);
        lastNumEventsEmittedByEsper = new AtomicLong(0L);
    }

    public void incrementNumEventsSentToEsper() {
        numEventsSentToEsper.getAndIncrement();
    }

    public void increaseNumEventsEmittedByEsper(long increase) {
        numEventsEmittedByEsper.getAndAdd(increase);
    }

    public void incrementNumEventUpdatesEmittedByEsper() {
        numEventUpdatesEmittedByEsper.getAndIncrement();
    }

    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
    public long getNumEventsSentToEsper() {
        return numEventsSentToEsper.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
    public long getNumEventsEmittedByEsper() {
        return numEventsEmittedByEsper.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
    public long getNumEventUpdatesEmittedByEsper() {
        return numEventUpdatesEmittedByEsper.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.VALUE })
    public double getRatioEventsSentVsEmitted() {

        long numSent = numEventsSentToEsper.get();
        long numEmitted = numEventsEmittedByEsper.get();

        // reset the last counts each time the method is called
        long currNumSent = numSent - lastNumEventsSentToEsper.getAndSet(numSent);
        long currNumEmitted = numEmitted - lastNumEventsEmittedByEsper.getAndSet(numEmitted);

        if(currNumEmitted == 0L)
            return 0.0;

        return (double)currNumSent/(double)currNumEmitted;
    }
}
