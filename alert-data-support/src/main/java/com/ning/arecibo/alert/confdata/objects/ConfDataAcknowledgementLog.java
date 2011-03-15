package com.ning.arecibo.alert.confdata.objects;

import java.util.Map;
import java.sql.Timestamp;

import com.ning.arecibo.util.Logger;

public class ConfDataAcknowledgementLog extends ConfDataObject
{
    private final static Logger log = Logger.getLogger(ConfDataAcknowledgementLog.class);

    public final static String TYPE_NAME = "acknowledgement_log";
    public final static String INSERT_TEMPLATE_NAME = ":insert_acknowledgement_log";
    public final static String UPDATE_TEMPLATE_NAME = ":update_acknowledgement_log";

    private final static String ALERT_INCIDENT_ID_FIELD = "alert_incident_id";
    private final static String PERSON_ID_FIELD = "person_id";
    private final static String ACK_TIME_FIELD = "ack_time";
    private final static String ACK_COMMENT_FIELD = "ack_comment";

    protected volatile Long alertIncidentId = null;
    protected volatile Long personId = null;
    protected volatile Timestamp ackTime = null;
    protected volatile String ackComment = null;

    public ConfDataAcknowledgementLog() {}

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
        setAlertIncidentId(getLong(map,ALERT_INCIDENT_ID_FIELD));
        setPersonId(getLong(map,PERSON_ID_FIELD));
        setAckTime(getTimestamp(map,ACK_TIME_FIELD));
        setAckComment(getString(map,ACK_COMMENT_FIELD));
    }

    @Override
    public Map<String,Object> toPropertiesMap() {
        Map<String,Object> map = super.toPropertiesMap();
        setLong(map,ALERT_INCIDENT_ID_FIELD,getAlertIncidentId());
        setLong(map,PERSON_ID_FIELD,getPersonId());
        setTimestamp(map,ACK_TIME_FIELD,getAckTime());
        setString(map,ACK_COMMENT_FIELD,getAckComment());

        return map;
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n",ALERT_INCIDENT_ID_FIELD,getAlertIncidentId()));
        sb.append(String.format("   %s -> %s\n",PERSON_ID_FIELD,getPersonId()));
        sb.append(String.format("   %s -> %s\n",ACK_TIME_FIELD,getAckTime()));
        sb.append(String.format("   %s -> %s\n",ACK_COMMENT_FIELD,getAckComment()));
    }

    public Long getAlertIncidentId() {
        return alertIncidentId;
    }

    public void setAlertIncidentId(Long alertIncidentId) {
        this.alertIncidentId = alertIncidentId;
    }

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public Timestamp getAckTime() {
        return ackTime;
    }

    public void setAckTime(Timestamp ackTime) {
        this.ackTime = ackTime;
    }

    public String getAckComment() {
        return ackComment;
    }

    public void setAckComment(String ackComment) {
        this.ackComment = ackComment;
    }
}
