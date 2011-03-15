package com.ning.arecibo.eventlogger;

/**
 * An {@link EventKeyValueExtractor} maps {@link Event}s to values suitable for
 * serialization as key/value pairs.
 */
public interface EventKeyValueExtractor<K,V> {

	public K extractKey(Event event);
	public V extractValue(Event event);

}
