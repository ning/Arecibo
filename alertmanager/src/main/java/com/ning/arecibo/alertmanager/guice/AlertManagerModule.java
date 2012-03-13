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

package com.ning.arecibo.alertmanager.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.ning.arecibo.alert.client.AlertClient;
import com.ning.arecibo.alert.client.AlertClientConfig;
import com.ning.arecibo.alert.client.discovery.AlertFinder;
import com.ning.arecibo.alert.client.discovery.DefaultAlertFinder;
import com.ning.arecibo.alert.client.rest.DefaultAlertClient;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.service.DummyServiceLocator;
import com.ning.arecibo.util.service.ServiceLocator;
import org.skife.config.ConfigurationObjectFactory;

public class AlertManagerModule extends AbstractModule
{
    private static final Logger log = Logger.getLogger(AlertManagerModule.class);

    @Override
    public void configure()
    {
        final AreciboAlertManagerConfig alertManagerConfigonfig = new ConfigurationObjectFactory(System.getProperties()).build(AreciboAlertManagerConfig.class);
        bind(AreciboAlertManagerConfig.class).toInstance(alertManagerConfigonfig);

        final AlertClientConfig alertClientConfig = new ConfigurationObjectFactory(System.getProperties()).build(AlertClientConfig.class);
        bind(AlertClientConfig.class).toInstance(alertClientConfig);

        configureServiceLocator(alertManagerConfigonfig);
        configureAlertFinder(alertClientConfig);

        bind(AlertClient.class).to(DefaultAlertClient.class).asEagerSingleton();

        for (final String guiceModule : alertManagerConfigonfig.getExtraGuiceModules().split(",")) {
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

    private void configureAlertFinder(final AlertClientConfig config)
    {
        try {
            bind(AlertFinder.class).to((Class<? extends AlertFinder>) Class.forName(config.getAlertFinderClass())).asEagerSingleton();
        }
        catch (ClassNotFoundException e) {
            log.error("Unable to find AlertFinder", e);
            bind(AlertFinder.class).to(DefaultAlertFinder.class).asEagerSingleton();
        }
    }

    private void configureServiceLocator(final AreciboAlertManagerConfig config)
    {
        try {
            bind(ServiceLocator.class).to((Class<? extends ServiceLocator>) Class.forName(config.getServiceLocatorClass())).asEagerSingleton();
        }
        catch (ClassNotFoundException e) {
            log.error("Unable to find ServiceLocator", e);
            bind(ServiceLocator.class).to(DummyServiceLocator.class).asEagerSingleton();
        }
    }
}
