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

package com.ning.arecibo.alert.guice;

import com.google.inject.AbstractModule;
import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.email.EmailManager;
import com.ning.arecibo.alert.endpoint.AlertStatusEndPoint;
import com.ning.arecibo.alert.endpoint.AlertingConfigEndPoint;
import com.ning.arecibo.alert.endpoint.ConfigEventSendEndPoint;
import com.ning.arecibo.alert.endpoint.ConfigStatusEndPoint;
import com.ning.arecibo.alert.endpoint.ConfigUpdateEndPoint;
import com.ning.arecibo.alert.endpoint.JSONAlertStatusEndPoint;
import com.ning.arecibo.alert.endpoint.NotifConfigEndPoint;
import com.ning.arecibo.alert.endpoint.NotifGroupEndPoint;
import com.ning.arecibo.alert.endpoint.NotifGroupMappingEndPoint;
import com.ning.arecibo.alert.endpoint.NotifMappingEndPoint;
import com.ning.arecibo.alert.endpoint.PersonEndPoint;
import com.ning.arecibo.alert.endpoint.ThresholdConfigEndPoint;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;
import com.ning.arecibo.alert.manage.AsynchronousEventHandler;
import com.ning.arecibo.event.publisher.EventPublisherConfig;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.service.DummyServiceLocator;
import com.ning.arecibo.util.service.ServiceLocator;
import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

import java.util.UUID;

public class AlertServiceModule extends AbstractModule
{
    private static final Logger log = Logger.getLogger(AlertServiceModule.class);

    @Override
    public void configure()
    {
        final ConfigurationObjectFactory configFactory = new ConfigurationObjectFactory(System.getProperties());
        final AlertServiceConfig alertServiceConfig = configFactory.build(AlertServiceConfig.class);
        bind(AlertServiceConfig.class).toInstance(alertServiceConfig);
        final EventPublisherConfig eventPublisherConfig = configFactory.build(EventPublisherConfig.class);
        bind(EventPublisherConfig.class).toInstance(eventPublisherConfig);

        configureServiceLocator(alertServiceConfig);

        bind(AsynchronousEventHandler.class).asEagerSingleton();

        bind(ConfigManager.class).asEagerSingleton();
        bind(AlertManager.class).asEagerSingleton();
        bind(LoggingManager.class).asEagerSingleton();
        bind(EmailManager.class).asEagerSingleton();

        bind(AlertStatusEndPoint.class).asEagerSingleton();
        bind(JSONAlertStatusEndPoint.class).asEagerSingleton();
        bind(ConfigStatusEndPoint.class).asEagerSingleton();
        bind(ConfigUpdateEndPoint.class).asEagerSingleton();
        bind(ConfigEventSendEndPoint.class).asEagerSingleton();
        bind(PersonEndPoint.class).asEagerSingleton();
        bind(NotifConfigEndPoint.class).asEagerSingleton();
        bind(NotifGroupEndPoint.class).asEagerSingleton();
        bind(NotifMappingEndPoint.class).asEagerSingleton();
        bind(AlertingConfigEndPoint.class).asEagerSingleton();
        bind(NotifGroupMappingEndPoint.class).asEagerSingleton();
        bind(ThresholdConfigEndPoint.class).asEagerSingleton();

        bind(UUID.class).annotatedWith(SelfUUID.class).toInstance(UUID.randomUUID());

        final ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(AsynchronousEventHandler.class).as("arecibo.alert:name=AsynchronousEventHandler");
    }

    private void configureServiceLocator(final AlertServiceConfig config)
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
