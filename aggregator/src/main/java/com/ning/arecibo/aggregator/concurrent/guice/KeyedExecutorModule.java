package com.ning.arecibo.aggregator.concurrent.guice;

import java.util.concurrent.ExecutorService;
import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.AbstractModule;
import com.ning.arecibo.aggregator.concurrent.KeyedExecutor;

public class KeyedExecutorModule extends AbstractModule
{
    @Override
	public void configure()
	{
        KeyedExecutorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(KeyedExecutorConfig.class);

        bind(KeyedExecutorConfig.class).toInstance(config);
        bind(ExecutorService.class).annotatedWith(KeyedExecutorSupport.class).toProvider(KeyedExecutorThreadPoolProvider.class).asEagerSingleton();
	    bind(KeyedExecutor.class).asEagerSingleton();

	    ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(KeyedExecutor.class).as("arecibo.concurrent:name=KeyedExecutor");
    }
}
