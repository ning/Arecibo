package com.ning.arecibo.alert.confdata.objects;

import java.util.Map;

import com.ning.arecibo.util.Logger;

public class ConfDataThresholdConfig extends ConfDataObject
{
    private final static Logger log = Logger.getLogger(ConfDataThresholdConfig.class);

    public final static String TYPE_NAME = "threshold_config";
    public final static String INSERT_TEMPLATE_NAME = ":insert_threshold_config";
    public final static String UPDATE_TEMPLATE_NAME = ":update_threshold_config";

    private final static String ALERTING_CONFIG_ID_FIELD = "alerting_config_id";
    private final static String MONITORED_EVENT_TYPE_FIELD = "monitored_event_type";
    private final static String MONITORED_ATTRIBUTE_TYPE_FIELD = "monitored_attribute_type";
    private final static String CLEARING_INTERVAL_MS_FIELD = "clearing_interval_ms";
    private final static String MIN_THRESHOLD_VALUE_FIELD = "min_threshold_value";
    private final static String MAX_THRESHOLD_VALUE_FIELD = "max_threshold_value";
    private final static String MIN_THRESHOLD_SAMPLES_FIELD = "min_threshold_samples";
    private final static String MAX_SAMPLE_WINDOW_MS_FIELD = "max_sample_window_ms";

    protected volatile Long alertingConfigId = null;
    protected volatile String monitoredEventType = null;
    protected volatile String monitoredAttributeType = null;
    protected volatile Long clearingIntervalMs = null;
    protected volatile Double minThresholdValue = null;
    protected volatile Double maxThresholdValue = null;
    protected volatile Long minThresholdSamples = null;
    protected volatile Long maxSampleWindowMs = null;

    public ConfDataThresholdConfig() {}

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public String getInsertSqlTemplateName() {
        return INSERT_TEMPLATE_NAME;
    }

    @Override
    public String getUpdateSqlTemplateName() {
        return UPDATE_TEMPLATE_NAME;
    }

    @Override
    public void setPropertiesFromMap(Map<String,Object> map) {
        super.setPropertiesFromMap(map);
        setAlertingConfigId(getLong(map, ALERTING_CONFIG_ID_FIELD));
        setMonitoredEventType(getString(map,MONITORED_EVENT_TYPE_FIELD));
        setMonitoredAttributeType(getString(map,MONITORED_ATTRIBUTE_TYPE_FIELD));
        setClearingIntervalMs(getLong(map,CLEARING_INTERVAL_MS_FIELD));
        setMinThresholdValue(getDouble(map,MIN_THRESHOLD_VALUE_FIELD));
        setMaxThresholdValue(getDouble(map,MAX_THRESHOLD_VALUE_FIELD));
        setMinThresholdSamples(getLong(map,MIN_THRESHOLD_SAMPLES_FIELD));
        setMaxSampleWindowMs(getLong(map,MAX_SAMPLE_WINDOW_MS_FIELD));
    }

    @Override
    public Map<String,Object> toPropertiesMap() {
        Map<String,Object> map = super.toPropertiesMap();
        setLong(map, ALERTING_CONFIG_ID_FIELD, getAlertingConfigId());
        setString(map,MONITORED_EVENT_TYPE_FIELD,getMonitoredEventType());
        setString(map,MONITORED_ATTRIBUTE_TYPE_FIELD,getMonitoredAttributeType());
        setLong(map,CLEARING_INTERVAL_MS_FIELD,getClearingIntervalMs());
        setDouble(map,MIN_THRESHOLD_VALUE_FIELD,getMinThresholdValue());
        setDouble(map,MAX_THRESHOLD_VALUE_FIELD,getMaxThresholdValue());
        setLong(map,MIN_THRESHOLD_SAMPLES_FIELD,getMinThresholdSamples());
        setLong(map,MAX_SAMPLE_WINDOW_MS_FIELD,getMaxSampleWindowMs());

        return map;
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", ALERTING_CONFIG_ID_FIELD, getAlertingConfigId()));
        sb.append(String.format("   %s -> %s\n",MONITORED_EVENT_TYPE_FIELD,getMonitoredEventType()));
        sb.append(String.format("   %s -> %s\n",MONITORED_ATTRIBUTE_TYPE_FIELD,getMonitoredAttributeType()));
        sb.append(String.format("   %s -> %s\n",CLEARING_INTERVAL_MS_FIELD,getClearingIntervalMs()));
        sb.append(String.format("   %s -> %s\n",MIN_THRESHOLD_VALUE_FIELD,getMinThresholdValue()));
        sb.append(String.format("   %s -> %s\n",MAX_THRESHOLD_VALUE_FIELD,getMaxThresholdValue()));
        sb.append(String.format("   %s -> %s\n",MIN_THRESHOLD_SAMPLES_FIELD,getMinThresholdSamples()));
        sb.append(String.format("   %s -> %s\n",MAX_SAMPLE_WINDOW_MS_FIELD,getMaxSampleWindowMs()));
    }

    public Long getAlertingConfigId() {
        return alertingConfigId;
    }

    public void setAlertingConfigId(Long alertingConfigId) {
        this.alertingConfigId = alertingConfigId;
    }

    public String getMonitoredEventType() {
        return monitoredEventType;
    }

    public void setMonitoredEventType(String monitoredEventType) {
        this.monitoredEventType = monitoredEventType;
    }

    public String getMonitoredAttributeType() {
        return monitoredAttributeType;
    }

    public void setMonitoredAttributeType(String monitoredAttributeType) {
        this.monitoredAttributeType = monitoredAttributeType;
    }

    public Long getClearingIntervalMs() {
        return clearingIntervalMs;
    }

    public void setClearingIntervalMs(Long clearingIntervalMs) {
        this.clearingIntervalMs = clearingIntervalMs;
    }

    public Double getMinThresholdValue() {
        return minThresholdValue;
    }

    public void setMinThresholdValue(Double minThresholdValue) {
        this.minThresholdValue = minThresholdValue;
    }

    public Double getMaxThresholdValue() {
        return maxThresholdValue;
    }

    public void setMaxThresholdValue(Double maxThresholdValue) {
        this.maxThresholdValue = maxThresholdValue;
    }

    public Long getMinThresholdSamples() {
        return minThresholdSamples;
    }

    public void setMinThresholdSamples(Long minThresholdSamples) {
        this.minThresholdSamples = minThresholdSamples;
    }

    public Long getMaxSampleWindowMs() {
        return maxSampleWindowMs;
    }

    public void setMaxSampleWindowMs(Long maxSampleWindowMs) {
        this.maxSampleWindowMs = maxSampleWindowMs;
    }
}
