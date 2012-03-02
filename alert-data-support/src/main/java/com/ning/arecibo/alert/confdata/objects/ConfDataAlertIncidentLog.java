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

package com.ning.arecibo.alert.confdata.objects;

import java.util.Map;
import java.sql.Timestamp;

import com.ning.arecibo.util.Logger;

public class ConfDataAlertIncidentLog extends ConfDataObject
{
    private final static Logger log = Logger.getLogger(ConfDataAlertIncidentLog.class);

    public final static String TYPE_NAME = "alert_incident_log";
    public final static String INSERT_TEMPLATE_NAME = ":insert_alert_incident_log";
    public final static String UPDATE_TEMPLATE_NAME = ":update_alert_incident_log";

    private final static String THRESHOLD_CONFIG_ID_FIELD = "threshold_config_id";
    private final static String CONTEXT_IDENTIFIER_FIELD = "context_identifier";
    private final static String START_TIME_FIELD = "start_time";
    private final static String CLEAR_TIME_FIELD = "clear_time";
    private final static String INITIAL_ALERT_EVENT_VALUE_FIELD = "initial_alert_event_value";
    private final static String SHORT_DESCRIPTION_FIELD = "short_description";

    private final static int MAX_CONTEXT_IDENTIFIER_LENGTH = 48;

    protected volatile Long thresholdConfigId = null;
    protected volatile String contextIdentifier = null;
    protected volatile Timestamp startTime = null;
    protected volatile Timestamp clearTime = null;
    protected volatile Double initialAlertEventValue = null;
    protected volatile String shortDescription = null;

    public ConfDataAlertIncidentLog() {}

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
        setThresholdConfigId(getLong(map, THRESHOLD_CONFIG_ID_FIELD));
        setContextIdentifier(getString(map,CONTEXT_IDENTIFIER_FIELD));
        setStartTime(getTimestamp(map,START_TIME_FIELD));
        setClearTime(getTimestamp(map,CLEAR_TIME_FIELD));
        setInitialAlertEventValue(getDouble(map,INITIAL_ALERT_EVENT_VALUE_FIELD));
        setShortDescription(getString(map,SHORT_DESCRIPTION_FIELD));
    }

    @Override
    public Map<String,Object> toPropertiesMap() {
        Map<String,Object> map = super.toPropertiesMap();
        setLong(map,THRESHOLD_CONFIG_ID_FIELD,getThresholdConfigId());
        setString(map,CONTEXT_IDENTIFIER_FIELD,getContextIdentifier());
        setTimestamp(map,START_TIME_FIELD,getStartTime());
        setTimestamp(map,CLEAR_TIME_FIELD,getClearTime());
        setDouble(map,INITIAL_ALERT_EVENT_VALUE_FIELD,getInitialAlertEventValue());
        setString(map,SHORT_DESCRIPTION_FIELD,getShortDescription());

        return map;
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n",THRESHOLD_CONFIG_ID_FIELD, getThresholdConfigId()));
        sb.append(String.format("   %s -> %s\n",CONTEXT_IDENTIFIER_FIELD, getContextIdentifier()));
        sb.append(String.format("   %s -> %s\n",START_TIME_FIELD,getStartTime()));
        sb.append(String.format("   %s -> %s\n",CLEAR_TIME_FIELD,getClearTime()));
        sb.append(String.format("   %s -> %s\n",INITIAL_ALERT_EVENT_VALUE_FIELD,getInitialAlertEventValue()));
        sb.append(String.format("   %s -> %s\n",SHORT_DESCRIPTION_FIELD,getShortDescription()));
    }

    public Long getThresholdConfigId() {
        return thresholdConfigId;
    }

    public void setThresholdConfigId(Long thresholdConfigId) {
        this.thresholdConfigId = thresholdConfigId;
    }

    public String getContextIdentifier() {
        return contextIdentifier;
    }

    public void setContextIdentifier(String contextIdentifier) {

        // make sure not to violate the max char limit for this column
        if(contextIdentifier.length() > MAX_CONTEXT_IDENTIFIER_LENGTH)
            contextIdentifier.substring(0,MAX_CONTEXT_IDENTIFIER_LENGTH);

        this.contextIdentifier = contextIdentifier;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getClearTime() {
        return clearTime;
    }

    public void setClearTime(Timestamp clearTime) {
        this.clearTime = clearTime;
    }

    public Double getInitialAlertEventValue() {
        return initialAlertEventValue;
    }

    public void setInitialAlertEventValue(Double initialAlertEventValue) {
        this.initialAlertEventValue = initialAlertEventValue;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }
}
