package com.ning.arecibo.collector.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ning.arecibo.collector.ResolutionUtils;
import com.ning.arecibo.collector.dao.AggregationType;
import com.ning.arecibo.collector.dao.EventTableDescriptor;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jdbi.DBIProvider;
import com.ning.arecibo.util.timeline.TimelineDAO;
import org.skife.config.ConfigurationObjectFactory;
import org.skife.config.TimeSpan;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

import java.util.HashMap;
import java.util.Map;

public class CollectorModule extends AbstractModule
{

    final static Logger log = Logger.getLogger(CollectorModule.class);

    @Override
    public void configure()
    {
        CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);

        bind(CollectorConfig.class).toInstance(config);

        ResolutionUtils resUtils = new ResolutionUtils();
        Map<String, EventTableDescriptor> tableDescriptors = getEventTableDescriptors(resUtils, config);
        bind(new TypeLiteral<Map<String, EventTableDescriptor>>()
        {
        }).annotatedWith(EventTableDescriptors.class).toInstance(tableDescriptors);

        configureDao();

        bind(ResolutionUtils.class).toInstance(resUtils);

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(TimelineDAO.class).as("arecibo.collector:name=TimelineDAO");
    }

    protected void configureDao()
    {
        // set up db connection, with named statistics
        final Named moduleName = Names.named(CollectorConstants.COLLECTOR_DB);

        bind(DBI.class).annotatedWith(moduleName).toProvider(new DBIProvider(System.getProperties(), "arecibo.events.collector.db")).asEagerSingleton();
        bind(IDBI.class).annotatedWith(moduleName).to(Key.get(DBI.class, moduleName)).asEagerSingleton();
        bind(TimelineDAO.class).asEagerSingleton();
    }

    private Map<String, EventTableDescriptor> getEventTableDescriptors(ResolutionUtils resUtils,
                                                                       CollectorConfig collectorConfig)
    {
        // get the size of the largest list
        int numDescriptorCombos = Math.max(collectorConfig.getReductionFactors().length,
            Math.max(collectorConfig.getNumPartitionsToKeep().length,
                Math.max(collectorConfig.getSplitIntervalsInMinutes().length,
                    collectorConfig.getNumPartitionsToSplitAhead().length)));

        // parse lists in order, and ones that run out of items will just replicate the last one in the list
        // TODO: cleanup this config ugliness!
        HashMap<String, EventTableDescriptor> tableDescriptors = new HashMap<String, EventTableDescriptor>();

        int currReductionFactor = 0;
        int currNumPartitionsToKeep = 0;
        TimeSpan currSplitInterval = null;
        int currNumPartitionsToSplitAhead = 0;

        for (int idx = 0; idx < numDescriptorCombos; idx++) {
            // note, if any of these have a format exception, will throw a Runtimer out of here (which is ok)
            if (idx < collectorConfig.getReductionFactors().length) {
                currReductionFactor = collectorConfig.getReductionFactors()[idx];
            }
            if (idx < collectorConfig.getNumPartitionsToKeep().length) {
                currNumPartitionsToKeep = collectorConfig.getNumPartitionsToKeep()[idx];
            }
            if (idx < collectorConfig.getSplitIntervalsInMinutes().length) {
                currSplitInterval = collectorConfig.getSplitIntervalsInMinutes()[idx];
            }
            if (idx < collectorConfig.getNumPartitionsToSplitAhead().length) {
                currNumPartitionsToSplitAhead = collectorConfig.getNumPartitionsToSplitAhead()[idx];
            }

            for (AggregationType aggType : AggregationType.values()) {
                if (idx > 0 && !aggType.getSupportsMultiRes()) {
                    continue;
                }

                EventTableDescriptor tableDescriptor = new EventTableDescriptor(aggType,
                    currReductionFactor,
                    resUtils.getResolutionTag(currReductionFactor),
                    currNumPartitionsToKeep,
                    currSplitInterval,
                    currNumPartitionsToSplitAhead);

                String hashKey = tableDescriptor.getHashKey();
                tableDescriptors.put(hashKey, tableDescriptor);
            }
        }

        return tableDescriptors;

    }
}

