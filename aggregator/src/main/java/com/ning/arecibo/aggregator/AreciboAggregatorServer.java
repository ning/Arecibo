package com.ning.arecibo.aggregator;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.ning.arecibo.aggregator.concurrent.guice.KeyedExecutorModule;
import com.ning.arecibo.aggregator.esper.guice.EsperStatsModule;
import com.ning.arecibo.aggregator.guice.AggregatorModule;
import com.ning.arecibo.aggregator.impl.AggregatorServer;
import com.ning.arecibo.aggregator.impl.EventProcessorImpl;
import com.ning.arecibo.aggregator.plugin.guice.MonitoringPluginModule;
import com.ning.arecibo.event.publisher.EventPublisherModule;
import com.ning.arecibo.event.publisher.EventSenderType;
import com.ning.arecibo.event.receiver.RESTEventReceiverModule;
import com.ning.arecibo.event.receiver.UDPEventReceiverModule;
import com.ning.arecibo.util.EmbeddedJettyJerseyModule;
import com.ning.arecibo.util.lifecycle.LifecycleModule;
import com.ning.arecibo.util.rmi.RMIModule;

public class AreciboAggregatorServer
{
    public static void main(String[] args) throws Exception
	{
        Injector injector = Guice.createInjector(Stage.PRODUCTION,
                new LifecycleModule(),
                // TODO: need to bind an implementation of ServiceLocator
		        new RMIModule(),
		        new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
                    }
                },
                new EventPublisherModule(EventSenderType.SERVER),
                new EmbeddedJettyJerseyModule(),
                new RESTEventReceiverModule(EventProcessorImpl.class, "arecibo.aggregator:name=EventProcessor"),
                new UDPEventReceiverModule(),
                new EsperStatsModule(),
		        new MonitoringPluginModule(),
                new KeyedExecutorModule(),
                new AggregatorModule());

        AggregatorServer server = injector.getInstance(AggregatorServer.class);
        server.run();
    }
}
