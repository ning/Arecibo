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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.ning.arecibo.collector.healthchecks.DAOHealthCheck;
import com.ning.arecibo.collector.healthchecks.TimelineEventHandlerHealthCheck;
import com.ning.arecibo.collector.persistent.BackgroundDBChunkWriter;
import com.ning.arecibo.collector.persistent.TimelineAggregator;
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.collector.process.EventHandler;
import com.ning.arecibo.collector.rt.kafka.KafkaEventHandler;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.ArrayListProvider;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jdbi.DBIProvider;
import com.ning.arecibo.util.lifecycle.LifecycleAction;
import com.ning.arecibo.util.lifecycle.LifecycleEvent;
import com.ning.arecibo.util.lifecycle.LifecycledProvider;
import com.ning.arecibo.util.service.DummyServiceLocator;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.arecibo.util.timeline.persistent.FileBackedBuffer;
import com.ning.jersey.metrics.TimedResourceModule;
import com.yammer.metrics.core.HealthCheck;
import com.yammer.metrics.guice.InstrumentationModule;
import com.yammer.metrics.reporting.guice.MetricsServletModule;
import org.skife.config.ConfigurationObjectFactory;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.weakref.jmx.MBeanExporter;
import org.weakref.jmx.ObjectNames;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;

public class CollectorModule extends AbstractModule
{
    private static final Logger log = Logger.getLogger(CollectorModule.class);

    @Override
    public void configure()
    {
        final CollectorConfig config = configureConfig();

        configureFileBackedBuffer(config);
        configureDao();
        configureTimelineAggregator();
        configureBackgroundDBChunkWriter();
        configureStats();
        configureServiceLocator(config);
        configureEventHandlers(config);
        configureExtraModules(config);
    }

