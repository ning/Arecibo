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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.ZipUtils;
import com.ning.arecibo.util.xml.XStreamUtils;
import com.thoughtworks.xstream.XStream;

public final class MonitoringEvent extends Event implements TransformableEvent
{
    public static final UUID FAKE_UUID = UUID.randomUUID() ;

    public static final String KEY_CONFIG_PATH = "deployedConfigSubPath";
    public static final String KEY_VERSION = "deployedVersion";
    public static final String KEY_TYPE = "deployedType";
    public static final String KEY_ENV = "deployedEnv";
    public static final String KEY_HOST = "hostName";
    public static final String KEY_MAP = "attrMap";

    static final Logger log = Logger.getLogger(MonitoringEvent.class);
	static final long serialVersionUID = 864783057346699991L;

	private String hostName;
	private String eventType;
	private String deployedEnv;
	private String deployedVersion;
	private String deployedType;
	private String deployedConfigSubPath;

	final HashMap<String, Object> keyvalues;

	public MonitoringEvent(Event event)
	{
		super(
			event.getTimestamp(),
			event.getEventType() == null ? event.getClass().getSimpleName() : event.getEventType(),
			event.getSourceUUID() == null ? FAKE_UUID : event.getSourceUUID()
		);

		if ( event instanceof MapEvent ) {
			MapEvent mapEvent = (MapEvent) event ;
			HashMap<String, Object> from = new HashMap<String, Object>(mapEvent.getMap());
			this.eventType = mapEvent.getEventType() ;
			this.deployedConfigSubPath = (String) from.get(KEY_CONFIG_PATH);
			this.deployedVersion = (String) from.get(KEY_VERSION);
			this.deployedType = (String) from.get(KEY_TYPE);
			this.deployedEnv = (String) from.get(KEY_ENV);
			this.hostName = (String) from.get(KEY_HOST);
			from.remove(KEY_HOST);
			from.remove(KEY_CONFIG_PATH);
			from.remove(KEY_VERSION);
			from.remove(KEY_TYPE);
			from.remove(KEY_HOST);
			this.keyvalues = from ;
		}
		else {
			keyvalues = new HashMap<String, Object>();
		}
		
		removeAtomicValues();
	}

	public MonitoringEvent(long ts, String eventType, Map<String, Object> keyvalues)
    {
        super(
                ts,
                eventType,
                FAKE_UUID
        );

        this.eventType = eventType;
        this.hostName = (String) keyvalues.get(KEY_HOST);
        this.deployedConfigSubPath = (String) keyvalues.get(KEY_CONFIG_PATH);
        this.deployedVersion = (String) keyvalues.get(KEY_VERSION);
        this.deployedType = (String) keyvalues.get(KEY_TYPE);
        this.deployedEnv = (String) keyvalues.get(KEY_ENV);

        this.keyvalues = new HashMap<String,Object>(keyvalues);
        keyvalues.remove(KEY_HOST);
        keyvalues.remove(KEY_CONFIG_PATH);
        keyvalues.remove(KEY_VERSION);
        keyvalues.remove(KEY_TYPE);
        keyvalues.remove(KEY_HOST);

        removeAtomicValues();
    }

    public MonitoringEvent(long ts, String eventType, UUID uuid, String hostName, String deployedEnv, String deployedVersion, String deployedType, String deployedConfigSubPath, Map<String, Object> keyvalues)
	{
		super(
                ts,
                eventType,
                uuid == null ? FAKE_UUID : uuid
        );

		this.eventType = eventType;
        this.hostName = hostName;
		this.deployedEnv = deployedEnv;
		this.deployedVersion = deployedVersion;
		this.deployedType = deployedType;
		this.deployedConfigSubPath = deployedConfigSubPath;
		this.keyvalues = new HashMap<String, Object>(keyvalues);
		
		removeAtomicValues();
	}
	
