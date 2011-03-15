package com.ning.arecibo.alert.confdata.objects;

import java.util.Map;

import com.ning.arecibo.util.Logger;

public class ConfDataMessagingDescription extends ConfDataObject
{
    private final static Logger log = Logger.getLogger(ConfDataMessagingDescription.class);

    public final static String TYPE_NAME = "messaging_description";
    public final static String INSERT_TEMPLATE_NAME = ":insert_messaging_description";
    public final static String UPDATE_TEMPLATE_NAME = ":update_messaging_description";

    private final static String MESSAGE_TYPE_FIELD = "message_type";
    private final static String MESSAGE_TEXT_FIELD = "message_text";

    protected volatile String messageType = null;
    protected volatile String messageText = null;

    public ConfDataMessagingDescription() {}

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
        setMessageType(getString(map,MESSAGE_TYPE_FIELD));
        setMessageText(getString(map,MESSAGE_TEXT_FIELD));
    }

    @Override
    public Map<String,Object> toPropertiesMap() {
        Map<String,Object> map = super.toPropertiesMap();
        setString(map,MESSAGE_TYPE_FIELD,getMessageType());
        setString(map,MESSAGE_TEXT_FIELD,getMessageText());

        return map;
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n",MESSAGE_TYPE_FIELD,getMessageType()));
        sb.append(String.format("   %s -> %s\n",MESSAGE_TEXT_FIELD,getMessageText()));
    }

    public String getMessageType() {
        return this.messageType;
    }

    public void setMessageType(String messagingType) {
        this.messageType = messageType;
    }

    public String getMessageText() {
        return this.messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }
}
