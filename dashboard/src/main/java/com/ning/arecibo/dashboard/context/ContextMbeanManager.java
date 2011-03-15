package com.ning.arecibo.dashboard.context;

import java.util.concurrent.atomic.AtomicLong;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;

public class ContextMbeanManager {
	
    private static AtomicLong dataRequestCount = new AtomicLong();
    private static AtomicLong dataRequestTotalMs = new AtomicLong(); 
    private static AtomicLong graphRequestCount = new AtomicLong();
    private static AtomicLong graphRequestTotalMs = new AtomicLong(); 
    private static AtomicLong legendRequestCount = new AtomicLong();
    private static AtomicLong legendRequestTotalMs = new AtomicLong(); 
    private static AtomicLong sparklineRequestCount = new AtomicLong();
    private static AtomicLong sparklineRequestTotalMs = new AtomicLong(); 	

    // JMX Exposed Beans
    
    public void incrementGraphRequestCount() {
        graphRequestCount.incrementAndGet();
    }
    
    public void updateGraphRequestTotalMs(long updateMs) {
        graphRequestTotalMs.addAndGet(updateMs);
    }
    
    public void incrementLegendRequestCount() {
        legendRequestCount.incrementAndGet();
    }
    
    public void updateLegendRequestTotalMs(long updateMs) {
        legendRequestTotalMs.addAndGet(updateMs);
    }
    
    public void incrementSparklineRequestCount() {
        sparklineRequestCount.incrementAndGet();
    }
    
    public void updateSparklineRequestTotalMs(long updateMs) {
        sparklineRequestTotalMs.addAndGet(updateMs);
    }
    
    public void incrementDataRequestCount() {
        dataRequestCount.incrementAndGet();
    }
    
    public void updateDataRequestTotalMs(long updateMs) {
        dataRequestTotalMs.addAndGet(updateMs);
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getGraphRequestCount() {
        return graphRequestCount.get();
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getGraphRequestTotalMs() {
        return graphRequestTotalMs.get();
    } 
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getLegendRequestCount() {
        return legendRequestCount.get();
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getLegendRequestTotalMs() {
        return legendRequestTotalMs.get();
    } 
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getSparklineRequestCount() {
        return sparklineRequestCount.get();
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getSparklineRequestTotalMs() {
        return sparklineRequestTotalMs.get();
    } 	
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getDataRequestCount() {
        return dataRequestCount.get();
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getDataRequestTotalMs() {
        return dataRequestTotalMs.get();
    } 	
}
