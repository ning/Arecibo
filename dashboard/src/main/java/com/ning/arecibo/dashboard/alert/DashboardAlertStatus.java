package com.ning.arecibo.dashboard.alert;

import com.ning.arecibo.alert.client.AlertStatus;
import com.ning.arecibo.dashboard.format.TimeFormatter;


public class DashboardAlertStatus
{
    private final AlertStatus baseStatus;
    private final long generationCount;

    public DashboardAlertStatus(AlertStatus baseStatus,long generationCount) {
        this.baseStatus = baseStatus;
        this.generationCount = generationCount;
    }

    public long getGenerationCount() {
    	return this.generationCount;
    }

    public String getAlertId() {
        return baseStatus.getAlertId();
    }

    public String getAlertType() {
        return baseStatus.getAlertType().toString();
    }

    public String getActivationStatus() {
        return baseStatus.getActivationStatus().toString();
    }

    public String getEventType() {
        return baseStatus.getEventType();
    }

    public String getAttributeType() {
        return baseStatus.getAttributeType();
    }
    
    public String getThresholdConfigId() {
        return baseStatus.getAuxAttribute("thresholdConfigId");
    }
    
    public String getShortDescription() {
        return baseStatus.getAuxAttribute("shortDescription");
    }
    
    public String getTimeInAlert() {
        return baseStatus.getAuxAttribute("timeInAlert");
    }

    public String getAttribute(String attribute) {
        return baseStatus.getAuxAttribute(attribute);
    }
    
    public String getFormattedTimeInAlert() {
    	try {
        	long timeInAlertMillis = Long.parseLong(this.getTimeInAlert());
        	return TimeFormatter.formatAsMilliseconds(timeInAlertMillis);
    	}
    	catch(NumberFormatException numEx) {
    	    return null;
    	} 
    }
}
