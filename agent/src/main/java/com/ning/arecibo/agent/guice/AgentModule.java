package com.ning.arecibo.agent.guice;

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
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;

public class AgentModule extends AbstractModule
{
    @Override
    public void configure()
    {
        initializeRmiConnectionTimeouts();

        bind(JMXClientCache.class).asEagerSingleton();
        bind(JMXDynamicUtils.class).asEagerSingleton();
        bind(JMXParserManager.class).asEagerSingleton();
        bind(DataSourceUtils.class).asEagerSingleton();
        bind(IdentityConfigIteratorFactory.class).asEagerSingleton();
        bind(JMXConfigIteratorFactory.class).asEagerSingleton();
        bind(SNMPConfigIteratorFactory.class).asEagerSingleton();
        bind(ConfigFileUtils.class).asEagerSingleton();
        bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
        bind(AgentDataCollectorManager.class).asEagerSingleton();

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(AgentDataCollectorManager.class).as("arecibo.agent:name=AgentDataManager");
    }

    private void initializeRmiConnectionTimeouts()
    {
        // set the rmi timeouts for the jmx connection/read method calls
        int connectionTimeout = Integer.getInteger("arecibo.tools.coremonitor.connection_timeout", ConnectionTimeout.DEFAULT);
        long connectionTimeoutMillis = connectionTimeout * 1000L;
        System.setProperty("sun.rmi.transport.connectionTimeout", Long.toString(connectionTimeoutMillis));
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", Long.toString(connectionTimeoutMillis));
    }
}
