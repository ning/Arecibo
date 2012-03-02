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
