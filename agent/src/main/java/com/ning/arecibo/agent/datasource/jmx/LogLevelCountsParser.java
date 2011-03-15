package com.ning.arecibo.agent.datasource.jmx;

import com.ning.arecibo.util.Logger;

import java.util.Map;
import java.util.HashMap;
import com.ning.arecibo.agent.datasource.ValueParser;
import com.ning.arecibo.agent.datasource.jmx.MonitoredMBean;

public class LogLevelCountsParser implements ValueParser
{
    private static final Logger log = Logger.getLogger(LogLevelCountsParser.class);

	public Map<String, Object> parse(MonitoredMBean mbean, String attrName, Object value)
	{
		if (!(value instanceof String[])) {
            log.warn("Value not a string array for '%s'", value.toString());
            return null;
        }
        Map<String, Object> resMap = new HashMap<String, Object>();

        for (String string : (String[]) value) {
            String[] parts = string.split(":");
            if (parts.length != 2) {
                log.warn("Could not parse line '%s'", string);
            }
            String name = parts[0].trim();
            
            // esper doesn't like event types that aren't camel case....
            // we can assume these are single word values
            name = name.toLowerCase();
            
            Integer val = Integer.valueOf(parts[1].trim());
            resMap.put(name, val);
        }

        return resMap;
	}
}
