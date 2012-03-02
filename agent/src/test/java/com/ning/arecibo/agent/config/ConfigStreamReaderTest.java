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

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

import org.skife.config.ConfigurationObjectFactory;
import org.testng.annotations.Test;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.config.ConfigFileUtils;
import com.ning.arecibo.agent.config.ConfigStreamReader;
import com.ning.arecibo.agent.config.ConfigType;
import com.ning.arecibo.agent.guice.AgentConfig;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;
import com.ning.arecibo.util.Logger;

public class ConfigStreamReaderTest {

    private static final Logger log = Logger.getLogger(ConfigStreamReaderTest.class);

    private static String RESOURCE_DIR = "src/main/resources";

    @Test(groups = "fast")
    public void testLoadBundledMonitoringJSONConfigs() throws ConfigException {

        try {

            File resourceDir = new File(RESOURCE_DIR);
            if(!resourceDir.exists() || !resourceDir.isDirectory()) {
                throw new ConfigException("Couldn't open resource dir '" + RESOURCE_DIR + "'");
            }

            File[] files = resourceDir.listFiles();
            AgentConfig config = new ConfigurationObjectFactory(System.getProperties()).build(AgentConfig.class);
            for(File file:files) {
                String fileName = file.getName();
                int suffixIndex = fileName.lastIndexOf(ConfigFileUtils.CONFIG_FILE_SUFFIX);
                if(suffixIndex == -1) {
                    continue;
                }

                String monitoringType = fileName.substring(0,suffixIndex);
                Properties props = new Properties();

                props.put("arecibo.tools.coremonitor.config_types_enabled", ConfigType.ALL.toString() + "," + ConfigType.EXCLUSION);
                props.put("arecibo.tools.coremonitor.jmx_monitoring_profile_polling_enabled", "false");

                GuiceDefaultsForDataSources defaults = new GuiceDefaultsForDataSources(config);
                ConfigFileUtils configFileUtils = new ConfigFileUtils(defaults);

                List<InputStream> configStreams = configFileUtils.getMonitoringTypeConfigStreamList(monitoringType,monitoringType,false);
                for(InputStream configStream:configStreams) {
                    InputStreamReader reader = new InputStreamReader(configStream);
                    ConfigStreamReader testConfigReader = new ConfigStreamReader(reader,"testHost","/",monitoringType,defaults);
                }
            }

        }
        catch(ConfigException cEx) {
            // make it log the exception in the maven output
            log.warn(cEx);
            throw(cEx);
        }
    }
}
