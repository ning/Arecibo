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
        bind(Integer.class).annotatedWith(Names.named("UDPServerPort")).toInstance(config.getPort());
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
