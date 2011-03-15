package com.ning.arecibo.aggregator.concurrent.guice;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.ning.arecibo.aggregator.concurrent.KeyedExecutor;

public class KeyedExecutorModule implements Module
{
	public void configure(Binder binder)
	{
		binder.bind(ExecutorService.class).annotatedWith(KeyedExecutorSupport.class).toProvider(FixedThreadPoolProvider.class).asEagerSingleton();
	    binder.bind(KeyedExecutor.class).asEagerSingleton();

	    ExportBuilder builder = MBeanModule.newExporter(binder);

        builder.export(KeyedExecutor.class).as("arecibo.concurrent:name=KeyedExecutor");
    }

	static class FixedThreadPoolProvider implements Provider<ExecutorService>
	{
		public ExecutorService get()
		{
			return Executors.newFixedThreadPool(Integer.getInteger(KeyedExecutorNumThreads.PROPERTY_NAME, KeyedExecutorNumThreads.DEFAULT));
		}
	}
}
