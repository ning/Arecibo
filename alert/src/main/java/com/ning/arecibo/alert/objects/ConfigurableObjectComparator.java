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

package com.ning.arecibo.alert.objects;

import java.util.Comparator;

public class ConfigurableObjectComparator implements Comparator<ConfigurableObject>
{
    private static final ConfigurableObjectComparator instance = new ConfigurableObjectComparator();

    public static ConfigurableObjectComparator getInstance()
    {
        return instance;
    }

    public int compare(ConfigurableObject tm1, ConfigurableObject tm2)
    {
        String id1 = tm1.getLabel();
        String id2 = tm2.getLabel();

        return id1.compareTo(id2);
    }

    public boolean equals(ConfigurableObject tm1, ConfigurableObject tm2)
    {
        String id1 = tm1.getLabel();
        String id2 = tm2.getLabel();

        return id1.equals(id2);
    }
}
