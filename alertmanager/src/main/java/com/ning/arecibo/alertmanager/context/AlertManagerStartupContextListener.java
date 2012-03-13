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
import com.ning.jetty.base.modules.ServerModuleBuilder;
import com.ning.jetty.core.listeners.SetupServer;

import javax.servlet.ServletContextEvent;

public class AlertManagerStartupContextListener extends SetupServer
{
    @Override
    public void contextInitialized(final ServletContextEvent event)
    {
        final ServerModuleBuilder builder = new ServerModuleBuilder()
            .setAreciboProfile(System.getProperty("action.arecibo.profile", "ning.jmx:name=MonitoringProfile"))
            .addModule(new LifecycleModule())
            .addModule(new AlertManagerModule())
            .enableLog4J()
            .addResource("com.ning.arecibo.alertmanager.resources")
            .addResource("com.ning.arecibo.util.jaxrs");

        guiceModule = builder.build();

        // Let Guice create the injector
        super.contextInitialized(event);

        final Injector injector = (Injector) event.getServletContext().getAttribute(Injector.class.getName());

        // do this here for now, should really be part of better guiciness for servlet context
        // lifecycle is needed, if for no other reason than to start the LoggingModule
        final Lifecycle lc = injector.getInstance(Lifecycle.class);
        lc.fire(LifecycleEvent.START);
    }
}