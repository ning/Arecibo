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
