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

package com.ning.arecibo.dashboard.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.ning.arecibo.collector.CollectorClient;
import com.ning.arecibo.collector.CollectorClientConfig;
import com.ning.arecibo.collector.discovery.CollectorFinder;
import com.ning.arecibo.collector.discovery.DefaultCollectorFinder;
import com.ning.arecibo.collector.rest.DefaultCollectorClient;
import com.ning.arecibo.dashboard.alert.AlertRESTClient;
import com.ning.arecibo.dashboard.alert.AlertStatusManager;
import com.ning.arecibo.dashboard.alert.ClusterAwareAlertClient;
import com.ning.arecibo.dashboard.config.SuperGroupsManager;
import com.ning.arecibo.dashboard.format.DashboardFormatManager;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;
import com.ning.arecibo.event.publisher.HdfsEventPublisher;
import com.ning.arecibo.event.publisher.RandomEventServiceChooser;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.service.DummyServiceLocator;
import com.ning.arecibo.util.service.ServiceLocator;
import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

public class DashboardModule extends AbstractModule
{
    private static final Logger log = Logger.getLogger(DashboardModule.class);

    @Override
    public void configure()
    {
        // Dashboard configuration
        final DashboardConfig dashboardConfig = new ConfigurationObjectFactory(System.getProperties()).build(DashboardConfig.class);
        bind(DashboardConfig.class).toInstance(dashboardConfig);

        // Collector client configuration
        final CollectorClientConfig collectorClientConfig = new ConfigurationObjectFactory(System.getProperties()).build(CollectorClientConfig.class);
        bind(CollectorClientConfig.class).toInstance(collectorClientConfig);

        bind(SuperGroupsManager.class).asEagerSingleton();

        configureServiceLocator(dashboardConfig);
        configureCollectorFinder(collectorClientConfig);
        configureCollectorClient();

        bind(DashboardFormatManager.class).asEagerSingleton();
        bind(GalaxyStatusManager.class).asEagerSingleton();
        bind(AlertStatusManager.class).asEagerSingleton();
        bind(ClusterAwareAlertClient.class).asEagerSingleton();
        bind(AlertRESTClient.class).asEagerSingleton();

        final ExportBuilder builder = MBeanModule.newExporter(binder());
        builder.export(RandomEventServiceChooser.class).as("arecibo:type=HdfsEventServiceChooser");
        builder.export(HdfsEventPublisher.class).as("arecibo:name=HdfsEventPublisher");

        installExtraModules(dashboardConfig);
    }

    protected void configureCollectorClient()
    {
        bind(CollectorClient.class).to(DefaultCollectorClient.class).asEagerSingleton();
    }

    protected void configureCollectorFinder(final CollectorClientConfig config)
    {
        try {
            bind(CollectorFinder.class).to((Class<? extends CollectorFinder>) Class.forName(config.getCollectorFinderClass())).asEagerSingleton();
        }
        catch (ClassNotFoundException e) {
            log.error("Unable to find CollectorFinder", e);
            bind(CollectorFinder.class).to(DefaultCollectorFinder.class).asEagerSingleton();
        }
    }

    private void configureServiceLocator(final DashboardConfig config)
    {
        try {
            bind(ServiceLocator.class).to((Class<? extends ServiceLocator>) Class.forName(config.getServiceLocatorClass())).asEagerSingleton();
        }
        catch (ClassNotFoundException e) {
            log.error("Unable to find ServiceLocator", e);
            bind(ServiceLocator.class).to(DummyServiceLocator.class).asEagerSingleton();
        }
    }

    protected void installExtraModules(final DashboardConfig dashboardConfig)
    {
        for (final String guiceModule : dashboardConfig.getExtraGuiceModules().split(",")) {
            if (guiceModule.isEmpty()) {
                continue;
            }

            try {
                log.info("Installing extra module: " + guiceModule);
                install((Module) Class.forName(guiceModule).newInstance());
            }
            catch (InstantiationException e) {
                log.warn("Ignoring module: " + guiceModule, e);
            }
            catch (IllegalAccessException e) {
                log.warn("Ignoring module: " + guiceModule, e);
            }
            catch (ClassNotFoundException e) {
                log.warn("Ignoring module: " + guiceModule, e);
            }
        }
    }
}
