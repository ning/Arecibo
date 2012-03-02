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

package com.ning.arecibo.event;

import com.espertech.esper.client.EventBean;
import com.ning.arecibo.eventlogger.Event;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MapEvent extends Event implements TransformableEvent
{
    static final long serialVersionUID = 2908927541122355973L;

    public static final UUID FAKE_UUID = UUID.randomUUID();
    public static final String KEY_EVENT_NAME = "eventType";
    public static final String KEY_UUID = "sourceUUID";
    public static final String KEY_TIMESTAMP = "timestamp";

    private long ts;
    private String type;
    private UUID uuid;
    private HashMap<String, Object> map;

    public MapEvent(Event event)
    {
        super(event.getTimestamp(), event.getEventType(), event.getSourceUUID());
        this.type = event.getEventType();
        this.uuid = event.getSourceUUID();
        this.ts = event.getTimestamp();
        this.map = new HashMap<String, Object>();
        copyEvent(event);
    }

    private void copyEvent(Event event)
    {
        Method[] methods = event.getClass().getMethods();
        Object[] args = new Object[0];
        for (Method m : methods) {
            if (m.getName().startsWith("get")) {
                String field = m.getName().substring(3);
                field = field.substring(0, 1).toLowerCase() + field.substring(1);
                if (!(field.equals(KEY_TIMESTAMP) || field.equals(KEY_UUID) || field.equals(KEY_EVENT_NAME))) {
                    try {
                        map.put(field, m.invoke(event, args));
                    }
                    catch (IllegalAccessException e) {
                    }
                    catch (InvocationTargetException e) {
                    }
                }
            }
        }
    }

    public MapEvent(long ts, String type, UUID uuid, Map<String, Object> map)
    {
        super(-1, MapEvent.class.getSimpleName(), FAKE_UUID);
        this.type = type;
        this.uuid = uuid;
        this.ts = ts;
        this.map = new HashMap<String, Object>(map);
    }

    @JsonCreator
    public MapEvent(Map<String, Object> map)
    {
        super(-1, MapEvent.class.getSimpleName(), FAKE_UUID);
        this.map = new HashMap<String, Object>(map);
        this.type = (String) map.get(KEY_EVENT_NAME);
        this.uuid = UUID.fromString((String) map.get(KEY_UUID));
        Long time = (Long) map.get(KEY_TIMESTAMP);
        if (time == null) {
            this.ts = System.currentTimeMillis();
        }
        else {
            this.ts = time;
        }
        this.map.remove(KEY_UUID);
        this.map.remove(KEY_EVENT_NAME);
        this.map.remove(KEY_TIMESTAMP);
    }

    public void setUuid(UUID uuid)
    {
        this.uuid = uuid;
    }

    public String getEventType()
    {
        return type;
    }

    public UUID getSourceUUID()
    {
        return uuid;
    }

    public long getTimestamp()
    {
        return ts;
    }

    public Object getValue(String key)
    {
        return map.get(key);
    }

    public Set<String> getKeys()
    {
        return map.keySet();
    }

    public Object getObject(String name)
    {
        return map.get(name);
    }

    @JsonValue
    public Map<String, Object> toMap()
    {
        HashMap<String, Object> clone = (HashMap<String, Object>) map.clone();
        clone.put(KEY_EVENT_NAME, this.getEventType());
        clone.put(KEY_TIMESTAMP, this.getTimestamp());
        clone.put(KEY_UUID, this.getSourceUUID());
        return clone;
    }

    public Map<String, Object> getMap()
    {
        return map;
    }

    public String toString()
    {
        if (map == null) {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        for (Object o : map.values()) {
            sb.append(o).append(", ");
        }
        return sb.toString();
    }

    public static MapEvent fromEventBean(EventBean event, String eventName)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        for (String name : event.getEventType().getPropertyNames()) {
            map.put(name, event.get(name));
        }
        map.put(MapEvent.KEY_EVENT_NAME, eventName);
        return new MapEvent(map);
    }
}
