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

package com.ning.arecibo.alertmanager.context;

import com.google.inject.Injector;
import com.ning.arecibo.alertmanager.guice.AlertManagerModule;
import com.ning.arecibo.alertmanager.guice.AreciboAlertManagerConfig;
import com.ning.arecibo.util.lifecycle.Lifecycle;
import com.ning.arecibo.util.lifecycle.LifecycleEvent;
import com.ning.arecibo.util.lifecycle.LifecycleModule;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.jetty.base.modules.ServerModuleBuilder;
import com.ning.jetty.core.CoreConfig;
import com.ning.jetty.core.listeners.SetupServer;

import javax.servlet.ServletContextEvent;
import java.util.HashMap;
import java.util.Map;

public class AlertManagerStartupContextListener extends SetupServer
{
    Lifecycle lifecycle = null;
    ServiceLocator serviceLocator = null;

    @Override
    public void contextInitialized(final ServletContextEvent event)
    {
        final ServerModuleBuilder builder = new ServerModuleBuilder()
            .setAreciboProfile(System.getProperty("action.arecibo.profile", "ning.jmx:name=MonitoringProfile"))
            .addModule(new LifecycleModule())
            .addModule(new AlertManagerModule())
            .enableLog4J()
            .setJerseyUriPattern("(.)*/rest/.*")
            .addJerseyResource("com.ning.arecibo.alertmanager.resources")
            .addJerseyResource("com.ning.arecibo.util.jaxrs");

        guiceModule = builder.build();

        // Let Guice create the injector
        super.contextInitialized(event);

        // Further setup for services discovery
        final Injector injector = (Injector) event.getServletContext().getAttribute(Injector.class.getName());
        serviceLocator = injector.getInstance(ServiceLocator.class);
        serviceLocator.startReadOnly();

        // Advertise alertmanager endpoints
        final CoreConfig jettyConfig = injector.getInstance(CoreConfig.class);
        final AreciboAlertManagerConfig alertManagerConfig = injector.getInstance(AreciboAlertManagerConfig.class);
        final Map<String, String> map = new HashMap<String, String>();
        map.put("host", jettyConfig.getServerHost());
        map.put("port", String.valueOf(jettyConfig.getServerPort()));
        final ServiceDescriptor self = new ServiceDescriptor(alertManagerConfig.getServiceName(), map);
        serviceLocator.advertiseLocalService(self);

        // Fire START event
        lifecycle = injector.getInstance(Lifecycle.class);
        lifecycle.fire(LifecycleEvent.START);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent)
    {
        if (serviceLocator != null) {
            serviceLocator.stop();
        }

        if (lifecycle != null) {
            lifecycle.fire(LifecycleEvent.STOP);
        }

        super.contextDestroyed(servletContextEvent);
    }
}