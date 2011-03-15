package com.ning.arecibo.aggregator.esper.guice;

import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.ning.arecibo.aggregator.esper.EsperStatsManager;

public class EsperStatsModule implements Module
{
	public void configure(Binder binder)
	{
	    binder.bind(EsperStatsManager.class).asEagerSingleton();

	    ExportBuilder builder = MBeanModule.newExporter(binder);

        builder.export(EsperStatsManager.class).as("arecibo.esper:name=EsperStats");
    }
}
