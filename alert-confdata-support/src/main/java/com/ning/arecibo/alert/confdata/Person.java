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

package com.ning.arecibo.alert.confdata;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Person
{
    private final int id;
    private final String nickName;
    private final String firstName;
    private final String lastName;
    private final boolean isGroupAlias;

    @JsonCreator
    public Person(@JsonProperty("label") final String nickName,
                  @JsonProperty("first_name") final String firstName,
                  @JsonProperty("last_name") final String lastName,
                  @JsonProperty("is_group_alias") final String groupAlias,
                  @JsonProperty("id") final int id)
    {
        this.nickName = nickName;
        this.firstName = firstName;
        this.lastName = lastName;
        isGroupAlias = !(groupAlias != null && groupAlias.equals("0"));
        this.id = id;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public boolean isGroupAlias()
    {
        return isGroupAlias;
    }

    public int getId()
    {
        return id;
    }
}