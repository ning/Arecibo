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

import com.google.common.collect.ImmutableMap;
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
import com.ning.arecibo.util.lifecycle.LifecycleListener;
import com.ning.arecibo.util.lifecycle.LifecycleModule;
import com.ning.arecibo.util.lifecycle.StagedLifecycle;
import com.ning.arecibo.util.rmi.RMIModule;
import com.ning.arecibo.util.rmi.RMIRegistryConfig;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;
import org.eclipse.jetty.server.Server;
import org.weakref.jmx.guice.MBeanModule;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventCollectorServer
{
    private static final Logger log = Logger.getLogger(EventCollectorServer.class);
    private final Server server;
    private final ServiceLocator serviceLocator;
    private final CollectorConfig collectorConfig;
    private final EmbeddedJettyConfig jettyConfig;
    private final UDPEventReceiverConfig udpConfig;
    private final RMIRegistryConfig rmiConfig;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Inject
    public EventCollectorServer(final CollectorConfig collectorConfig,
                                final EmbeddedJettyConfig config,
                                final UDPEventReceiverConfig udpConfig,
                                final RMIRegistryConfig rmiConfig,
                                final Server server,
                                final ServiceLocator serviceLocator)
    {
        this.collectorConfig = collectorConfig;
        this.jettyConfig = config;
        this.udpConfig = udpConfig;
        this.rmiConfig = rmiConfig;
        this.server = server;
        this.serviceLocator = serviceLocator;
    }

    public void start() throws Exception
    {
        server.start();
        isRunning.set(true);
    }

    public void announce()
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
    }

    public void unannounce()
    {
        serviceLocator.stop();
    }

    public void stop()
    {
        try {
            server.stop();
            isRunning.set(false);
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
            new LifecycleModule(new StagedLifecycle()),
            new EmbeddedJettyJerseyModule(ImmutableMap.<String, String>of(
                "/xn/rest/.*", "com.ning.arecibo.event.receiver",
                "/rest/.*", "com.ning.arecibo.collector.resources,com.ning.arecibo.util.jaxrs"
            )),
            new CollectorRESTEventReceiverModule(),
            new UDPEventReceiverModule(),
            new MBeanModule(),
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

        final Lifecycle lifecycle = injector.getInstance(Lifecycle.class);
        final EventCollectorServer starter = injector.getInstance(EventCollectorServer.class);

        final LifecycleListener listener = new LifecycleListener()
        {
            @Override
            public void onEvent(final LifecycleEvent event)
            {
                try {
                    if (LifecycleEvent.START.equals(event)) {
                        starter.start();
                    }
                    else if (LifecycleEvent.ANNOUNCE.equals(event)) {
                        starter.announce();
                    }
                    else if (LifecycleEvent.UNANNOUNCE.equals(event)) {
                        starter.unannounce();
                    }
                    else if (LifecycleEvent.STOP.equals(event)) {
                        starter.stop();
                    }
                }
                catch (Exception ex) {
                    log.warn(ex, "Error while performing lifecycle action");
                    throw new RuntimeException(ex);
                }
            }
        };

        lifecycle.addListener(LifecycleEvent.START, listener);
        lifecycle.addListener(LifecycleEvent.STOP, listener);
        lifecycle.addListener(LifecycleEvent.ANNOUNCE, listener);

        lifecycle.fireUpTo(LifecycleEvent.ANNOUNCE, true);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    lifecycle.fireUpTo(LifecycleEvent.STOP, true);
                }
                catch (Exception ex) {
                    log.warn(ex, "Error while stopping the service");
                    throw new RuntimeException(ex);
                }
            }
        }));

        Thread.currentThread().join();
    }
}
