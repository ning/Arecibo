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

package com.ning.arecibo.alertmanager.tabs.managingkey;

import java.util.Date;
import java.sql.Timestamp;

import org.apache.wicket.IClusterable;
import org.joda.time.LocalTime;
import org.joda.time.DateTimeZone;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKey;
import com.ning.arecibo.alertmanager.utils.ConfDataFormModel;
import com.ning.arecibo.alertmanager.utils.DayOfWeekUtils;
import com.ning.arecibo.alertmanager.utils.columns.TimestampPropertyColumn;

import com.ning.arecibo.util.Logger;


public class ManagingKeyFormModel extends ConfDataManagingKey implements ConfDataFormModel, IClusterable {
    final static Logger log = Logger.getLogger(ManagingKeyFormModel.class);

    private volatile String lastMessage = null;

    public ManagingKeyFormModel() {
        this(null);
    }

    public ManagingKeyFormModel(ConfDataManagingKey confDataManagingKey) {

        if(confDataManagingKey != null)
            this.setPropertiesFromMap(confDataManagingKey.toPropertiesMap());
    }

    public Date getManualOverrideUntil() {
        Timestamp ts = getActivatedUntilTs();
        if(ts != null)
            return new Date(ts.getTime());
        else
            return null;
    }

    public void setManualOverrideUntil(Date time) {

        Timestamp ts;
        if(time != null)
            ts = new Timestamp(time.getTime());
        else
            ts = null;

        setActivatedUntilTs(ts);
    }

    public String getManualOverrideUntilString() {
        Date date = getManualOverrideUntil();

        if(date == null)
            return null;

        return TimestampPropertyColumn.format(new Timestamp(date.getTime()));
    }

    public Boolean getManualOverrideIndefinitely() {
        return getActivatedIndefinitely();
    }

    public void setManualOverrideIndefinitely(Boolean manualOverrideIndefinitely) {
        setActivatedIndefinitely(manualOverrideIndefinitely);
    }

    public Integer getAutoActivateTODStartHours() {
        Long millis = getAutoActivateTODStartMs();
        if(millis != null) {
            LocalTime lt = new LocalTime(millis, DateTimeZone.forOffsetHours(0));
            return lt.getHourOfDay();
        }
        else
            return null;
    }

    public void setAutoActivateTODStartHours(Integer hours) {
        if(hours == null) {
            Long millis = getAutoActivateTODStartMs();
            LocalTime lt;
            if(millis != null) {
                lt = new LocalTime(millis,DateTimeZone.forOffsetHours(0)).withHourOfDay(0);
                setAutoActivateTODStartMs((long)lt.getMillisOfDay());
            }
        }
        else {
            Long millis = getAutoActivateTODStartMs();
            LocalTime lt;
            if(millis != null) {
                lt = new LocalTime(millis,DateTimeZone.forOffsetHours(0)).withHourOfDay(hours);
            }
            else {
                lt = new LocalTime(0L,DateTimeZone.forOffsetHours(0)).withHourOfDay(hours);
            }
            setAutoActivateTODStartMs((long)lt.getMillisOfDay());
        }
    }

    public Integer getAutoActivateTODStartMinutes() {
        Long millis = getAutoActivateTODStartMs();
        if(millis != null) {
            LocalTime lt = new LocalTime(millis,DateTimeZone.forOffsetHours(0));
            return lt.getMinuteOfHour();
        }
        else {
            return null;
        }
    }

    public void setAutoActivateTODStartMinutes(Integer minutes) {
        if(minutes == null) {
            Long millis = getAutoActivateTODStartMs();
            LocalTime lt;
            if(millis != null) {
                lt = new LocalTime(millis,DateTimeZone.forOffsetHours(0)).withMinuteOfHour(0);
                setAutoActivateTODStartMs((long)lt.getMillisOfDay());
            }
        }
        else {
            Long millis = getAutoActivateTODStartMs();
            LocalTime lt;
            if(millis != null) {
                lt = new LocalTime(millis,DateTimeZone.forOffsetHours(0)).withMinuteOfHour(minutes);
            }
            else {
                lt = new LocalTime(0L,DateTimeZone.forOffsetHours(0)).withMinuteOfHour(minutes);
            }
            setAutoActivateTODStartMs((long)lt.getMillisOfDay());
        }
    }

    public Integer getAutoActivateTODEndHours() {
        Long millis = getAutoActivateTODEndMs();
        if(millis != null) {
            LocalTime lt = new LocalTime(millis,DateTimeZone.forOffsetHours(0));
            return lt.getHourOfDay();
        }
        else {
            return null;
        }
    }

    public void setAutoActivateTODEndHours(Integer hours) {
        if(hours == null) {
            Long millis = getAutoActivateTODEndMs();
            LocalTime lt;
            if(millis != null) {
                lt = new LocalTime(millis,DateTimeZone.forOffsetHours(0)).withHourOfDay(0);
                setAutoActivateTODEndMs((long)lt.getMillisOfDay());
            }
        }
        else {
            Long millis = getAutoActivateTODEndMs();
            LocalTime lt;
            if(millis != null) {
                lt = new LocalTime(millis,DateTimeZone.forOffsetHours(0)).withHourOfDay(hours);
            }
            else {
                lt = new LocalTime(0L,DateTimeZone.forOffsetHours(0)).withHourOfDay(hours);
            }
            setAutoActivateTODEndMs((long)lt.getMillisOfDay());
        }
    }

