package com.ning.arecibo.event;

import java.util.Set;
import java.util.Map;

public interface TransformableEvent
{
    public String getEventType() ;
    public Set<String> getKeys() ;
    Object getObject(String name);
	Map<String, Object> toMap();
}
