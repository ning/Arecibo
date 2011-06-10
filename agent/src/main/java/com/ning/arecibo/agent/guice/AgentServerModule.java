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

package com.ning.arecibo.agent.guice;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.servlet.ServletModule;
import com.ning.arecibo.agent.config.AgentConfig;
import com.ning.arecibo.agent.eventapireceiver.EventProcessorImpl;
import com.ning.arecibo.event.publisher.EventPublisherModule;
import com.ning.arecibo.event.publisher.EventSenderType;
import com.ning.arecibo.event.receiver.RESTEventReceiverModule;
import com.ning.arecibo.event.receiver.UDPEventReceiverModule;
import com.ning.arecibo.util.galaxy.GalaxyModule;
import com.ning.arecibo.util.lifecycle.LifecycleModule;
import org.skife.config.ConfigurationObjectFactory;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;

public class AgentServerModule extends ServletModule
{
    @Override
    protected void configureServlets()
    {
        install(new Module()
        {
            @Override
            public void configure(Binder binder)
            {
                AgentConfig config = new ConfigurationObjectFactory(System.getProperties()).build(AgentConfig.class);
                binder.bind(AgentConfig.class).toInstance(config);

                install(new LifecycleModule());

                // TODO: need to bind an implementation of ServiceLocator

                install(new RESTEventReceiverModule(EventProcessorImpl.class, "arecibo.agent:name=EventAPI"));
                install(new UDPEventReceiverModule(5));

                bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());

                // TODO: migrate?
                install(new GalaxyModule());
                install(new EventPublisherModule(EventSenderType.CLIENT));

                // TODO: migrate to config-magic
                install(new AgentModule());

                // TODO: create container
//                bind(GuiceContainer.class).to(AgentContainer.class).asEagerSingleton();
            }
        });

//        filter("/*").through(AgentContainer.class, ImmutableMap.of(
//            PackagesResourceConfig.PROPERTY_PACKAGES, "com.ning.arecibo.event.receiver"
//        ));
    }
}