    protected CollectorConfig configureConfig()
    {
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);
        bind(CollectorConfig.class).toInstance(config);
        return config;
    }

    protected void configureFileBackedBuffer(final CollectorConfig config)
    {
        final MBeanExporter exporter = new MBeanExporter(ManagementFactory.getPlatformMBeanServer());

        // Persistent buffer for in-memory samples
        try {
            final FileBackedBuffer fileBackedBuffer = new FileBackedBuffer(config.getSpoolDir(), "TimelineEventHandler", config.getSegmentsSize(), config.getMaxNbSegments());
            exporter.export(ObjectNames.generatedNameOf(FileBackedBuffer.class), fileBackedBuffer);
            bind(FileBackedBuffer.class).toInstance(fileBackedBuffer);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void configureDao()
    {
        bind(DBI.class).toProvider(new DBIProvider(System.getProperties(), "arecibo.collector.db")).asEagerSingleton();
        bind(IDBI.class).to(Key.get(DBI.class)).asEagerSingleton();
        bind(TimelineDAO.class).toProvider(CachingDefaultTimelineDAOProvider.class).asEagerSingleton();
    }

    protected void configureTimelineAggregator()
    {
        final LifecycledProvider<TimelineAggregator> lifecycledProvider = new LifecycledProvider<TimelineAggregator>(binder(), TimelineAggregator.class);
        lifecycledProvider.addListener(LifecycleEvent.START, new LifecycleAction<TimelineAggregator>()
        {
            public void doAction(final TimelineAggregator aggregator)
            {
                log.info("START event received: starting aggregator thread");
                aggregator.runAggregationThread();
            }
        });
        lifecycledProvider.addListener(LifecycleEvent.STOP, new LifecycleAction<TimelineAggregator>()
        {
            public void doAction(final TimelineAggregator aggregator)
            {
                log.info("STOP event received: stopping aggregator thread");
                aggregator.stopAggregationThread();
            }
        });
        bind(TimelineAggregator.class).toProvider(lifecycledProvider).asEagerSingleton();

        final ExportBuilder builder = MBeanModule.newExporter(binder());
        builder.export(TimelineAggregator.class).withGeneratedName();
    }

    protected void configureBackgroundDBChunkWriter()
    {
        final LifecycledProvider<BackgroundDBChunkWriter> lifecycledProvider = new LifecycledProvider<BackgroundDBChunkWriter>(binder(), BackgroundDBChunkWriter.class);
        // The stop is done as a consequence of TimelineEventHandler.forceCommit(true), done in the STOP listener for TimelineEventHandler
        lifecycledProvider.addListener(LifecycleEvent.START, new LifecycleAction<BackgroundDBChunkWriter>()
        {
            public void doAction(final BackgroundDBChunkWriter backgroundWriter)
            {
                log.info("START event received: starting backgroundWriter thread");
                backgroundWriter.runBackgroundWriteThread();
            }
        });
        bind(BackgroundDBChunkWriter.class).toProvider(lifecycledProvider).asEagerSingleton();

        final ExportBuilder builder = MBeanModule.newExporter(binder());
        builder.export(BackgroundDBChunkWriter.class).withGeneratedName();
    }

    protected void configureStats()
    {
        install(new MetricsServletModule("/1.0/healthcheck", "/1.0/metrics", "/1.0/ping", "/1.0/threads"));
        final Multibinder<HealthCheck> healthChecksBinder = Multibinder.newSetBinder(binder(), HealthCheck.class);
        healthChecksBinder.addBinding().to(TimelineEventHandlerHealthCheck.class).asEagerSingleton();
        healthChecksBinder.addBinding().to(DAOHealthCheck.class).asEagerSingleton();

        install(new InstrumentationModule());
        install(new TimedResourceModule());
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

    private void configureEventHandlers(final CollectorConfig config)
    {
        final ArrayListProvider<EventHandler> provider = new ArrayListProvider<EventHandler>();

        // Hook the persistent handler by default
        log.info("Persistent producer configured");
        final LifecycledProvider<TimelineEventHandler> lifecycledProvider = new LifecycledProvider<TimelineEventHandler>(binder(), TimelineEventHandler.class);
        lifecycledProvider.addListener(LifecycleEvent.START, new LifecycleAction<TimelineEventHandler>()
        {
            public void doAction(final TimelineEventHandler handler)
            {
                log.info("START event received: replaying on-disk events");
                handler.replay(config.getSpoolDir());
                handler.startHandlerThreads();
            }
        });
        lifecycledProvider.addListener(LifecycleEvent.STOP, new LifecycleAction<TimelineEventHandler>()
        {
            public void doAction(final TimelineEventHandler handler)
            {
                log.info("STOP event received: forcing commit of timelines");
                handler.commitAndShutdown();
            }
        });
        bind(TimelineEventHandler.class).toProvider(lifecycledProvider).asEagerSingleton();
        provider.addExportable(TimelineEventHandler.class);

        // Hook the real-time handler as needed
        // TODO - do we want to turn it off/on at runtime?
        if (config.isKafkaEnabled()) {
            log.info("Kafka producer configured");
            bind(KafkaEventHandler.class).asEagerSingleton();
            provider.addExportable(KafkaEventHandler.class);
        }

        bind(new TypeLiteral<List<EventHandler>>()
        {
        })
            .toProvider(provider)
            .asEagerSingleton();

        // Bind the Event filter
        try {
            final String eventFilterClass = config.getEventFilterClass();
            if (eventFilterClass == null) {
                bind(new TypeLiteral<Function<Event, Event>>()
                {
                }).annotatedWith(EventFilter.class).toInstance(Functions.<Event>identity());
            }
            else {
                bind(new TypeLiteral<Function<Event, Event>>()
                {
                }).annotatedWith(EventFilter.class).to((Class<? extends Function<Event, Event>>) Class.forName(eventFilterClass)).asEagerSingleton();
            }
        }
        catch (ClassNotFoundException e) {
            log.error("Unable to find Event filter class", e);
            bind(new TypeLiteral<Function<Event, Event>>()
            {
            }).annotatedWith(EventFilter.class).toInstance(Functions.<Event>identity());
        }
    }

    protected void configureExtraModules(final CollectorConfig config)
    {
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
}

