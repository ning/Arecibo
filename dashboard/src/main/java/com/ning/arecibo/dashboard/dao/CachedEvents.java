package com.ning.arecibo.dashboard.dao;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.sql.Timestamp;
import java.lang.ref.SoftReference;
import com.ning.arecibo.event.MapEvent;

class CachedEvents
{
	private final String cacheKey;
	private final AtomicReference<SoftReference<List<MapEvent>>> dataRef = new AtomicReference<SoftReference<List<MapEvent>>>(null);
	private final CountDownLatch latch = new CountDownLatch(1);
	private final AtomicBoolean garbageCollected = new AtomicBoolean(false);

    CachedEvents(String cacheKey)
	{
		this.cacheKey = cacheKey;
	}

	public String getCacheKey()
	{
		return cacheKey;
	}

	public List<MapEvent> getEvents()
	{
        SoftReference<List<MapEvent>> softRef;
		if ((softRef = dataRef.get()) != null ) {
			List<MapEvent> events = softRef.get();
			if ( events == null ) {
				garbageCollected.set(true);
			}
			return events ;
		}
		return null;		
	}

	public void waitFor(long time, TimeUnit unit) throws InterruptedException
	{
		latch.await(time, unit);
	}

	public void setDataRef(List<MapEvent> events)
	{
		this.dataRef.set(new SoftReference<List<MapEvent>>(events));
		latch.countDown();
	}

    public boolean isDataRefGarbageCollected()
    {
        return garbageCollected.get();
    }

	public static List<Map<String, Object>> getValues(String key, List<MapEvent> events)
	{
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		for (MapEvent event : events) {
			Object value = event.getObject(key);
			if (isUsefulValue(value)) {
				list.add(convert(event, key, value));
			}
		}
		return list;
	}

	public static boolean isUsefulValue(Object value)
	{
		return value != null && !(value instanceof String) &&
				!value.equals(Double.NaN);
	}

	public static HashMap<String, Object> convert(MapEvent me, String key, Object value)
	{
		HashMap<String, Object> row = new HashMap<String, Object>();
		row.put("event", me.getEventType());
		if (me.getObject("host") != null) {
			row.put("host", me.getObject("hostName"));
		}
		if (me.getObject("deployedType") != null) {
			row.put("dep_type", me.getObject("deployedType"));
		}
		if (me.getObject("deployedConfigSubPath") != null) {
			row.put("dep_path", me.getObject("deployedConfigSubPath"));
		}
		row.put("ts", new Timestamp(me.getTimestamp()));
		row.put("attr", key);
		row.put("value", value);
		return row;
	}


}
