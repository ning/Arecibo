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
