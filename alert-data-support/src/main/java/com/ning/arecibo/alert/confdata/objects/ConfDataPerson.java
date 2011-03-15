package com.ning.arecibo.alert.confdata.objects;

import java.util.Map;

import com.ning.arecibo.util.Logger;

public class ConfDataPerson extends ConfDataObject
{
    private final static Logger log = Logger.getLogger(ConfDataPerson.class);

    public final static String TYPE_NAME = "person";
    public final static String INSERT_TEMPLATE_NAME = ":insert_person";
    public final static String UPDATE_TEMPLATE_NAME = ":update_person";

    private final static String FIRST_NAME_FIELD = "first_name";
    private final static String LAST_NAME_FIELD = "last_name";
    private final static String IS_GROUP_ALIAS_FIELD = "is_group_alias";

    protected volatile String firstName = null;
    protected volatile String lastName = null;
    protected volatile Boolean isGroupAlias = null;

    public ConfDataPerson() {}

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
        setFirstName(getString(map,FIRST_NAME_FIELD));
        setLastName(getString(map,LAST_NAME_FIELD));
        setIsGroupAlias(getBoolean(map,IS_GROUP_ALIAS_FIELD));
    }

    @Override
    public Map<String,Object> toPropertiesMap() {
        Map<String,Object> map = super.toPropertiesMap();
        setString(map,FIRST_NAME_FIELD,getFirstName());
        setString(map,LAST_NAME_FIELD,getLastName());
        setBoolean(map,IS_GROUP_ALIAS_FIELD,getIsGroupAlias());

        return map;
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n",FIRST_NAME_FIELD,getFirstName()));
        sb.append(String.format("   %s -> %s\n",LAST_NAME_FIELD,getLastName()));
        sb.append(String.format("   %s -> %s\n",IS_GROUP_ALIAS_FIELD,getIsGroupAlias()));
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Boolean getIsGroupAlias() {
        return isGroupAlias;
    }

    public void setIsGroupAlias(Boolean groupAlias) {
        isGroupAlias = groupAlias;
    }
}
