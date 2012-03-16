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

package com.ning.arecibo.collector.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.collector.process.EventHandler;
import com.ning.arecibo.collector.rt.kafka.KafkaEventHandler;
import com.ning.arecibo.util.ArrayListProvider;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jdbi.DBIProvider;
import com.ning.arecibo.util.service.DummyServiceLocator;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.jersey.metrics.TimedResourceModule;
import org.skife.config.ConfigurationObjectFactory;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

import java.util.List;

public class CollectorModule extends AbstractModule
{
    private static final Logger log = Logger.getLogger(CollectorModule.class);

    private ExportBuilder builder = null;

    @Override
    public void configure()
    {
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);

        bind(CollectorConfig.class).toInstance(config);

        configureDao();
        configureStats();
        configureServiceLocator(config);
        configureEventHandlers(config);

        builder = MBeanModule.newExporter(binder());

        builder.export(TimelineEventHandler.class).as("arecibo.collector:name=TimelineEventHandler");

        for (final String guiceModule : config.getExtraGuiceModules().split(",")) {
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

    private void configureEventHandlers(final CollectorConfig config)
    {
        final ArrayListProvider<EventHandler> provider = new ArrayListProvider<EventHandler>();

        // Hook the persistent handler by default
        log.info("Persistent producer configured");
        provider.add(TimelineEventHandler.class);

        // Hook the real-time handler as needed
        // TODO - do we want to turn it off/on at runtime?
        if (config.isKafkaEnabled()) {
            log.info("Kafka producer configured");
            provider.add(KafkaEventHandler.class);
            builder.export(KafkaEventHandler.class).as("arecibo.collector:name=KafkaEventHandler");
        }

        bind(new TypeLiteral<List<EventHandler>>()
        {
        })
            .toProvider(provider)
            .asEagerSingleton();
    }

    private void configureServiceLocator(final CollectorConfig config)
    {
        try {
            bind(ServiceLocator.class).to((Class<? extends ServiceLocator>) Class.forName(config.getServiceLocatorClass())).asEagerSingleton();
        }
        catch (ClassNotFoundException e) {
            log.error("Unable to find ServiceLocator", e);
            bind(ServiceLocator.class).to(DummyServiceLocator.class).asEagerSingleton();
        }
    }

    protected void configureDao()
    {
        bind(DBI.class).toProvider(new DBIProvider(System.getProperties(), "arecibo.collector.db")).asEagerSingleton();
        bind(IDBI.class).to(Key.get(DBI.class)).asEagerSingleton();
        bind(TimelineDAO.class).toProvider(CachingDefaultTimelineDAOProvider.class).asEagerSingleton();
    }

    protected void configureStats()
    {
        install(new TimedResourceModule());
    }
}

