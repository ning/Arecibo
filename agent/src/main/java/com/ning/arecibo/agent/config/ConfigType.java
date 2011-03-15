package com.ning.arecibo.agent.config;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public enum ConfigType {

	JMX,
	SNMP,
    TRACER,
    HTTP,
    TCP,

    HEALTH_CHECK_TYPES_ONLY(
            HTTP,
            TCP),

    NON_HEALTH_CHECK_TYPES_ONLY(
            JMX,
            SNMP,
            TRACER),
    
    ALL(
            JMX,
            SNMP,
            TRACER,
            HTTP,
            TCP),

    EXCLUSION;

    private final List<ConfigType> subTypes;

    private ConfigType(ConfigType ... configTypes) {
        this.subTypes = new ArrayList<ConfigType>(Arrays.asList(configTypes));
    }

    public List<ConfigType> getSubTypes() {
        return this.subTypes;
    }
}
