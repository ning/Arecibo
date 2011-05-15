package com.ning.arecibo.event.receiver;

import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.ning.arecibo.util.lifecycle.LifecycleAction;
import com.ning.arecibo.util.lifecycle.LifecycleEvent;
import com.ning.arecibo.util.lifecycle.LifecycledProvider;

public class UDPEventReceiverModule extends AbstractModule
{
    @Override
    public void configure()
    {
        UDPEventReceiverConfig config = new ConfigurationObjectFactory(System.getProperties()).build(UDPEventReceiverConfig.class);

        bind(UDPEventReceiverConfig.class).toInstance(config);
        bind(DatagramSocket.class).annotatedWith(Names.named("UDPSocket")).toProvider(DatagramSocketProvider.class);
        bind(ExecutorService.class).annotatedWith(Names.named("DatagramDispatcher")).toInstance(Executors.newFixedThreadPool(config.getNumUDPThreads()));
        bind(UDPEventHandler.class).asEagerSingleton();

        LifecycledProvider<UDPServer> lifecycledProvider = new LifecycledProvider<UDPServer>(binder(), UDPServer.class);
        lifecycledProvider.addListener(LifecycleEvent.START, new LifecycleAction<UDPServer>()
        {
            public void doAction(UDPServer server)
            {
                try {
                    server.start();
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        lifecycledProvider.addListener(LifecycleEvent.STOP, new LifecycleAction<UDPServer>()
        {
            public void doAction(UDPServer server)
            {
                server.stop();
            }
        });

        bind(UDPServer.class).toProvider(lifecycledProvider).asEagerSingleton();

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(UDPEventHandler.class).as("arecibo:name=UDPEventHandler");
    }
}
