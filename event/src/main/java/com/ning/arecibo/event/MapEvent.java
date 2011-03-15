package com.ning.arecibo.event;

import com.espertech.esper.client.EventBean;
import com.ning.arecibo.eventlogger.Event;


import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 
 */
public class MapEvent extends Event implements TransformableEvent
{
	static final long serialVersionUID = 2908927541122355973L;
	
	public static final UUID FAKE_UUID = UUID.randomUUID() ;
	public static final String KEY_EVENT_NAME = "eventType";
	public static final String KEY_UUID = "sourceUUID" ;
	public static final String KEY_TIMESTAMP = "timestamp" ;

	private long ts;
	private String type;
	private UUID uuid;
	private HashMap<String, Object> map ;

	public MapEvent(Event event)
	{
		super(event.getTimestamp(), event.getEventType(), event.getSourceUUID());
		this.type = event.getEventType() ;
		this.uuid = event.getSourceUUID() ;
		this.ts = event.getTimestamp() ;
		this.map = new HashMap<String, Object>();
		copyEvent(event);
	}

	private void copyEvent(Event event)
	{
		Method[] methods = event.getClass().getMethods() ;
		Object[] args =  new Object[0] ;
		for ( Method m : methods ) {
			if ( m.getName().startsWith("get") ) {
				String field = m.getName().substring(3);
				field = field.substring(0, 1).toLowerCase() + field.substring(1);
				if ( !(field.equals(KEY_TIMESTAMP) || field.equals(KEY_UUID) || field.equals(KEY_EVENT_NAME)) ) {
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
		this.type = type ;
		this.uuid = uuid ;
		this.ts = ts ;
		this.map = new HashMap<String, Object>(map) ;
	}

	public MapEvent(Map<String, Object> map)
	{
		super(-1, MapEvent.class.getSimpleName(), FAKE_UUID);
		this.map = new HashMap<String, Object>(map) ;
		this.type = (String) map.get(KEY_EVENT_NAME);
		this.uuid = (UUID) map.get(KEY_UUID);
        Long time = (Long) map.get(KEY_TIMESTAMP);
        if ( time == null ) {
            this.ts = System.currentTimeMillis() ;
        }
        else {
            this.ts = time ;
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
		return type ;
	}

	public UUID getSourceUUID()
	{
		return uuid ;
	}

	public long getTimestamp()
	{
		return ts ;
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

	public Map<String, Object> toMap()
	{
		HashMap<String, Object> clone = (HashMap<String, Object>) map.clone();
		clone.put(KEY_EVENT_NAME, this.getEventType());
		clone.put(KEY_TIMESTAMP, this.getTimestamp());
		clone.put(KEY_UUID, this.getSourceUUID());
		return clone ;
	}

	public Map<String, Object> getMap()
	{
		return map;
	}

    public String toString()
	{
        StringBuffer sb = new StringBuffer();
        for (Object o : map.values()) {
            sb.append(o).append(", ");
        }
        return sb.toString();
    }

	public static MapEvent fromEventBean(EventBean event, String eventName)
    {
	    Map<String, Object> map = new HashMap<String, Object>();
	    for ( String name : event.getEventType().getPropertyNames() ) {
		    map.put(name, event.get(name));
	    }
	    map.put(MapEvent.KEY_EVENT_NAME, eventName);
        return new MapEvent(map);
    }

	public static MapEvent fromJSON(JSONObject json) throws JSONException
	{
        Map<String, Object> map = new HashMap<String, Object>();

        Iterator i = json.keys();
        while (i.hasNext()) {
            String key =  i.next().toString();
            map.put(key, json.get(key));
        }

        try {
            long timestamp = json.getLong(KEY_TIMESTAMP);
            map.put(KEY_TIMESTAMP, timestamp);
        }
        catch(JSONException jsonEx) {
            // this can be null
        }


        try {
            String uuidString = json.getString(KEY_UUID);
            UUID uuid;
            if(uuidString != null) {
                uuid = UUID.fromString(uuidString);
                map.put(KEY_UUID, uuid);
            }
        }
        catch(JSONException jsonEx) {
            // this can be null
        }

        return new MapEvent(map);
	}
}
