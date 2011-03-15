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
                0,
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
