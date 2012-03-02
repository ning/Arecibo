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

package com.ning.arecibo.agent.config.exclusion;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.config.ConfigType;
import com.ning.arecibo.agent.config.jmx.JMXDynamicUtils;


public class ExclusionConfig extends Config {

    private final Pattern eventTypePattern;
    private final Pattern eventAttributeTypePattern;

    public ExclusionConfig(Map<String,Object> optionsMap) throws ConfigException {

        super((String) optionsMap.get(Config.EVENT_TYPE),
              (String) optionsMap.get(Config.EVENT_ATTRIBUTE_TYPE),
              null,
              null,
              null,
              null,
              null);

        String eventType = (String)optionsMap.get(Config.EVENT_TYPE);
        String eventAttributeType = (String)optionsMap.get(Config.EVENT_ATTRIBUTE_TYPE);

        if(eventType != null && eventType.length() > 0) {
            String eventTypeRE = eventType.replace(JMXDynamicUtils.wildCardDelim,"(.+)");
            eventTypePattern = Pattern.compile(eventTypeRE);
        }
        else {
            eventTypePattern = null;
        }

        if(eventAttributeType != null && eventAttributeType.length() > 0) {
            String eventAttributeTypeRE = eventAttributeType.replace(JMXDynamicUtils.wildCardDelim,"(.+)");
            eventAttributeTypePattern = Pattern.compile(eventAttributeTypeRE);
        }
        else {
            eventAttributeTypePattern = null;
        }
    }

    public boolean testForExclusion(Config config) {

        boolean matches = false;

        // test for eventType
        if(this.eventTypePattern != null) {

            Matcher matcher = eventTypePattern.matcher(config.getEventType());

            if(!matcher.matches()) {
                return false;
            }
            else {
                matches = true;
            }
        }

        // test for eventType
        if(this.eventAttributeTypePattern != null) {

            Matcher matcher = eventAttributeTypePattern.matcher(config.getEventAttributeType());

            if(!matcher.matches()) {
                return false;
            }
            else {
                matches = true;
            }
        }

        return matches;
    }

    @Override
    public ConfigType getConfigType() {
        return ConfigType.EXCLUSION;
    }
}
