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

package com.ning.arecibo.collector;

import com.google.common.collect.ImmutableList;
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
        serviceLocator.startReadOnly();

        // Advertise event endpoints
        final Map<String, String> map = new HashMap<String, String>();
        map.put(EventService.HOST, jettyConfig.getHost());
        map.put(EventService.JETTY_PORT, String.valueOf(jettyConfig.getPort()));
        map.put(EventService.UDP_PORT, String.valueOf(udpConfig.getPort()));
        map.put(EventService.RMI_PORT, String.valueOf(rmiConfig.getPort()));
        final ServiceDescriptor self = new ServiceDescriptor(collectorConfig.getCollectorServiceName(), map);
        serviceLocator.advertiseLocalService(self);

        final String name = getClass().getSimpleName();
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
            serviceLocator.stop();

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

    public static void main(final String[] args) throws Exception
    {
        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
            new LifecycleModule(),
            new EmbeddedJettyJerseyModule(ImmutableList.<String>of("com.ning.arecibo.collector.resources", "com.ning.arecibo.util.jaxrs")),
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

        final EventCollectorServer server = injector.getInstance(EventCollectorServer.class);
        server.run();
    }
}
