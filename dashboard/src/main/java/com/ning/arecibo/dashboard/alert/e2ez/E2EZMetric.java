package com.ning.arecibo.dashboard.alert.e2ez;

import java.util.Map;
import java.util.HashMap;

public class E2EZMetric implements E2EZNode {
    
    private final String metricName;
    private final String subHeading;
    private final String displayName;
    private final String eventType;
    private final String attributeType;
    private final String description;
    private final Map<String,String> qualifyingAttributes = new HashMap<String,String>();

    public E2EZMetric(String metricName,String subHeading,String displayName,String eventType,String attributeType,String description) {

        if(metricName == null)
            throw new IllegalStateException("metricName cannot be null");
        if(eventType == null)
            throw new IllegalStateException("eventType cannot be null");
        if(attributeType == null)
            throw new IllegalStateException("attributeType cannot be null");
        
        this.metricName = metricName;
        this.subHeading = subHeading;
        this.displayName = displayName;
        this.eventType = eventType;
        this.attributeType = attributeType;
        this.description = description;
    }

    public String getMetricName() {
        return metricName;
    }

    public String getSubHeading() {
        return subHeading;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAttributeType() {
        return attributeType;
    }

    public String getDescription() {
        return description;
    }

    public void addQualifyingAttribute(String qAttName,String qAttValue) {
        qualifyingAttributes.put(qAttName,qAttValue);
    }

    public String getQualifyingAttribute(String qAttName) {
        return qualifyingAttributes.get(qAttName);
    }
}
