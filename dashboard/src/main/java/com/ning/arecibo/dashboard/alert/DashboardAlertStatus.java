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
