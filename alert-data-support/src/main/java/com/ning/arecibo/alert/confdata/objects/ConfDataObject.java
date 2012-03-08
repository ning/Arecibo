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

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public abstract class ConfDataObject implements Serializable
{
    /**
     * @return table name for this object
     */
    public abstract String getTypeName();

    private static final String ID_FIELD = "id";
    private static final String LABEL_FIELD = "label";
    private static final String CREATE_TIMESTAMP_FIELD = "create_timestamp";
    private static final String UPDATE_TIMESTAMP_FIELD = "update_timestamp";

    protected volatile Long id = null;
    protected volatile String label = null;
    protected volatile Timestamp createTimestamp = null;
    protected volatile Timestamp updateTimestamp = null;

    public Long getId()
    {
        return this.id;
    }

    public void setId(final Long id)
    {
        this.id = id;
    }

    public String getLabel()
    {
        return this.label;
    }

    public void setLabel(final String label)
    {
        this.label = label;
    }

    public Timestamp getCreateTimestamp()
    {
        return this.createTimestamp;
    }

    public void setCreateTimestamp(final Timestamp createTimestamp)
    {
        this.createTimestamp = createTimestamp;
    }

    public Timestamp getUpdateTimestamp()
    {
        return this.updateTimestamp;
    }

    public void setUpdateTimestamp(final Timestamp updateTimestamp)
    {
        this.updateTimestamp = updateTimestamp;
    }

    public Timestamp getNewUpdateTimestamp()
    {
        setUpdateTimestamp(new Timestamp(System.currentTimeMillis()));
        return getUpdateTimestamp();
    }

    public void populatePropertiesFromMap(final Map<String, Object> map)
    {
        // implementation classes should first call this via super(),
        // then add in their own fields
        this.id = getLong(map, ID_FIELD);
        this.label = getString(map, LABEL_FIELD);
        this.createTimestamp = getTimestamp(map, CREATE_TIMESTAMP_FIELD);
        this.updateTimestamp = getTimestamp(map, UPDATE_TIMESTAMP_FIELD);
    }

    public Map<String, Object> toPropertiesMap()
    {
        // implementation classes should first call this via super(),
        // then add in their own fields
        final Map<String, Object> map = new HashMap<String, Object>();
        setLong(map, ID_FIELD, this.id);
        setString(map, LABEL_FIELD, this.label);
        setTimestamp(map, CREATE_TIMESTAMP_FIELD, this.createTimestamp);
        setTimestamp(map, UPDATE_TIMESTAMP_FIELD, this.updateTimestamp);

        return map;
    }

    public void toStringBuilder(final StringBuilder sb)
    {
        // implementation classes should first call this via super(),
        // then add in their own fields
        sb.append(String.format("\n%s -> %s \n", ID_FIELD, getId()));
        sb.append(String.format("   %s -> %s\n", LABEL_FIELD, getLabel()));
        sb.append(String.format("   %s -> %s\n", CREATE_TIMESTAMP_FIELD, getCreateTimestamp()));
        sb.append(String.format("   %s -> %s\n", UPDATE_TIMESTAMP_FIELD, getUpdateTimestamp()));
    }

    public boolean equals(final ConfDataObject compObj)
    {
        return compObj != null && compObj.getClass() == this.getClass() && this.toPropertiesMap().equals(compObj.toPropertiesMap());
    }

    public boolean filterMatchesDataObject(final ConfDataObject dataObject)
    {
        // should override if needed
        // do an expensive default comparison
        // compare all fields converted to String, case insensitive,
        // compared on a substring matching basis only fields that
        // are non-null in the filter matter
        final Map<String, Object> filterMap = this.toPropertiesMap();
        if (filterMap.size() == 0) {
            return true;
        }

        if (dataObject == null) {
            return false;
        }

        final Map<String, Object> dataMap = dataObject.toPropertiesMap();

        return checkFilterMatch(dataMap, filterMap);
    }

    protected static boolean checkFilterMatch(final Map<String, Object> dataMap, final Map<String, Object> filterMap)
    {
        for (final String key : filterMap.keySet()) {
            final Object filterValue = filterMap.get(key);
            if (filterValue != null) {

                final Object dataValue = dataMap.get(key);
                if (!checkFilterMatch(dataValue, filterValue)) {
                    return false;
                }
            }
        }

        return true;
    }

    protected static boolean checkFilterMatch(final Object dataObject, final Object filterObject)
    {
        return filterObject == null || dataObject != null && dataObject.toString().toLowerCase().contains(filterObject.toString().toLowerCase());
    }

    // protected util methods, which do conversions, and handle nulls
    protected Long getLong(final Map<String, Object> map, final String key)
    {
        final Object obj = map.get(key);
        if (obj == null || !(obj instanceof Number)) {
            return null;
        }

        return ((Number) obj).longValue();
    }

    protected void setLong(final Map<String, Object> map, final String key, final Long value)
    {
        map.put(key, value);
    }

    protected Double getDouble(final Map<String, Object> map, final String key)
    {
        final Object obj = map.get(key);
        if (obj == null || !(obj instanceof Number)) {
            return null;
        }

        return ((Number) obj).doubleValue();
    }

    protected void setDouble(final Map<String, Object> map, final String key, final Double value)
    {
        map.put(key, value);
    }

    protected String getString(final Map<String, Object> map, final String key)
    {
        return (String) map.get(key);
    }

    protected void setString(final Map<String, Object> map, final String key, final String value)
    {
        map.put(key, value);
    }

    protected Timestamp getTimestamp(final Map<String, Object> map, final String key)
    {
        return (Timestamp) map.get(key);
    }

    protected void setTimestamp(final Map<String, Object> map, final String key, final Timestamp value)
    {
        map.put(key, value);
    }

    protected Boolean getBoolean(final Map<String, Object> map, final String key)
    {
        final Object obj = map.get(key);
        if (obj == null || !(obj instanceof String)) {
            return null;
        }

        return (((String) obj).charAt(0) != '0');
    }

    protected void setBoolean(final Map<String, Object> map, final String key, final Boolean value)
    {
        final String stringValue;

        if (value == null) {
            stringValue = null;
        }
        else if (value) {
            stringValue = "1";
        }
        else {
            stringValue = "0";
        }

        map.put(key, stringValue);
    }

    protected <T extends Enum<T>> T getEnum(final Map<String, Object> map, final String key, final Class<T> clazz)
    {
        final String obj = (String) map.get(key);

        if (obj == null) {
            return null;
        }

        return T.valueOf(clazz, obj);
    }

    protected <T extends Enum<T>> void setEnum(final Map<String, Object> map, final String key, final T value)
    {
        if (value == null) {
            map.put(key, null);
        }
        else {
            final String valueObj = value.toString();
            map.put(key, valueObj);
        }
    }
}
