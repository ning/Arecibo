package com.ning.arecibo.dashboard.alert.e2ez;

import static com.ning.arecibo.dashboard.alert.AlertStatusManager.PATH_ATTR;
import static com.ning.arecibo.dashboard.alert.AlertStatusManager.TYPE_ATTR;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.inject.Inject;
import com.ning.arecibo.alert.client.AlertActivationStatus;
import com.ning.arecibo.alert.client.AlertStatus;
import com.ning.arecibo.alert.client.AlertType;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAOException;
import com.ning.arecibo.alert.confdata.objects.ConfDataAlertIncidentLog;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdQualifyingAttr;
import com.ning.arecibo.dashboard.alert.DashboardAlertStatus;
import com.ning.arecibo.dashboard.guice.E2EZThresholdConfigCacheSize;
import com.ning.arecibo.dashboard.guice.E2EZTimeRangeMapCacheSize;
import com.ning.arecibo.util.LRUCache;

public class E2EZHistoricalMetricStatusCache {

    private final LRUCache<String, Map<String,DashboardAlertStatus>> lruTimeRangeMapCache;
    private final LRUCache<Long, DashboardAlertStatus> lruPerThresholdConfigStatusCache;
    private final ConfDataDAO alertConfDataDAO;

    @Inject
    public E2EZHistoricalMetricStatusCache(@E2EZThresholdConfigCacheSize int thesholdConfigCacheSize,
                                           @E2EZTimeRangeMapCacheSize int timeRangeCacheSize,
                                           ConfDataDAO alertConfDataDAO) {
        this.alertConfDataDAO = alertConfDataDAO;
        this.lruPerThresholdConfigStatusCache = new LRUCache<Long, DashboardAlertStatus>(thesholdConfigCacheSize);
        this.lruTimeRangeMapCache = new LRUCache<String, Map<String,DashboardAlertStatus>>(timeRangeCacheSize);
    }

    public String getMetricCacheKey(E2EZMetric metric) {

        return getMetricCacheKey(metric.getEventType(),metric.getAttributeType(),
                metric.getQualifyingAttribute(TYPE_ATTR),metric.getQualifyingAttribute(PATH_ATTR));
    }

    public String getMetricCacheKey(DashboardAlertStatus alertStatus) {
        return getMetricCacheKey(alertStatus.getEventType(),alertStatus.getAttributeType(),
                        alertStatus.getAttribute(TYPE_ATTR),alertStatus.getAttribute(PATH_ATTR));
    }

    public String getMetricCacheKey(String eventType,String attributeType,String deployedType,String deployedPath) {

        return eventType + ":" +
                attributeType + ":" +
                deployedType + ":" +
                deployedPath;
    }

    public String getTimeRangeCacheKey(long startTime,long endTime) {
        return startTime + ":" + endTime;
    }

    public void getCachedStatus(E2EZMetric metric,long startTime,long endTime,List<DashboardAlertStatus> alertStatus) {

        String timeRangeKey = getTimeRangeCacheKey(startTime,endTime);

        Map<String,DashboardAlertStatus> timeRangeMap;
        synchronized(lruTimeRangeMapCache) {
            timeRangeMap = lruTimeRangeMapCache.get(timeRangeKey);
        }

        if(timeRangeMap == null) {
            timeRangeMap = getTimeRangeMap(startTime,endTime);

            synchronized(lruTimeRangeMapCache) {
                lruTimeRangeMapCache.put(timeRangeKey,timeRangeMap);
            }
        }

        String metricCacheKey = getMetricCacheKey(metric);
        DashboardAlertStatus status = timeRangeMap.get(metricCacheKey);

        if(status == null)
            return;

        alertStatus.add(status);
    }

