package com.ning.arecibo.collector;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.collector.guice.CollectorModule;
import com.ning.arecibo.collector.guice.CollectorRESTEventReceiverModule;
import com.ning.arecibo.event.receiver.UDPEventReceiverConfig;
import com.ning.arecibo.event.receiver.UDPEventReceiverModule;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.util.EmbeddedJettyConfig;
import com.ning.arecibo.util.EmbeddedJettyJerseyModule;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.lifecycle.Lifecycle;
import com.ning.arecibo.util.lifecycle.LifecycleEvent;
import com.ning.arecibo.util.lifecycle.LifecycleModule;
import com.ning.arecibo.util.rmi.RMIModule;
import com.ning.arecibo.util.rmi.RMIRegistryConfig;
import com.ning.arecibo.util.service.DummyServiceLocatorModule;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;
import org.eclipse.jetty.server.Server;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventCollectorServer
{
    private static final Logger log = Logger.getLogger(EventCollectorServer.class);
    private final Server server;
    private final Lifecycle lifecycle;
    private final ServiceLocator serviceLocator;
    private final CollectorConfig collectorConfig;
    private final EmbeddedJettyConfig jettyConfig;
    private final UDPEventReceiverConfig udpConfig;
    private final RMIRegistryConfig rmiConfig;

    public static final String NAME = EventCollectorServer.class.getSimpleName();

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Inject
    public EventCollectorServer(CollectorConfig collectorConfig,
                                EmbeddedJettyConfig config,
                                UDPEventReceiverConfig udpConfig,
                                RMIRegistryConfig rmiConfig,
                                Server server,
                                Lifecycle lifecycle,
                                ServiceLocator serviceLocator)
    {
        this.collectorConfig = collectorConfig;
        this.jettyConfig = config;
        this.udpConfig = udpConfig;
        this.rmiConfig = rmiConfig;
        this.server = server;
        this.lifecycle = lifecycle;
        this.serviceLocator = serviceLocator;
    }

    public void run() throws Exception
    {
        // advertise event endpoints
        Map<String, String> map = new HashMap<String, String>();
        map.put(EventService.HOST, jettyConfig.getHost());
        map.put(EventService.JETTY_PORT, String.valueOf(jettyConfig.getPort()));
        map.put(EventService.UDP_PORT, String.valueOf(udpConfig.getPort()));
        map.put(EventService.RMI_PORT, String.valueOf(rmiConfig.getPort()));
        ServiceDescriptor self = new ServiceDescriptor(collectorConfig.getCollectorServiceName(), map);

        // advertise on beacon
        serviceLocator.advertiseLocalService(self);

        String name = getClass().getSimpleName();
        final long startTime = System.currentTimeMillis();
        log.info("Starting up %s on port %d", name, jettyConfig.getPort());

        lifecycle.fire(LifecycleEvent.START);

        final long secondsToStart = (System.currentTimeMillis() - startTime) / 1000;
        log.info("STARTUP COMPLETE: server started in %d:%02d", secondsToStart / 60, secondsToStart % 60);

        final Thread t = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            public void run()
            {
                t.interrupt();
            }
        });

        server.start();
        isRunning.set(true);

        try {
            Thread.currentThread().join();
        }
        catch (InterruptedException e) {
            // continue
        }

        stop();
    }

    public void stop()
    {
        try {
            log.info("Shutting down %s", getClass().getSimpleName());

            log.info("Stopping lifecycle");
            lifecycle.fire(LifecycleEvent.STOP);

            log.info("Stopping jetty server");
            server.stop();
            isRunning.set(false);

            log.info("Shutdown completed");
        }
        catch (Exception e) {
            log.warn(e);
        }
    }

    public boolean isRunning()
    {
        return isRunning.get();
    }

    public static void main(String[] args) throws Exception
    {
        Injector injector = Guice.createInjector(Stage.PRODUCTION,
            new LifecycleModule(),
            new EmbeddedJettyJerseyModule(),
            new CollectorRESTEventReceiverModule(),
            new UDPEventReceiverModule(),
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
                }
            },
            new RMIModule(),
            new CollectorModule());

        EventCollectorServer server = injector.getInstance(EventCollectorServer.class);
        server.run();
    }
}
