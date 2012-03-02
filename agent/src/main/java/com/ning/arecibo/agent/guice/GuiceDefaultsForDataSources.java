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
