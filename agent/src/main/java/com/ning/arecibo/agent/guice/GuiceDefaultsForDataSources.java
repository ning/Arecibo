package com.ning.arecibo.agent.guice;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import org.skife.config.TimeSpan;
import com.google.inject.Inject;
import com.ning.arecibo.agent.config.ConfigType;

public class GuiceDefaultsForDataSources
{
    private final AgentConfig agentConfig;
    private final Set<ConfigType> configTypesEnabled;
    private final boolean isJMXMonitoringProfilePollingEnabled;

    @Inject
	public GuiceDefaultsForDataSources(AgentConfig agentConfig)
	{
        this.agentConfig = agentConfig;
        this.configTypesEnabled = new ConcurrentSkipListSet<ConfigType>();
        for (ConfigType configType : agentConfig.getConfigTypesEnabled()) {
            List<ConfigType> subTypes = configType.getSubTypes();
            if(subTypes == null || subTypes.size() == 0)
                this.configTypesEnabled.add(configType);
            else {
                this.configTypesEnabled.addAll(subTypes);
            }
        }

        if(this.configTypesEnabled.contains(ConfigType.JMX)) {
            this.isJMXMonitoringProfilePollingEnabled = agentConfig.isJMXMonitoringProfilePollingEnabled();
        }
        else {
            this.isJMXMonitoringProfilePollingEnabled = false;
        }
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
		return isJMXMonitoringProfilePollingEnabled;
	}

    public int getJmxPort()
    {
        return agentConfig.getJMXPort();
    }

	public TimeSpan getDefaultPollingInterval()
	{
		return agentConfig.getDefaultPollingInterval();
	}

	public String getSnmpCommunity()
	{
		return agentConfig.getSNMPCommunity();
	}

	public int getSnmpPort()
	{
		return agentConfig.getSNMPPort();
	}
}