	/**
	 * Esper doesn't like AtomicLong/AtomicIntegers, they really shouldn't be getting here in the first place, but...
	 */
	private void removeAtomicValues() {
	    if(this.keyvalues == null)
	        return;
	    
	    List<String> atomicKeys = null;
	    Set<String> keys = this.keyvalues.keySet();
	    for(String key:keys) {
	        Object value = this.keyvalues.get(key);
	        if(value instanceof AtomicLong || value instanceof AtomicInteger) {
	            if(atomicKeys == null)
	                atomicKeys = new ArrayList<String>();
	            atomicKeys.add(key);
	        }
	    }
	    
	    if(atomicKeys == null)
	        return;
	    
	    for(String atomicKey:atomicKeys) {
	        Object value = this.keyvalues.get(atomicKey);
	        if(value instanceof AtomicLong) {
                value = Long.valueOf(((AtomicLong)value).get());
	        }
	        else if(value instanceof AtomicInteger) {
	            value = Integer.valueOf(((AtomicInteger)value).get());
	        }
	        
	        this.keyvalues.put(atomicKey, value);
	    }
	}

    /**
	 * return the keys used in the key/value map
	 */
	public Set<String> getKeys()
	{
		return this.keyvalues.keySet();
	}

	/**
	 * return the value for a given attribute. In Esper, the map will be referred to as "value('key')"
	 *
	 * @deprecated
	 */
	@Deprecated
	public Double getValue(String key)
	{
		Number n = getNumber(key);
		if (n != null) {
			return n.doubleValue();
		}
		return null;
	}

	public Object getObject(String key)
	{
		return keyvalues.get(key);
	}

	public Map<String, Object> toMap()
	{
		HashMap<String, Object> clone = (HashMap<String, Object>) keyvalues.clone();
		clone.put(KEY_CONFIG_PATH, this.deployedConfigSubPath);
		clone.put(KEY_ENV, this.deployedEnv);
		clone.put(KEY_TYPE, this.deployedType);
		clone.put(KEY_VERSION, this.deployedVersion);
		clone.put(KEY_HOST, this.hostName);
		clone.put(MapEvent.KEY_EVENT_NAME, this.getEventType());
		clone.put(MapEvent.KEY_TIMESTAMP, this.getTimestamp());
		clone.put(MapEvent.KEY_UUID, this.getSourceUUID());
		return clone ;
	}

    public Map<String, Object> getMap()
    {
        return keyvalues;
    }

	public Number getNumber(String key)
	{
		Object o = keyvalues.get(key);
		if (o instanceof Number) {
			return (Number) o;
		}
		return null;
	}

	public String getString(String key)
	{
		Object o = keyvalues.get(key);
		if (o instanceof String) {
			return (String) o;
		}
		else {
			if (o != null) {
				return o.toString();
			}
		}
		return null;
	}

	public String getHostName()
	{
		return hostName;
	}

	public String getEventType()
	{
		return eventType;
	}

	public String getDeployedEnv()
	{
		return deployedEnv;
	}

	public String getDeployedVersion()
	{
		return deployedVersion;
	}

	public String getDeployedType()
	{
		return deployedType;
	}

	public String getDeployedConfigSubPath()
	{
		return deployedConfigSubPath;
	}

	public String toBase64String() throws IOException
	{
		return new String(Base64.encodeBase64(ZipUtils.gzip(toXML().getBytes())));
	}

	public String toXML()
	{
		XStream xs = XStreamUtils.getXStreamNoStringCache();
		return xs.toXML(this);
	}

	public static MonitoringEvent fromXML(String xml)
	{
		XStream xs = XStreamUtils.getXStreamNoStringCache();
		return (MonitoringEvent) xs.fromXML(xml);
	}


	public static MonitoringEvent fromBase64String(String s) throws IOException, ClassNotFoundException
	{
		byte[] b64 = s.getBytes();
		byte[] buf = ZipUtils.gunzip(Base64.decodeBase64(b64));
		return fromXML(new String(buf));
	}

	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append(KEY_HOST, hostName)
				.append(MapEvent.KEY_EVENT_NAME, eventType)
				.append(KEY_ENV, deployedEnv)
				.append(KEY_VERSION, deployedEnv)
				.append(KEY_TYPE, deployedType)
				.append(KEY_CONFIG_PATH, deployedConfigSubPath)
				.append(KEY_MAP, keyvalues)
				.toString();
	}

    public static boolean looksLikeME(MapEvent event)
    {
        return event.getEventType() != null && event.getSourceUUID() != null
                 &&  event.getObject(KEY_TYPE) != null
			     &&  event.getObject(KEY_ENV) != null 
			     &&  event.getObject(KEY_HOST) != null;
    }
}
