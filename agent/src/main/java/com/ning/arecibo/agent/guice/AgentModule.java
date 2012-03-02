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

import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.AbstractModule;
import com.ning.arecibo.agent.AgentDataCollectorManager;
import com.ning.arecibo.agent.config.ConfigFileUtils;
import com.ning.arecibo.agent.config.jmx.JMXConfigIteratorFactory;
import com.ning.arecibo.agent.config.jmx.JMXDynamicUtils;
import com.ning.arecibo.agent.config.snmp.SNMPConfigIteratorFactory;
import com.ning.arecibo.agent.datasource.DataSourceUtils;
import com.ning.arecibo.agent.datasource.IdentityConfigIteratorFactory;
import com.ning.arecibo.agent.datasource.jmx.JMXClientCache;
import com.ning.arecibo.agent.datasource.jmx.JMXParserManager;
import com.ning.arecibo.util.Logger;

public class AgentModule extends AbstractModule
{
    final static Logger log = Logger.getLogger(AgentModule.class);

    @Override
	public void configure()
	{
        AgentConfig agentConfig = new ConfigurationObjectFactory(System.getProperties()).build(AgentConfig.class);

        bind(AgentConfig.class).toInstance(agentConfig);

        initializeRmiConnectionTimeouts(agentConfig);

        bind(JMXClientCache.class).asEagerSingleton();
        bind(JMXDynamicUtils.class).asEagerSingleton();
        bind(JMXParserManager.class).asEagerSingleton();
        bind(DataSourceUtils.class).asEagerSingleton();
        bind(IdentityConfigIteratorFactory.class).asEagerSingleton();
        bind(JMXConfigIteratorFactory.class).asEagerSingleton();
        bind(SNMPConfigIteratorFactory.class).asEagerSingleton();
        bind(ConfigFileUtils.class).asEagerSingleton();
        bind(AgentDataCollectorManager.class).asEagerSingleton();

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(AgentDataCollectorManager.class).as("arecibo.agent:name=AgentDataManager");
	}

    private void initializeRmiConnectionTimeouts(AgentConfig agentConfig) {
        
        // set the rmi timeouts for the jmx connection/read method calls
        long connectionTimeout = agentConfig.getConnectionTimeout().getMillis();
        System.setProperty("sun.rmi.transport.connectionTimeout", Long.toString(connectionTimeout));
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", Long.toString(connectionTimeout));
    }
}
