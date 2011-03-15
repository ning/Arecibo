package com.ning.arecibo.alert.client;

import java.util.HashMap;
import java.util.Map;

public class AlertStatus
{
    private final String alertId;
    private final AlertType alertType;
    private final String eventType;
    private final String attributeType;
    private final AlertActivationStatus activationStatus;
    private final Map<String,String> auxAttributes;

    public AlertStatus(String alertId,AlertType alertType, AlertActivationStatus activationStatus,String eventType,String attributeType) {
        this.alertId = alertId;
        this.alertType = alertType;
        this.activationStatus = activationStatus;
        this.eventType = eventType;
        this.attributeType = attributeType;
        this.auxAttributes = new HashMap<String,String>();
    }

    public final String getAlertId() {
        return alertId;
    }

    public final AlertType getAlertType() {
        return alertType;
    }

    public final String getEventType() {
        return eventType;
    }

    public final String getAttributeType() {
        return attributeType;
    }

    public final void addAuxAttribute(String attribute,String value) {
        auxAttributes.put(attribute, value);
    }

    public final String getAuxAttribute(String attribute) {
        return auxAttributes.get(attribute);
    }

    public final AlertActivationStatus getActivationStatus() {
        return activationStatus;
    }

    public final Map<String,String> getAuxAttributeMap() {
        return auxAttributes;
    }

    public final void setAuxAttributeMap(Map<String,String> auxAttributes) {
        this.auxAttributes.clear();
        this.auxAttributes.putAll(auxAttributes);
    }
}
