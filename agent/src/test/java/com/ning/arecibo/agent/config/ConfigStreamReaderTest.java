package com.ning.arecibo.agent.config;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.testng.annotations.Test;
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
            for(File file:files) {
                String fileName = file.getName();
                int suffixIndex = fileName.lastIndexOf(ConfigFileUtils.CONFIG_FILE_SUFFIX);
                if(suffixIndex == -1) {
                    continue;
                }

                String monitoringType = fileName.substring(0,suffixIndex);

                GuiceDefaultsForDataSources defaults = new GuiceDefaultsForDataSources(ConfigType.ALL.toString() + "," + ConfigType.EXCLUSION.toString());
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
