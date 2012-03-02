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

package com.ning.arecibo.aggregator.dictionary;

import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.antlr.stringtemplate.StringTemplate;
import com.espertech.esper.client.EventType;
import com.ning.arecibo.aggregator.stringtemplates.StringTemplates;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;

public class EventDefinition implements Serializable
{
	final String eventType;
	final SortedMap<String, Class> properties;
	final Class evtClass;
	final Class sourceEventClass;
	final boolean isGenerated ;

    public EventDefinition(Object sourceEvent, String eventType, Class<? extends Event> evtClass, SortedMap<String, Class> properties)
	{
		this(sourceEvent.getClass(), eventType,evtClass,properties);
	}

	public EventDefinition(Class sourceEventClass, String eventType, Class<? extends Event> evtClass, SortedMap<String, Class> properties)
	{
		this.sourceEventClass = sourceEventClass;
		this.eventType = eventType;
		this.evtClass = evtClass;
		this.properties = properties;
		this.isGenerated = true ;
	}

	public EventDefinition(Object sourceEvent, String eventType, Class<? extends Event> evtClass)
	{
		this.sourceEventClass = sourceEvent.getClass();
		this.eventType = eventType;
		this.evtClass = evtClass;
		this.properties = getPropertiesViaReflection(evtClass);
		this.isGenerated = false ;
	}

	public EventDefinition(String outputEvent, EventType eventType)
	{
		this.sourceEventClass = MapEvent.class ;
		this.eventType = outputEvent ;
		this.evtClass = MapEvent.class ;
		this.properties = getPropertiesFromEventType(eventType);
		this.isGenerated = false ;
	}

	private SortedMap<String, Class> getPropertiesFromEventType(EventType eventType)
	{
		TreeMap<String, Class> tree = new TreeMap<String, Class>();
		for ( String name : eventType.getPropertyNames() ) {
			tree.put(name, eventType.getPropertyType(name));
		}
		return tree ;
	}

	private SortedMap<String, Class> getPropertiesViaReflection(Class<? extends Event> clazz)
	{
		SortedMap<String, Class> map = new TreeMap<String, Class>();

		Method[] methods = clazz.getMethods();
		for ( Method m : methods ) {
			if (m.getName().startsWith("get")) {
				String property = EventDictionary.toLowerCamelCase(m.getName().substring("get".length()));
				map.put(property, m.getReturnType());
			}
		}		
		return map ;
	}

	public boolean isGeneratedClass()
	{
		return this.isGenerated ;
	}

	public String getEventType()
	{
		return eventType;
	}

	public Class getEvtClass()
	{
		return evtClass;
	}

    public SortedMap<String, Class> getProperties()
    {
        return properties;
    }

	public void addProperties(SortedMap<String,Class> updateProperties)
	{
        this.properties.putAll(updateProperties);
	}

	public Class getSourceEventClass()
	{
		return sourceEventClass;
	}

	public void renderDebugText(PrintWriter pw)
	{
		pw.printf("\n\nEvent %s extends %s :\n", eventType, sourceEventClass.getSimpleName());
		for (Map.Entry<String, Class> entry : properties.entrySet()) {
			pw.printf("\t%s : %s\n", entry.getKey(), entry.getValue().getName());
		}
	}

	public void renderHtml(PrintWriter pw)
	{
		StringTemplate st = StringTemplates.getTemplate("eventDef");
		st.setAttribute("def", this);
		st.setAttribute("properties", properties.entrySet());
		pw.println(st);
	}
}

