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

import java.util.Map;
import com.ning.arecibo.agent.config.exclusion.ExclusionConfig;
import com.ning.arecibo.agent.config.http.HTTPResponseCheckConfig;
import com.ning.arecibo.agent.config.jmx.JMXConfig;
import com.ning.arecibo.agent.config.snmp.SNMPConfig;
import com.ning.arecibo.agent.config.tcp.TCPConnectCheckConfig;
import com.ning.arecibo.agent.config.tracer.TracerConfig;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;

import com.ning.arecibo.util.Logger;



public abstract class BaseConfigReader implements ConfigReader
{
	private static final Logger log = Logger.getLogger(BaseConfigReader.class);

	protected final String host;
	protected final String configPath;
	protected final String coreType;
    protected final GuiceDefaultsForDataSources guiceDefaults;

    public BaseConfigReader(String host, String configPath, String coreType, GuiceDefaultsForDataSources guiceDefaults) {
        this.host = host;
        this.configPath = configPath;
        this.coreType = coreType;
        this.guiceDefaults = guiceDefaults;
    }

    public Config createConfigFromMap(Map<String,Object> map) throws ConfigException {

        String type = (String)map.get("type");
        if(type == null) {
            log.warn("Config entry missing 'type'");
            return null;
        }

        try {
            ConfigType configType = ConfigType.valueOf(type.toUpperCase());

            if(guiceDefaults != null && !guiceDefaults.isConfigTypeEnabled(configType))
                return null;

            switch(configType) {
                case JMX:
                    return new JMXConfig(host,configPath,coreType,guiceDefaults,map);

                case SNMP:
                    return new SNMPConfig(host,configPath,coreType,guiceDefaults,map);

                case TRACER:
                    return new TracerConfig(host,configPath,coreType,guiceDefaults,map);

                case HTTP:
                    return new HTTPResponseCheckConfig(host,configPath,coreType,guiceDefaults,map);

                case TCP:
                    return new TCPConnectCheckConfig(host,configPath,coreType,guiceDefaults,map);

                case EXCLUSION:
                    return new ExclusionConfig(map);

                default:
                    throw new ConfigException("Unknown ConfigType " + configType);
            }
        }
        catch(ConfigException ex) {

            log.warn(ex);
            log.info("Config map entries that resulted in failure:");
            for (String key : map.keySet()) {
                    Object value = map.get(key);
                    log.info("\t%s = %s", key, value);
            }

            throw ex;
        }
    }
}