    public Integer getAutoActivateTODEndMinutes() {
        Long millis = getAutoActivateTODEndMs();
        if(millis != null) {
            LocalTime lt = new LocalTime(millis,DateTimeZone.forOffsetHours(0));
            return lt.getMinuteOfHour();
        }
        else {
            return null;
        }
    }

    public void setAutoActivateTODEndMinutes(Integer minutes) {
        if(minutes == null) {
            Long millis = getAutoActivateTODEndMs();
            LocalTime lt;
            if(millis != null) {
                lt = new LocalTime(millis,DateTimeZone.forOffsetHours(0)).withMinuteOfHour(0);
                setAutoActivateTODEndMs((long)lt.getMillisOfDay());
            }
        }
        else {
            Long millis = getAutoActivateTODEndMs();
            LocalTime lt;
            if(millis != null) {
                lt = new LocalTime(millis,DateTimeZone.forOffsetHours(0)).withMinuteOfHour(minutes);
            }
            else {
                lt = new LocalTime(0L,DateTimeZone.forOffsetHours(0)).withMinuteOfHour(minutes);
            }
            setAutoActivateTODEndMs((long)lt.getMillisOfDay());
        }
    }

    public String getAutoActivateDOWStartString() {
        Long dow = getAutoActivateDOWStart();

        if(dow != null)
            return DayOfWeekUtils.format(dow.intValue());
        else
            return DayOfWeekUtils.NONE_SELECTED;
    }

    public void setAutoActivateDOWStartString(String dowString) {

        if(dowString != null && !dowString.equals(DayOfWeekUtils.NONE_SELECTED)) {
            setAutoActivateDOWStart(DayOfWeekUtils.getDayOfWeekFromString(dowString).longValue());
        }
        else {
            setAutoActivateDOWStart(null);
        }
    }

    public String getAutoActivateDOWEndString() {
        Long dow = getAutoActivateDOWEnd();

        if(dow != null)
            return DayOfWeekUtils.format(dow.intValue());
        else
            return DayOfWeekUtils.NONE_SELECTED;
    }

    public void setAutoActivateDOWEndString(String dowString) {

        if(dowString != null && !dowString.equals(DayOfWeekUtils.NONE_SELECTED)) {
            setAutoActivateDOWEnd(DayOfWeekUtils.getDayOfWeekFromString(dowString).longValue());
        }
        else {
            setAutoActivateDOWEnd(null);
        }
    }

    private boolean validate() {
        // make sure we have start and end TOD, or none
        if((getAutoActivateTODStartMs() != null && getAutoActivateTODEndMs() == null) ||
                (getAutoActivateTODStartMs() == null && getAutoActivateTODEndMs() != null)) {

            lastMessage = "Must have both Start and End Time of Day specified (or neither)";
            return false;
        }

        // make sure we have start and end DOW, or none
        if((getAutoActivateDOWStart() != null && getAutoActivateDOWEnd() == null) ||
                (getAutoActivateDOWStart() == null && getAutoActivateDOWEnd() != null)) {

            lastMessage = "Must have both Start and End Day of Week specified (or neither)";
            return false;
        }

        return true;
    }

    private void cleanData() {
        this.setLabel(this.getKey());

        if(this.getManualOverrideIndefinitely() != null && this.getManualOverrideIndefinitely()) {
            setActivatedUntilTs(null);
        }
    }

    public boolean insert(ConfDataDAO confDataDAO) {

        try {

            if(!validate())
                return false;

            lastMessage = null;
            cleanData();

            confDataDAO.insert(this);

            lastMessage = "Inserted new managingKey record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            //log.warn(ex);

            // TODO: this logic is a hack, need a more elegant way to determine this
            // maybe check against the list before inserting to the db
            String baseMessage = getBaseErrorMessageFromException(ex);
            if(baseMessage.contains("ORA-00001")) {
                baseMessage = "Got uniqueness violation, the managing key, must be unique in the system";
            }
            lastMessage = "Got error inserting managing key record: " + baseMessage;
            return false;
        }
    }

    public boolean update(ConfDataDAO confDataDAO) {

        try {
            if(!validate())
                return false;

            lastMessage = null;
            cleanData();

            ConfDataManagingKey currDbManagingKey = confDataDAO.selectById(this.getId(),this.getTypeName(),ConfDataManagingKey.class);
            if(currDbManagingKey == null) {
                // this could happen if someone else deletes this object while we're editing...
                confDataDAO.insert(this);
            }
            else if(!currDbManagingKey.toPropertiesMap().equals(this.toPropertiesMap())) {
                confDataDAO.update(this);
            }

            lastMessage = "Updated managingKey record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            lastMessage = "Got error updating managingKey record: " + getBaseErrorMessageFromException(ex);
            return false;
        }
    }

    public boolean delete(ConfDataDAO confDataDAO) {

        try {
            lastMessage = null;

            // all related notification configs will be automatically deleted via cascading delete constraints
            confDataDAO.delete(this);

            lastMessage = "Deleted managingKey record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            lastMessage = "Got error deleting managingKey record: " + getBaseErrorMessageFromException(ex);
            return false;
        }
    }

    public String getLastStatusMessage() {

        return lastMessage;
    }

    private String getBaseErrorMessageFromException(Exception ex) {

        if(ex == null)
            return null;

        Throwable t = ex;
        while(t.getCause() != null) {
            t = t.getCause();
        }

        if (t.getMessage() == null)
            return t.toString();

        return t.getMessage();
    }
}
