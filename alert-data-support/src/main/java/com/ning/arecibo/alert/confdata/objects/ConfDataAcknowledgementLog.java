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

import java.sql.Timestamp;
import java.util.Map;

public class ConfDataAcknowledgementLog extends ConfDataObject
{
    public static final String TYPE_NAME = "acknowledgement_log";

    private static final String ALERT_INCIDENT_ID_FIELD = "alert_incident_id";
    private static final String PERSON_ID_FIELD = "person_id";
    private static final String ACK_TIME_FIELD = "ack_time";
    private static final String ACK_COMMENT_FIELD = "ack_comment";

    protected volatile Long alertIncidentId = null;
    protected volatile Long personId = null;
    protected volatile Timestamp ackTime = null;
    protected volatile String ackComment = null;

    public ConfDataAcknowledgementLog()
    {
    }

    @Override
    public String getTypeName()
    {
        return TYPE_NAME;
    }

    @Override
    public void populatePropertiesFromMap(final Map<String, Object> map)
    {
        super.populatePropertiesFromMap(map);
        setAlertIncidentId(getLong(map, ALERT_INCIDENT_ID_FIELD));
        setPersonId(getLong(map, PERSON_ID_FIELD));
        setAckTime(getTimestamp(map, ACK_TIME_FIELD));
        setAckComment(getString(map, ACK_COMMENT_FIELD));
    }

    @Override
    public Map<String, Object> toPropertiesMap()
    {
        final Map<String, Object> map = super.toPropertiesMap();
        setLong(map, ALERT_INCIDENT_ID_FIELD, getAlertIncidentId());
        setLong(map, PERSON_ID_FIELD, getPersonId());
        setTimestamp(map, ACK_TIME_FIELD, getAckTime());
        setString(map, ACK_COMMENT_FIELD, getAckComment());

        return map;
    }

    @Override
    public void toStringBuilder(final StringBuilder sb)
    {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", ALERT_INCIDENT_ID_FIELD, getAlertIncidentId()));
        sb.append(String.format("   %s -> %s\n", PERSON_ID_FIELD, getPersonId()));
        sb.append(String.format("   %s -> %s\n", ACK_TIME_FIELD, getAckTime()));
        sb.append(String.format("   %s -> %s\n", ACK_COMMENT_FIELD, getAckComment()));
    }

    public Long getAlertIncidentId()
    {
        return alertIncidentId;
    }

    public void setAlertIncidentId(final Long alertIncidentId)
    {
        this.alertIncidentId = alertIncidentId;
    }

    public Long getPersonId()
    {
        return personId;
    }

    public void setPersonId(final Long personId)
    {
        this.personId = personId;
    }

    public Timestamp getAckTime()
    {
        return ackTime;
    }

    public void setAckTime(final Timestamp ackTime)
    {
        this.ackTime = ackTime;
    }

    public String getAckComment()
    {
        return ackComment;
    }

    public void setAckComment(final String ackComment)
    {
        this.ackComment = ackComment;
    }
}
