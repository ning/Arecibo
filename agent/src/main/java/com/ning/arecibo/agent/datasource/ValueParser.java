package com.ning.arecibo.agent.datasource;

import java.util.Map;
import com.ning.arecibo.agent.datasource.jmx.MonitoredMBean;



public interface ValueParser
{
    public Map<String, Object> parse(MonitoredMBean mbean, String attrName, Object value);
}
