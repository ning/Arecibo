package com.ning.arecibo.alert.confdata.objects;

import java.sql.Timestamp;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

public abstract class ConfDataObject implements Serializable
{
    // required methods to be implemented
    public abstract String getTypeName();
    public abstract String getInsertSqlTemplateName();
    public abstract String getUpdateSqlTemplateName();

    private final static String ID_FIELD = "id";
    private final static String LABEL_FIELD = "label";
    private final static String CREATE_TIMESTAMP_FIELD = "create_timestamp";
    private final static String UPDATE_TIMESTAMP_FIELD = "update_timestamp";

    // instance variables
    protected volatile Long id = null;
    protected volatile String label = null;
    protected volatile Timestamp createTimestamp = null;
    protected volatile Timestamp updateTimestamp = null;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Timestamp getCreateTimestamp() {
        return this.createTimestamp;
    }

    public void setCreateTimestamp(Timestamp createTimestamp) {
        this.createTimestamp = createTimestamp;
    }

    public Timestamp getUpdateTimestamp() {
        return this.updateTimestamp;
    }

    public void setUpdateTimestamp(Timestamp updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    public Timestamp getNewUpdateTimestamp() {
        setUpdateTimestamp(new Timestamp(System.currentTimeMillis()));
        return getUpdateTimestamp();
    }

    public void setPropertiesFromMap(Map<String,Object> map) {
        // implementation classes should first call this via super(),
        // then add in their own fields
        this.id = getLong(map,ID_FIELD);
        this.label = getString(map,LABEL_FIELD);
        this.createTimestamp = getTimestamp(map,CREATE_TIMESTAMP_FIELD);
        this.updateTimestamp = getTimestamp(map,UPDATE_TIMESTAMP_FIELD);
    }

    public Map<String,Object> toPropertiesMap() {
        // implementation classes should first call this via super(),
        // then add in their own fields
        Map<String,Object> map = new HashMap<String,Object>();
        setLong(map,ID_FIELD,this.id);
        setString(map,LABEL_FIELD,this.label);
        setTimestamp(map,CREATE_TIMESTAMP_FIELD,this.createTimestamp);
        setTimestamp(map,UPDATE_TIMESTAMP_FIELD,this.updateTimestamp);

        return map;
    }

    public void toStringBuilder(StringBuilder sb) {
        // implementation classes should first call this via super(),
        // then add in their own fields
        sb.append(String.format("\n%s -> %s \n",ID_FIELD,getId()));
        sb.append(String.format("   %s -> %s\n",LABEL_FIELD,getLabel()));
        sb.append(String.format("   %s -> %s\n",CREATE_TIMESTAMP_FIELD,getCreateTimestamp()));
        sb.append(String.format("   %s -> %s\n",UPDATE_TIMESTAMP_FIELD,getUpdateTimestamp()));
    }

    public boolean equals(ConfDataObject compObj) {
        
        if(compObj == null)
            return false;

        if(compObj.getClass() != this.getClass())
            return false;

        return this.toPropertiesMap().equals(compObj.toPropertiesMap());
    }

    public boolean dataMatchesFilterObject(ConfDataObject filterObj) {
        // should override if needed
        // do an expensive default comparison
        // compare all fields converted to String, case insensitive,
        // compared on a substring matching basis only fields that
        // are non-null in the filter matter

        if(filterObj == null)
            return true;

        Map<String,Object> filterMap = filterObj.toPropertiesMap();
        if(filterMap.size() == 0)
            return true;

        Map<String,Object> dataMap = this.toPropertiesMap();

        return checkFilterMatch(dataMap,filterMap);
    }

    public boolean filterMatchesDataObject(ConfDataObject dataObject) {
        // should override if needed
        // do an expensive default comparison
        // compare all fields converted to String, case insensitive,
        // compared on a substring matching basis only fields that
        // are non-null in the filter matter
        Map<String, Object> filterMap = this.toPropertiesMap();
        if (filterMap.size() == 0)
            return true;

        if (dataObject == null)
            return false;

        Map<String, Object> dataMap = dataObject.toPropertiesMap();

        return checkFilterMatch(dataMap,filterMap);
    }

    protected static boolean checkFilterMatch(Map<String, Object>dataMap,Map<String, Object>filterMap) {
        for(String key:filterMap.keySet()) {
            Object filterValue = filterMap.get(key);
            if(filterValue != null) {

                Object dataValue = dataMap.get(key);
                if(!checkFilterMatch(dataValue,filterValue))
                    return false;
            }
        }

        return true;
    }

    protected static boolean checkFilterMatch(Object dataObject,Object filterObject) {

        if(filterObject != null) {
            if(dataObject == null)
                return false;
            else
                return dataObject.toString().toLowerCase().contains(filterObject.toString().toLowerCase());
        }

        return true;
    }

    public void copyPropertiesMap(ConfDataObject copyObj) {
        this.setPropertiesFromMap(copyObj.toPropertiesMap());
    }

    // protected util methods, which do conversions, and handle nulls
    protected Long getLong(Map<String,Object> map,String key) {
        Object obj = map.get(key);
        if(obj == null || !(obj instanceof Number))
            return null;

        return ((Number)obj).longValue();
    }

    protected void setLong(Map<String,Object> map,String key,Long value) {
        map.put(key,value);
    }

    protected Double getDouble(Map<String,Object> map,String key) {
        Object obj = map.get(key);
        if(obj == null || !(obj instanceof Number))
            return null;

        return ((Number)obj).doubleValue();
    }

    protected void setDouble(Map<String,Object> map,String key,Double value) {
        map.put(key,value);
    }

    protected String getString(Map<String,Object> map,String key) {
        return (String)map.get(key);
    }

    protected void setString(Map<String,Object> map,String key,String value) {
        map.put(key,value);
    }

    protected Timestamp getTimestamp(Map<String,Object> map,String key) {
        return (Timestamp)map.get(key);
    }

    protected void setTimestamp(Map<String,Object> map,String key,Timestamp value) {
        map.put(key,value);
    }

    protected Boolean getBoolean(Map<String,Object> map,String key) {
        Object obj = map.get(key);
        if(obj == null || !(obj instanceof String))
            return null;

        return (((String)obj).charAt(0) != '0');
    }

    protected void setBoolean(Map<String,Object> map,String key,Boolean value) {
        String stringValue;

        if(value == null)
            stringValue = null;
        else if(value)
            stringValue = "1";
        else
            stringValue = "0";

        map.put(key,stringValue);
    }

    protected <T extends Enum<T>> T getEnum(Map<String,Object> map,String key,Class<T> clazz) {
        String obj = (String)map.get(key);

        if(obj == null)
            return null;

        return T.valueOf(clazz,obj);
    }

    protected <T extends Enum<T>> void setEnum(Map<String,Object> map,String key,T value) {
        if(value == null) {
            map.put(key,null);
        }
        else {
            String valueObj = value.toString();
            map.put(key,valueObj);
        }
    }
}
