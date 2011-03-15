package com.ning.arecibo.dashboard.dao;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.List;

import com.google.inject.Inject;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;
import com.ning.arecibo.dashboard.graph.DashboardGraphUtils;
import com.ning.arecibo.dashboard.guice.DashboardCollectorKeepAliveHost;

import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;

public class DashboardCollectorDAOKeepAliveManager implements Runnable
{
    private final static Logger log = Logger.getLogger(DashboardCollectorDAOKeepAliveManager.class);

    // run every 5 minutes (might want to inject this later)
    private final static long KEEP_ALIVE_INTERVAL = 1000L * 60L * 5L;

    private ScheduledThreadPoolExecutor executor;
    private AtomicLong lastExecutionTime = new AtomicLong(0L);
    private AtomicLong lastExecutionResultCount = new AtomicLong(0L);

    private final DashboardCollectorDAO dao;
    private final GalaxyStatusManager galaxyStatusManager;
    private final String dashboardKeepAliveHost;

    @Inject
    public DashboardCollectorDAOKeepAliveManager(DashboardCollectorDAO dao,
                                                 GalaxyStatusManager galaxyStatusManager,
                                                 @DashboardCollectorKeepAliveHost String dashboardKeepAliveHost) {
        this.dao = dao;
        this.galaxyStatusManager = galaxyStatusManager;
        this.dashboardKeepAliveHost = dashboardKeepAliveHost;
    }

    public synchronized void start()
    {
        if(dashboardKeepAliveHost == null || dashboardKeepAliveHost.length() == 0) {
            log.info("No dashboard collector keep alive host defined, not starting keep alive manager");
            return;
        }

        // one thread should be fine
        this.executor = new ScheduledThreadPoolExecutor(1);

        // start the config updater
        this.executor.scheduleWithFixedDelay(this,0, KEEP_ALIVE_INTERVAL,TimeUnit.MILLISECONDS);
    }

    public synchronized void stop()
    {
        if (this.executor != null) {
            this.executor.shutdown();
            this.executor = null;
        }
    }

    public void run() {

        try {
            log.info("Running connection keep alive queries");
            long startTs = System.currentTimeMillis();

            // touch all the tables in the schema....
            int[] reductionFactors = dao.getReductionFactors();
            if(reductionFactors == null) {
                log.info("Got null reductionFactors from collector, skipping execution of keep alive manager");
                return;
            }

            long resultCount = 0L;
            String coreType = galaxyStatusManager.getCoreType(dashboardKeepAliveHost);
            String path = galaxyStatusManager.getConfigSubPath(dashboardKeepAliveHost);
            List<Map<String, Object>> results;

            String eventType = "DAOKeepAliveManager";
            String attributeType = "LastKeepAliveQueryTimeMS";

            for(int reductionFactor:reductionFactors) {

                long timeWindow = reductionFactor * DashboardGraphUtils.TimeWindow.getDefaultTimeWindow().getMillis();
                ResolutionRequest resRequest = new ResolutionRequest(ResolutionRequestType.FIXED,reductionFactor);

                if(dashboardKeepAliveHost != null && !dashboardKeepAliveHost.equals("")) {
                    results = dao.getValuesForHostEvent(dashboardKeepAliveHost,eventType,attributeType,resRequest,timeWindow);
                    if(results != null)
                        resultCount += results.size();
                }

                if(coreType != null && !coreType.equals("")) {
                    results = dao.getValuesForTypeEvent(coreType,eventType,attributeType,resRequest,timeWindow);
                    if(results != null)
                        resultCount += results.size();
                }

                if(coreType != null && !coreType.equals("") && path != null && !path.equals("")) {
                    results = dao.getValuesForPathWithTypeEvent(coreType,path,eventType,attributeType,resRequest,timeWindow);
                    if(results != null)
                        resultCount += results.size();
                }
            }

            log.info("Completed keep alive queries, returned %d values",resultCount);

            long time = System.currentTimeMillis() - startTs;
            lastExecutionTime.set(time);
            lastExecutionResultCount.set(resultCount);
        }
        catch(DashboardCollectorDAOException dcdEx) {
            log.warn(dcdEx,"Got DashboardCollectorDAOException running keep alive query");
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx,"Got RuntimeException running keep alive query");
        }
    }

    @MonitorableManaged(monitored = true)
    public long getLastKeepAliveQueryTimeMS() {
        return lastExecutionTime.get();
    }

    @MonitorableManaged(monitored = true)
    public long getLastKeepAliveQueryResultCount() {
        return lastExecutionResultCount.get();
    }
}
