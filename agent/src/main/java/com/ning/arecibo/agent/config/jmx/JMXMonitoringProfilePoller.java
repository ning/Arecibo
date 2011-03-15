package com.ning.arecibo.agent.config.jmx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.ObjectName;
import org.apache.commons.lang.StringUtils;
import com.ning.arecibo.agent.config.BaseConfigReader;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.datasource.jmx.JMXClient;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.galaxy.GalaxyCoreStatus;

public class JMXMonitoringProfilePoller extends BaseConfigReader
{
	public static final String NING_JMX_NAME_MONITORING_PROFILE = "arecibo.jmx:name=MonitoringProfile";
	public static final String ATTR_MONITORING_PROFILE = "MonitoringProfile";
	public static final String ATTR_MONITORING_PROFILE_VERSION = "MonitoringProfileVersion";

    private static final String delimeter = ";";
    private static final String commentChar = "#";

	private static final Logger log = Logger.getLoggerViaExpensiveMagic();
	private List<Config> configs = new ArrayList<Config>();

	public JMXMonitoringProfilePoller(String host, String configPath, String coreType, GuiceDefaultsForDataSources guiceDefaults) throws ConfigException
	{
		super(host, configPath, coreType, guiceDefaults);
		init();
	}

	public JMXMonitoringProfilePoller(GalaxyCoreStatus status, GuiceDefaultsForDataSources guiceDefaults) throws ConfigException
	{
		this(status.getZoneHostName(), status.getConfigPath(), status.getCoreType(), guiceDefaults);
	}

    //TODO : optimization can be achieved by caching the profile and comparing the profile version
    // actually maybe not a good idea, since need to rely on the version being updated reliably when a new version of core is released,
    // or if a new bean comes online that had null or NaN data previously
	private void init()
	{
		JMXClient client = null;
		try {
			client = new JMXClient(String.format("%s:%s", host, guiceDefaults.getJmxPort()));
			ObjectName objectName = new ObjectName(NING_JMX_NAME_MONITORING_PROFILE);
			if (client.getMBeanServerConnection().isRegistered(objectName)) {
				String[] list = (String[]) client.getMBeanServerConnection().getAttribute(objectName, ATTR_MONITORING_PROFILE);
				for (String line : list) {
					try {
                        // for now, assume all monitoring profiles use the old-style 1 line ad hoc format
						Config config = parseOldFormatLine(line);
						if (config != null) {
							configs.add(config);
						}
					}
					catch (Exception e) {
						log.info(e,"Could not parse config line '" + line + "', for '" + host + ":" + guiceDefaults.getJmxPort() + "', for core monitoring file");
					}
				}
				log.info("Loaded " + configs.size() + " configs via registered monitoring profile for host '" + host + "'");
			}
			else {
				log.info("No registered monitoring profile found for host '" + host + "'");
			}
		}
		catch (Exception e) {
			log.info("Could not poll '" + host + ":" + guiceDefaults.getJmxPort() + "' for core monitoring file",e);
		}
		finally {
			if(client != null) {
				try {
					client.close();
				}
				catch(IOException e) {
					log.warn("Problem closing jmx client connection",e);
				}
			}
		}
	}

    private JMXConfig parseOldFormatLine(String line) throws ConfigException {
        if (StringUtils.isEmpty(line) || line.startsWith(commentChar)) {
            return null;
        }

        line = line.split(commentChar)[0].trim();       // strip off end-of-line comments
        if (StringUtils.isEmpty(line)) {
            return null;
        }

        if (!line.contains(delimeter)) {
            return null;
        }

        String[] parts = line.split(delimeter);
        if (parts.length < 4 || parts.length > 5) {
            throw new ConfigException("problem parsing line '" + line + "'");
        }
        String objectName = parts[0].trim();
        String attribute = parts[1].trim();
        String eventType = parts[2].trim();
        String eventAttributeType = parts[3].trim();

        String control = (parts.length == 5) ? parts[4].trim() : "";

        Map<String, Object> optionsMap = new HashMap<String, Object>();
        optionsMap.put(JMXConfig.OBJECT_NAME, objectName);
        optionsMap.put(JMXConfig.ATTRIBUTE, attribute);
        optionsMap.put(Config.EVENT_TYPE, eventType);
        optionsMap.put(Config.EVENT_ATTRIBUTE_TYPE, eventAttributeType);
        optionsMap.put(Config.MONITORING_TYPES, Config.getMonitoringTypesFromControlString(control));

        return new JMXConfig(host, configPath, coreType, guiceDefaults, optionsMap);
    }

	public List<Config> getConfigurations()
	{
		return configs;
	}
}
