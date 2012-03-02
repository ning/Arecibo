package com.ning.arecibo.collector.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ning.arecibo.collector.ResolutionUtils;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jdbi.DBIProvider;
import com.ning.arecibo.util.service.DummyServiceLocator;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.arecibo.util.timeline.TimelineDAO;
import org.skife.config.ConfigurationObjectFactory;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

public class CollectorModule extends AbstractModule
{
    private static final Logger log = Logger.getLogger(CollectorModule.class);

    @Override
    public void configure()
    {
        CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);

        bind(CollectorConfig.class).toInstance(config);

        ResolutionUtils resUtils = new ResolutionUtils();

        configureDao();

        configureServiceLocator(config);

        bind(ResolutionUtils.class).toInstance(resUtils);

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(TimelineDAO.class).as("arecibo.collector:name=TimelineDAO");

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

    private void configureServiceLocator(CollectorConfig config)
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
        // set up db connection, with named statistics
        final Named moduleName = Names.named(CollectorConstants.COLLECTOR_DB);

        bind(DBI.class).annotatedWith(moduleName).toProvider(new DBIProvider(System.getProperties(), "arecibo.events.collector.db")).asEagerSingleton();
        bind(IDBI.class).annotatedWith(moduleName).to(Key.get(DBI.class, moduleName)).asEagerSingleton();
        bind(TimelineDAO.class).asEagerSingleton();
    }
}

