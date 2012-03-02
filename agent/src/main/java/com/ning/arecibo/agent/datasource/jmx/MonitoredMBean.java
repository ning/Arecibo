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

package com.ning.arecibo.agent.datasource.jmx;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.ning.arecibo.agent.datasource.ValueParser;
import com.ning.arecibo.agent.datasource.jmx.JMXClient.MBeanDescriptor;

public class MonitoredMBean
{
    private final String                   name;
    private final MBeanDescriptor          mbeanDescriptor;
    private final Object                   memberId;
    private final Map<String, ValueParser> valueParsers;
    private Set<String>                    relevantAttrs;

    public MonitoredMBean(String name, MBeanDescriptor mbeanDescriptor, Object memberId)
    {
        this.name            = name;
        this.mbeanDescriptor = mbeanDescriptor;
        this.memberId        = memberId;
        this.relevantAttrs   = new LinkedHashSet<String>();
        this.valueParsers    = new HashMap<String, ValueParser>();
        setRelevantAttributes(mbeanDescriptor.getAttributeNames());
    }

    public String getName()
    {
        return name;
    }

    public MBeanDescriptor getMBeanDescriptor()
    {
        return mbeanDescriptor;
    }

    public Object getMemberId()
    {
        return memberId;
    }

    public String[] getRelevantAttributes()
    {
        return relevantAttrs.toArray(new String[relevantAttrs.size()]);
    }

    public boolean isRelevantAttribute(String name)
    {
        return relevantAttrs.contains(name);
    }

	/**
	 * Set the attributes that we're interested in. Note that the clear() statement will delete the possibly large
	 * default set, so it's important to set the attributes this way.
	 */
	public void setRelevantAttributes(String... relevantAttrs)
	{
	    this.relevantAttrs.clear();
	    this.relevantAttrs.addAll(Arrays.asList(relevantAttrs));
	}

    public void addValueParser(String attrName, ValueParser parser)
    {
        valueParsers.put(attrName, parser);
    }

    public ValueParser getValueParser(String attrName)
    {
        return valueParsers.get(attrName);
    }
}
