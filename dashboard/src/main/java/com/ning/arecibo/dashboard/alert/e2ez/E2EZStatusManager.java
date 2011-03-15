package com.ning.arecibo.dashboard.alert.e2ez;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.ning.arecibo.dashboard.alert.AlertStatusManager;
import com.ning.arecibo.dashboard.alert.DashboardAlertStatus;
import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.dashboard.alert.AlertStatusManager.PATH_ATTR;
import static com.ning.arecibo.dashboard.alert.AlertStatusManager.TYPE_ATTR;

public class E2EZStatusManager {
    private final static Logger log = Logger.getLogger(E2EZStatusManager.class);

    // constants in the alert threshold config naming
    public final static String E2EZ = "E2EZ";
    public final static String WARN = "WARN";
    public final static String CRITICAL = "CRITICAL";

    private final AlertStatusManager alertStatusManager;
    private final E2EZHistoricalMetricStatusCache historicalCache;
    private final E2EZConfigManager configManager;

    @Inject
    public E2EZStatusManager(E2EZConfigManager configManager,
                                    E2EZHistoricalMetricStatusCache historicalCache,
                                    AlertStatusManager alertStatusManager) {

        this.configManager = configManager;
        this.historicalCache = historicalCache;
        this.alertStatusManager = alertStatusManager;
    }

    public E2EZMetricStatus getCurrentMetricStatus(String metricName) {

        E2EZMetric metric = configManager.getMetric(metricName);
        if(metric == null)
            return E2EZMetricStatus.UNKNOWN;

       return getCurrentMetricStatus(metric);
    }

    public E2EZMetricStatus getCurrentMetricStatus(E2EZMetric metric) {

        List<DashboardAlertStatus> alertList = new ArrayList<DashboardAlertStatus>();
        getMatchingCurrentAlertStatusForMetric(metric,alertList);
        return getStatusFromAlertList(alertList);
    }

    public E2EZMetricStatus getHistoricalMetricStatus(String metricName,long startTime,long endTime) {

        E2EZMetric metric = configManager.getMetric(metricName);
        if(metric == null)
            return E2EZMetricStatus.UNKNOWN;

        return getHistoricalMetricStatus(metric,startTime,endTime);
    }

    public E2EZMetricStatus getHistoricalMetricStatus(E2EZMetric metric,long startTime,long endTime) {

        List<DashboardAlertStatus> alertList = new ArrayList<DashboardAlertStatus>();
        getMatchingHistoricalAlertStatusForMetric(metric,startTime,endTime,alertList);
        return getStatusFromAlertList(alertList);
    }

    public E2EZMetricStatus getCurrentMetricGroupStatus(String metricGroupName) {
        E2EZMetricGroup metricGroup = configManager.getMetricGroup(metricGroupName);
        if(metricGroup != null)
            return getCurrentMetricGroupStatus(metricGroup);
        else
            return E2EZMetricStatus.UNKNOWN;
    }

    public E2EZMetricStatus getCurrentMetricGroupStatus(E2EZMetricGroup metricGroup) {

        List<DashboardAlertStatus> alertList = new ArrayList<DashboardAlertStatus>();

        getMatchingCurrentAlertStatusForMetricGroup(metricGroup,alertList);
        return getStatusFromAlertList(metricGroup,alertList);
    }

    public E2EZMetricStatus getHistoricalMetricGroupStatus(String metricGroupName,long startTime,long endTime) {
        E2EZMetricGroup metricGroup = configManager.getMetricGroup(metricGroupName);
        if(metricGroup != null)
            return getHistoricalMetricGroupStatus(metricGroup,startTime,endTime);
        else
            return E2EZMetricStatus.UNKNOWN;
    }

    public E2EZMetricStatus getHistoricalMetricGroupStatus(E2EZMetricGroup metricGroup,long startTime,long endTime) {

        List<DashboardAlertStatus> alertList = new ArrayList<DashboardAlertStatus>();

        getMatchingHistoricalAlertStatusForMetricGroup(metricGroup,startTime,endTime,alertList);
        return getStatusFromAlertList(metricGroup,alertList);
    }

    private E2EZMetricStatus getStatusFromAlertList(List<DashboardAlertStatus> alertList) {

        int warnCount = 0;

        for(DashboardAlertStatus alert:alertList) {

            String id = alert.getThresholdConfigId();
            if(id.contains(WARN))
                warnCount++;
            else if(id.contains(CRITICAL))
                return E2EZMetricStatus.CRITICAL;
        }

        if(warnCount > 0)
            return E2EZMetricStatus.WARN;
        else
            return E2EZMetricStatus.OK;

        // TODO: Eventually return INVESTIGATING or UNKNOWN, in response to ops input, etc.
    }

    private E2EZMetricStatus getStatusFromAlertList(E2EZMetricGroup metricGroup,List<DashboardAlertStatus> alertList) {

        int warnCount = 0;
        int criticalCount = 0;

        for(DashboardAlertStatus alert:alertList) {

            String id = alert.getThresholdConfigId();
            if(id.contains(WARN))
                warnCount++;
            else if(id.contains(CRITICAL))
                criticalCount++;
        }

        if(criticalCount >= metricGroup.getCriticalThreshold())
            return E2EZMetricStatus.CRITICAL;
        if(criticalCount + warnCount >= metricGroup.getWarnThreshold())
            return E2EZMetricStatus.WARN;
        else
            return E2EZMetricStatus.OK;

        // TODO: Eventually return INVESTIGATING or UNKNOWN, in response to ops input, etc.
    }

    private void getMatchingCurrentAlertStatusForMetric(E2EZMetric metric,List<DashboardAlertStatus> alertList) {

        List<DashboardAlertStatus> alertStatii = alertStatusManager.getMetricsInAlert(metric.getEventType(), metric.getAttributeType(),
                metric.getQualifyingAttribute(TYPE_ATTR),
                metric.getQualifyingAttribute(PATH_ATTR),
                null);

        if(alertStatii!=null) {
            for (DashboardAlertStatus alertStatus : alertStatii) {

                String id = alertStatus.getThresholdConfigId();
                if (id.contains(E2EZ))
                    alertList.add(alertStatus);
            }
        }
    }

    private void getMatchingCurrentAlertStatusForMetricGroup(E2EZMetricGroup metricGroup,List<DashboardAlertStatus> alertList) {

        for(E2EZNode child:metricGroup.getChildren()) {

            if(child instanceof E2EZMetricGroup) {
                getMatchingCurrentAlertStatusForMetricGroup((E2EZMetricGroup)child,alertList);
            }
            else if(child instanceof E2EZMetric) {
                E2EZMetric metric = (E2EZMetric)child;
                getMatchingCurrentAlertStatusForMetric(metric,alertList);
            }
        }
    }


    private void getMatchingHistoricalAlertStatusForMetric(E2EZMetric metric,long startTime,long endTime,List<DashboardAlertStatus> alertList) {

        historicalCache.getCachedStatus(metric,startTime,endTime,alertList);
    }

    private void getMatchingHistoricalAlertStatusForMetricGroup(E2EZMetricGroup metricGroup,long startTime,long endTime,List<DashboardAlertStatus> alertList) {

        for(E2EZNode child:metricGroup.getChildren()) {

            if(child instanceof E2EZMetricGroup) {
                getMatchingHistoricalAlertStatusForMetricGroup((E2EZMetricGroup)child,startTime,endTime,alertList);
            }
            else if(child instanceof E2EZMetric) {
                E2EZMetric metric = (E2EZMetric)child;
                getMatchingHistoricalAlertStatusForMetric(metric,startTime,endTime,alertList);
            }
        }
    }


}
