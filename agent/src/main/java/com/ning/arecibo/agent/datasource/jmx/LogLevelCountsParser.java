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
