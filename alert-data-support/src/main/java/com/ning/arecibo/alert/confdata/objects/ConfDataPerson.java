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

import com.ning.arecibo.util.Logger;

import java.util.Map;

public class ConfDataPerson extends ConfDataObject
{
    private static final Logger log = Logger.getLogger(ConfDataPerson.class);

    public static final String TYPE_NAME = "person";
    public static final String INSERT_TEMPLATE_NAME = ":insert_person";
    public static final String UPDATE_TEMPLATE_NAME = ":update_person";

    private static final String FIRST_NAME_FIELD = "first_name";
    private static final String LAST_NAME_FIELD = "last_name";
    private static final String IS_GROUP_ALIAS_FIELD = "is_group_alias";

    protected volatile String firstName = null;
    protected volatile String lastName = null;
    protected volatile Boolean isGroupAlias = null;

    public ConfDataPerson()
    {
    }

    @Override
    public String getTypeName()
    {
        return TYPE_NAME;
    }

    @Override
    public String getInsertSqlTemplateName()
    {
        return INSERT_TEMPLATE_NAME;
    }

    @Override
    public String getUpdateSqlTemplateName()
    {
        return UPDATE_TEMPLATE_NAME;
    }

    @Override
    public void setPropertiesFromMap(final Map<String, Object> map)
    {
        super.setPropertiesFromMap(map);
        setFirstName(getString(map, FIRST_NAME_FIELD));
        setLastName(getString(map, LAST_NAME_FIELD));
        setIsGroupAlias(getBoolean(map, IS_GROUP_ALIAS_FIELD));
    }

    @Override
    public Map<String, Object> toPropertiesMap()
    {
        final Map<String, Object> map = super.toPropertiesMap();
        setString(map, FIRST_NAME_FIELD, getFirstName());
        setString(map, LAST_NAME_FIELD, getLastName());
        setBoolean(map, IS_GROUP_ALIAS_FIELD, getIsGroupAlias());

        return map;
    }

    @Override
    public void toStringBuilder(final StringBuilder sb)
    {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", FIRST_NAME_FIELD, getFirstName()));
        sb.append(String.format("   %s -> %s\n", LAST_NAME_FIELD, getLastName()));
        sb.append(String.format("   %s -> %s\n", IS_GROUP_ALIAS_FIELD, getIsGroupAlias()));
    }

    public String getFirstName()
    {
        return this.firstName;
    }

    public void setFirstName(final String firstName)
    {
        this.firstName = firstName;
    }

    public String getLastName()
    {
        return this.lastName;
    }

    public void setLastName(final String lastName)
    {
        this.lastName = lastName;
    }

    public Boolean getIsGroupAlias()
    {
        return isGroupAlias;
    }

    public void setIsGroupAlias(final Boolean groupAlias)
    {
        isGroupAlias = groupAlias;
    }
}
