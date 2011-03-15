package com.ning.arecibo.agent.guice;

import java.util.Set;
import java.util.StringTokenizer;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import com.google.inject.Inject;
import com.ning.arecibo.agent.config.ConfigType;

public class GuiceDefaultsForDataSources
{
    private final Set<ConfigType> configTypesEnabled;
	private final int jmxPort;
	private final int snmpPort;
	private final String snmpCommunity;
	private final int defaultPollingIntervalSeconds;
    private final boolean JMXMonitoringProfilePollingEnabled;

    @Inject
	public GuiceDefaultsForDataSources(@ConfigTypesEnabled String configTypesEnabledString,
                                        @DefaultPollingIntervalSeconds int defaultPollingIntervalSeconds,
                                        @JMXMonitoringProfilePollingEnabled boolean JMXMonitoringProfilePollingEnabled,
                                        @JMXPort int jmxPort,
	                                    @SNMPCommunity String snmpCommunity,
	                                    @SNMPPort int snmpPort)
	{
        this.configTypesEnabled = new ConcurrentSkipListSet<ConfigType>();
        StringTokenizer st = new StringTokenizer(configTypesEnabledString,",");
        while(st.hasMoreTokens()) {
            ConfigType configType = ConfigType.valueOf(st.nextToken().toUpperCase());

            List<ConfigType> subTypes = configType.getSubTypes();
            if(subTypes == null || subTypes.size() == 0)
                this.configTypesEnabled.add(configType);
            else {
                this.configTypesEnabled.addAll(subTypes);
            }
        }

        if(this.configTypesEnabled.contains(ConfigType.JMX))
            this.JMXMonitoringProfilePollingEnabled = JMXMonitoringProfilePollingEnabled;
        else
            this.JMXMonitoringProfilePollingEnabled = false;

        this.jmxPort = jmxPort;
        this.defaultPollingIntervalSeconds = defaultPollingIntervalSeconds;
        this.snmpCommunity = snmpCommunity;
        this.snmpPort = snmpPort;
    }

    public boolean isConfigTypeEnabled(ConfigType cType)
    {
        return this.configTypesEnabled.contains(cType);
    }

    public Set<ConfigType> getConfigTypesEnabled() {
        return this.configTypesEnabled;
    }

    public boolean isJMXMonitoringProfilePollingEnabled()
	{
		return JMXMonitoringProfilePollingEnabled;
	}

    public int getJmxPort()
    {
        return jmxPort;
    }

	public int getDefaultPollingIntervalSeconds()
	{
		return defaultPollingIntervalSeconds;
	}

	public String getSnmpCommunity()
	{
		return snmpCommunity;
	}

	public int getSnmpPort()
	{
		return snmpPort;
	}
}
