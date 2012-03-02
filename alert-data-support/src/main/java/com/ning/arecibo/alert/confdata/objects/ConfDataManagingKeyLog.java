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

public class ConfDataManagingKeyLog extends ConfDataObject
{
    private final static Logger log = Logger.getLogger(ConfDataManagingKeyLog.class);

    public final static String TYPE_NAME = "managing_key_log";
    public final static String INSERT_TEMPLATE_NAME = ":insert_managing_key_log";
    public final static String UPDATE_TEMPLATE_NAME = ":update_managing_key_log";

    private final static String MANAGING_KEY_ID_FIELD = "managing_key_id";
    private final static String ACTION_FIELD = "action";
    private final static String START_TIME_FIELD = "start_time";
    private final static String END_TIME_FIELD = "end_time";

    protected volatile Long managingKeyId = null;
    protected volatile String action = null;
    protected volatile Timestamp startTime = null;
    protected volatile Timestamp endTime = null;

    public ConfDataManagingKeyLog() {}

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
        setManagingKeyId(getLong(map, MANAGING_KEY_ID_FIELD));
        setAction(getString(map,ACTION_FIELD));
        setStartTime(getTimestamp(map,START_TIME_FIELD));
        setEndTime(getTimestamp(map,END_TIME_FIELD));
    }

    @Override
    public Map<String,Object> toPropertiesMap() {
        Map<String,Object> map = super.toPropertiesMap();
        setLong(map, MANAGING_KEY_ID_FIELD, getManagingKeyId());
        setString(map,ACTION_FIELD,getAction());
        setTimestamp(map,START_TIME_FIELD,getStartTime());
        setTimestamp(map,END_TIME_FIELD,getEndTime());

        return map;
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", MANAGING_KEY_ID_FIELD, getManagingKeyId()));
        sb.append(String.format("   %s -> %s\n",ACTION_FIELD,getAction()));
        sb.append(String.format("   %s -> %s\n",START_TIME_FIELD,getStartTime()));
        sb.append(String.format("   %s -> %s\n",END_TIME_FIELD,getEndTime()));
    }

    public Long getManagingKeyId() {
        return managingKeyId;
    }

    public void setManagingKeyId(Long managingKeyId) {
        this.managingKeyId = managingKeyId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
}
