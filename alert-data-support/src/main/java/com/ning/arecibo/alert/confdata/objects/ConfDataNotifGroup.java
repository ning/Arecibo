package com.ning.arecibo.alert.confdata.objects;

import java.util.Map;

import com.ning.arecibo.util.Logger;

public class ConfDataNotifGroup extends ConfDataObject
{
    private final static Logger log = Logger.getLogger(ConfDataNotifGroup.class);

    public final static String TYPE_NAME = "notif_group";
    public final static String INSERT_TEMPLATE_NAME = ":insert_notif_group";
    public final static String UPDATE_TEMPLATE_NAME = ":update_notif_group";

    private final static String ENABLED_FIELD = "enabled";

    protected volatile Boolean enabled = null;

    public ConfDataNotifGroup() {}

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
    public Map<String,Object> toPropertiesMap() {
        Map<String,Object> map = super.toPropertiesMap();
        setBoolean(map,ENABLED_FIELD,getEnabled());

        return map;
    }

    @Override
    public void setPropertiesFromMap(Map<String,Object> map) {
        super.setPropertiesFromMap(map);
        setEnabled(getBoolean(map,ENABLED_FIELD));
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
