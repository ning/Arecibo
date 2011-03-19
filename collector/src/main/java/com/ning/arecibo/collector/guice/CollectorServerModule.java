/*
 * Copyright 2011 Ning, Inc.
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

package com.ning.arecibo.collector.guice;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import com.ning.arecibo.collector.config.CollectorConfig;
import com.ning.arecibo.collector.process.CollectorEventProcessor;
import com.ning.arecibo.event.receiver.RESTEventReceiverModule;
import com.ning.arecibo.event.receiver.UDPEventReceiverModule;
import com.ning.arecibo.util.lifecycle.LifecycleModule;
import com.ning.arecibo.util.rmi.RMIRegistryProvider;
import org.apache.log4j.Logger;
import org.skife.config.ConfigurationObjectFactory;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.rmi.registry.Registry;

public class CollectorServerModule extends ServletModule
{
    private static final Logger log = Logger.getLogger(CollectorServerModule.class);

    @Override
    protected void configureServlets()
    {
        install(new Module()
        {
            @Override
            public void configure(Binder binder)
            {

                CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
                binder.bind(CollectorConfig.class).toInstance(config);

                install(new LifecycleModule());

                // TODO: need to bind an implementation of ServiceLocator

                install(new RESTEventReceiverModule(CollectorEventProcessor.class, "arecibo.collector:name=CollectorEventProcessor"));
                install(new UDPEventReceiverModule());

                bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());

                binder.bind(Registry.class).toProvider(RMIRegistryProvider.class).asEagerSingleton();
                binder.bindConstant().annotatedWith(Names.named("RMIRegistryPort")).to(config.getRmiPort());

                install(new CollectorModule(config));
            }
        });

//        filter("/*").through(GuiceContainer.class, ImmutableMap.of(
//            PackagesResourceConfig.PROPERTY_PACKAGES, "com.ning.arecibo.collector.endpoint"
//        ));
    }
}