    private Map<String,DashboardAlertStatus> getTimeRangeMap(long startTime,long endTime) {

        List<ConfDataThresholdConfig> thresholdConfigs = getHistoricalThresholdsInAlert(startTime, endTime);
        ConcurrentHashMap<String,DashboardAlertStatus> timeRangeMap = new ConcurrentHashMap<String,DashboardAlertStatus>();

        for (ConfDataThresholdConfig thresh : thresholdConfigs) {

            String thresholdConfigId = thresh.getLabel();

            // throw out any not dedicated to E2EZ
            if(!thresholdConfigId.contains(E2EZStatusManager.E2EZ))
                continue;

            // only consider WARN or CRITICAL alerts
            E2EZMetricStatus threshStatusType = E2EZMetricStatus.parseMetricStatusFromString(thresholdConfigId);
            if(threshStatusType.equals(E2EZMetricStatus.UNKNOWN))
                continue;

            // see if we already have this one cached
            synchronized(lruPerThresholdConfigStatusCache) {
                DashboardAlertStatus cachedStatus = lruPerThresholdConfigStatusCache.get(thresh.getId());

                if(cachedStatus != null) {
                    updateTimeRangeMapIfNecessary(timeRangeMap,cachedStatus,threshStatusType);
                    continue;
                }
            }

            // TODO: This is somewhat redundant with code in the ThresholdConfig, in the alert core, need to refactor to reuse
            // create baseStatus
            AlertStatus baseStatus = new AlertStatus(thresh.getId().toString(), AlertType.THRESHOLD, AlertActivationStatus.ERROR,
                                                        thresh.getMonitoredEventType(),thresh.getMonitoredAttributeType());

            // get qAtts
            List<ConfDataThresholdQualifyingAttr> qAtts = getThresholdQualifyingAttributes(thresh);

            // look for deployed type or path
            for(ConfDataThresholdQualifyingAttr qAtt:qAtts) {
                baseStatus.addAuxAttribute(qAtt.getAttributeType(),qAtt.getAttributeValue());
            }

            // TODO: add support for context attributes

            baseStatus.addAuxAttribute("thresholdConfigId", thresh.getLabel());
            //baseStatus.addAuxAttribute("shortDescription", thresh.getShortDescription());

            // convert to a DashboardAlertStatus
            DashboardAlertStatus status = new DashboardAlertStatus(baseStatus,0L);

            // add to cache
            synchronized(lruPerThresholdConfigStatusCache) {
                lruPerThresholdConfigStatusCache.put(thresh.getId(),status);
            }

            updateTimeRangeMapIfNecessary(timeRangeMap,status,threshStatusType);
        }

        return timeRangeMap;
    }

    private void updateTimeRangeMapIfNecessary(Map<String,DashboardAlertStatus> timeRangeMap,DashboardAlertStatus newStatus,E2EZMetricStatus newStatusType) {

        // see if we already have one for this metric in this time range
        String metricKey = getMetricCacheKey(newStatus);
        DashboardAlertStatus currTimeRangeStatus = timeRangeMap.get(metricKey);

        // if we already have one, see if we should replace it
        if(currTimeRangeStatus != null) {
            String currThresholdConfigId = currTimeRangeStatus.getAttribute("thresholdConfigId");
            E2EZMetricStatus currThreshStatusType = E2EZMetricStatus.parseMetricStatusFromString(currThresholdConfigId);

            // see if current one is outranked by our new one
            if(currThreshStatusType.isLessSevereThan(newStatusType))
                timeRangeMap.put(metricKey,newStatus);
        }
        else
            timeRangeMap.put(metricKey,newStatus);
    }


    private List<ConfDataThresholdConfig> getHistoricalThresholdsInAlert(long startTime, long endTime) {

        try {
            // get the list of alerts for period of time
            List<ConfDataThresholdConfig> thresholdConfigs = alertConfDataDAO.selectBySecondaryDoubleColumnRange(
                    "id",
                    ConfDataThresholdConfig.TYPE_NAME,
                    "threshold_config_id",
                    "start_time",
                    "clear_time",
                    ConfDataAlertIncidentLog.TYPE_NAME,
                    startTime,
                    endTime,
                    startTime,
                    endTime,
                    ConfDataThresholdConfig.class);

            return thresholdConfigs;
        }
        catch (ConfDataDAOException cdEx) {
            throw new RuntimeException(cdEx);
        }
    }

    private List<ConfDataThresholdQualifyingAttr> getThresholdQualifyingAttributes(ConfDataThresholdConfig thresh) {

        try {
            List<ConfDataThresholdQualifyingAttr> qAtts = alertConfDataDAO.selectByColumn("threshold_config_id",thresh.getId(),
                                                                                            ConfDataThresholdQualifyingAttr.TYPE_NAME,ConfDataThresholdQualifyingAttr.class);

            return qAtts;
        }
        catch(ConfDataDAOException cdEx) {
            throw new RuntimeException(cdEx);
        }
    }
}
