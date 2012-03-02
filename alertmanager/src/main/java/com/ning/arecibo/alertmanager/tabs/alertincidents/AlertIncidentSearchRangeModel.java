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

package com.ning.arecibo.alertmanager.tabs.alertincidents;

import java.util.Date;
import java.util.TimeZone;
import java.sql.Timestamp;

import org.apache.wicket.IClusterable;

public class AlertIncidentSearchRangeModel implements IClusterable {

    private final static TimeZone localTZ = TimeZone.getDefault();
    private final static long localOffset = localTZ.getOffset(System.currentTimeMillis());
    private final static long OneDay = 1000L * 60L * 60L * 24L;
    private final static long ThirtyOneDays = 31L * OneDay;

    private volatile String lastValidationMessage = null;
    private volatile Date searchRangeStart = null;
    private volatile Date searchRangeEnd = null;

    public AlertIncidentSearchRangeModel() {
        // by default, set search range to the last 24 hours
        long currTimeMs = System.currentTimeMillis() - localOffset;
        searchRangeStart = new Date(currTimeMs - OneDay);
        searchRangeEnd = new Date(currTimeMs);
    }

    public boolean validate() {
        lastValidationMessage = null;

        long start = searchRangeStart.getTime();
        long end = searchRangeEnd.getTime();

        // make sure start is before end
        if(end <= start) {
            lastValidationMessage = "Start Date must be before End Date";
            return false;
        }

        // check date range, don't allow more than say 31 days
        if(end - start > ThirtyOneDays) {
            lastValidationMessage = "Date range should be limited to 30 days";
            return false;
        }

        return true;
    }

    public String getLastValidationMessage() {
        return lastValidationMessage;
    }

    public Date getSearchRangeStart() {
        return searchRangeStart;
    }

    public void setSearchRangeStart(Date searchRangeStart) {
        this.searchRangeStart = searchRangeStart;
    }

    public Date getSearchRangeEnd() {
        return searchRangeEnd;
    }

    public void setSearchRangeEnd(Date searchRangeEnd) {
        this.searchRangeEnd = searchRangeEnd;
    }
}
