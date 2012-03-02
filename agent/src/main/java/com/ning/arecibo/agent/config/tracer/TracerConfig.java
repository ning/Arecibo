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

package com.ning.arecibo.agent.config.tracer;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.skife.config.TimeSpan;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.config.ConfigType;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;

public final class TracerConfig extends Config
{
    public final static String METHOD_TYPE = "methodType";

    protected final String methodType;

    public TracerConfig(String host,String fullConfigPath, String deployedType,
                      GuiceDefaultsForDataSources guiceDefaults,
                      Map<String,Object> optionsMap) throws ConfigException
    {
        super((String) optionsMap.get(Config.EVENT_TYPE),
                (String) optionsMap.get(Config.EVENT_ATTRIBUTE_TYPE),
                Config.getMonitoringTypesFromObjectList((List) optionsMap.get(Config.MONITORING_TYPES)),
                optionsMap.get(Config.POLLING_INTERVAL_SECS) != null?
                        new TimeSpan(optionsMap.get(Config.POLLING_INTERVAL_SECS).toString()) :
                        guiceDefaults.getDefaultPollingInterval(),
                host,
                fullConfigPath,
                deployedType);

        this.methodType = (String) optionsMap.get(TracerConfig.METHOD_TYPE);

        if(this.methodType == null)
            throw new ConfigException("methodType cannot be null");
    }

    public String getMethodType() {
        return methodType;
    }

    @Override
    public String getConfigDescriptor() {

        return super.getConfigDescriptor() + ":" + this.methodType;
    }

    @Override
    public String getConfigHashKey() {

        return this.methodType + super.getConfigHashKey();
    }

    @Override
    public boolean equalsConfig(Config cmpConfig) {

        if (cmpConfig == null || !(cmpConfig instanceof TracerConfig))
            return false;

        if (!super.equalsConfig(cmpConfig))
            return false;

        if (!StringUtils.equals(this.methodType, ((TracerConfig) cmpConfig).methodType))
            return false;

        return true;
    }

    @Override
    public ConfigType getConfigType() {
        return ConfigType.TRACER;
    }
}
