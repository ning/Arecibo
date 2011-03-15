package com.ning.arecibo.collector.guice;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ning.arecibo.collector.ResolutionUtils;
import com.ning.arecibo.collector.dao.AggregationType;
import com.ning.arecibo.collector.dao.CollectorDAO;
import com.ning.arecibo.collector.dao.EventTableDescriptor;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jdbi.DBIProvider;

public class CollectorModule extends AbstractModule {
	
    final static Logger log = Logger.getLogger(CollectorModule.class);
    
    @Override
	public void configure()
	{
        bindConstant().annotatedWith(BufferWindowInSeconds.class).to(Integer.getInteger(BufferWindowInSeconds.PROPERTY_NAME, BufferWindowInSeconds.DEFAULT));
        bindConstant().annotatedWith(CollectorReadOnlyMode.class).to(Boolean.parseBoolean(System.getProperty(CollectorReadOnlyMode.PROPERTY_NAME, Boolean.toString(CollectorReadOnlyMode.DEFAULT))));
		bindConstant().annotatedWith(HostUpdateInterval.class).to(Long.getLong(HostUpdateInterval.PROPERTY_NAME, HostUpdateInterval.DEFAULT));
		bindConstant().annotatedWith(ThrottlePctFreeThreshold.class).to(Integer.getInteger(ThrottlePctFreeThreshold.PROPERTY_NAME, ThrottlePctFreeThreshold.DEFAULT));
		bindConstant().annotatedWith(TableSpaceName.class).to(System.getProperty(TableSpaceName.PROPERTY_NAME, TableSpaceName.DEFAULT));
		bindConstant().annotatedWith(CollectorServiceName.class).to(System.getProperty(CollectorServiceName.PROPERTY_NAME, CollectorServiceName.DEFAULT));
		bindConstant().annotatedWith(TablespaceStatsUpdateIntervalMinutes.class).to(Integer.getInteger(TablespaceStatsUpdateIntervalMinutes.PROPERTY_NAME, TablespaceStatsUpdateIntervalMinutes.DEFAULT));
		bindConstant().annotatedWith(MaxTableSpaceMB.class).to(Integer.getInteger(MaxTableSpaceMB.PROPERTY_NAME, MaxTableSpaceMB.DEFAULT));
		bindConstant().annotatedWith(MaxSplitAndSweepInitialDelayMinutes.class).to(Integer.getInteger(MaxSplitAndSweepInitialDelayMinutes.PROPERTY_NAME, MaxSplitAndSweepInitialDelayMinutes.DEFAULT));
		bindConstant().annotatedWith(MaxTriageThreads.class).to(Integer.getInteger(MaxTriageThreads.PROPERTY_NAME, MaxTriageThreads.DEFAULT));
		bindConstant().annotatedWith(MaxBatchInsertThreads.class).to(Integer.getInteger(MaxBatchInsertThreads.PROPERTY_NAME, MaxBatchInsertThreads.DEFAULT));
		bindConstant().annotatedWith(MinBatchInsertSize.class).to(Integer.getInteger(MinBatchInsertSize.PROPERTY_NAME, MinBatchInsertSize.DEFAULT));
		bindConstant().annotatedWith(MaxBatchInsertSize.class).to(Integer.getInteger(MaxBatchInsertSize.PROPERTY_NAME, MaxBatchInsertSize.DEFAULT));
		bindConstant().annotatedWith(MaxAsynchTriageQueueSize.class).to(Integer.getInteger(MaxAsynchTriageQueueSize.PROPERTY_NAME, MaxAsynchTriageQueueSize.DEFAULT));
		bindConstant().annotatedWith(MaxAsynchInsertQueueSize.class).to(Integer.getInteger(MaxAsynchInsertQueueSize.PROPERTY_NAME, MaxAsynchInsertQueueSize.DEFAULT));
		bindConstant().annotatedWith(MaxPendingEvents.class).to(Integer.getInteger(MaxPendingEvents.PROPERTY_NAME, MaxPendingEvents.DEFAULT));
		bindConstant().annotatedWith(MaxPendingEventsCheckIntervalMs.class).to(Long.getLong(MaxPendingEventsCheckIntervalMs.PROPERTY_NAME, MaxPendingEventsCheckIntervalMs.DEFAULT));
		bindConstant().annotatedWith(EnableBatchRetryOnIntegrityViolation.class).to(Boolean.parseBoolean(System.getProperty(EnableBatchRetryOnIntegrityViolation.PROPERTY_NAME, Boolean.toString(EnableBatchRetryOnIntegrityViolation.DEFAULT))));
		bindConstant().annotatedWith(EnableDuplicateEventLogging.class).to(Boolean.parseBoolean(System.getProperty(EnableDuplicateEventLogging.PROPERTY_NAME, Boolean.toString(EnableDuplicateEventLogging.DEFAULT))));
		bindConstant().annotatedWith(EnablePerTableInserts.class).to(Boolean.parseBoolean(System.getProperty(EnablePerTableInserts.PROPERTY_NAME, Boolean.toString(EnablePerTableInserts.DEFAULT))));
		bindConstant().annotatedWith(EnablePreparedBatchInserts.class).to(Boolean.parseBoolean(System.getProperty(EnablePreparedBatchInserts.PROPERTY_NAME, Boolean.toString(EnablePreparedBatchInserts.DEFAULT))));

		String reductionFactorList = System.getProperty(ReductionFactorList.PROPERTY_NAME, ReductionFactorList.DEFAULT);
		String numPartitionsToKeepList = System.getProperty(NumPartitionsToKeepList.PROPERTY_NAME, NumPartitionsToKeepList.DEFAULT);
		String splitIntervalInMinutesList = System.getProperty(SplitIntervalInMinutesList.PROPERTY_NAME, SplitIntervalInMinutesList.DEFAULT);
		String numPartitionsToSplitAheadList = System.getProperty(NumPartitionsToSplitAheadList.PROPERTY_NAME, NumPartitionsToSplitAheadList.DEFAULT);
		
		int[] reductionFactors = getReductionFactors(reductionFactorList);
		bind(new TypeLiteral<int[]>(){}).annotatedWith(ReductionFactors.class).toInstance(reductionFactors);
		
		ResolutionUtils resUtils = new ResolutionUtils();
		Map<String,EventTableDescriptor> tableDescriptors = getEventTableDescriptors(resUtils,
																						reductionFactorList,
																						numPartitionsToKeepList,
																						splitIntervalInMinutesList,
																						numPartitionsToSplitAheadList);
		bind(new TypeLiteral<Map<String,EventTableDescriptor>>(){}).annotatedWith(EventTableDescriptors.class).toInstance(tableDescriptors);

        // set up db connection, with named statistics
        final Named moduleName = Names.named(CollectorConstants.COLLECTOR_DB);

        bind(DBI.class).annotatedWith(moduleName).toProvider(new DBIProvider(System.getProperties(), "arecibo.events.collector.db")).asEagerSingleton();
        bind(IDBI.class).annotatedWith(moduleName).to(Key.get(DBI.class, moduleName));
        bind(CollectorDAO.class).asEagerSingleton();

		bind(ResolutionUtils.class).toInstance(resUtils);

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(CollectorDAO.class).as("arecibo.collector:name=CollectorDAO");
	}
	
	private int[] getReductionFactors(String reductionFactorList) {
		StringTokenizer reductionFactorST = new StringTokenizer(reductionFactorList,",");
		int count = reductionFactorST.countTokens();
		
		int[] reductionFactors = new int[count];
		
		int i=0;
		while(reductionFactorST.hasMoreTokens()) {
			// note, if this has a number format exception, it will throw a Runtimer out of here (which is ok)
			reductionFactors[i++] = Integer.parseInt(reductionFactorST.nextToken());
		}
		
		return reductionFactors;
	}
	
	private Map<String,EventTableDescriptor> getEventTableDescriptors(ResolutionUtils resUtils,
																		String reductionFactorList,
																		String numPartitionsToKeepList,
																		String splitIntervalInMinutesList,
																		String numPartitionsToSplitAheadList) {
		
		StringTokenizer reductionFactors = new StringTokenizer(reductionFactorList,",");
		StringTokenizer numPartitionsToKeep = new StringTokenizer(numPartitionsToKeepList,",");
		StringTokenizer splitIntervalInMinutes = new StringTokenizer(splitIntervalInMinutesList,",");
		StringTokenizer numPartitionsToSplitAhead = new StringTokenizer(numPartitionsToSplitAheadList,",");
		
		// get the size of the largest list
		int numDescriptorCombos = Math.max(reductionFactors.countTokens(),
									Math.max(numPartitionsToKeep.countTokens(),
									Math.max(splitIntervalInMinutes.countTokens(),
												numPartitionsToSplitAhead.countTokens())));
		
		// parse lists in order, and ones that run out of items will just replicate the last one in the list
		// TODO: cleanup this config ugliness!
		HashMap<String,EventTableDescriptor> tableDescriptors = new HashMap<String,EventTableDescriptor>();
		
		int currReductionFactor = 0;
		int currNumPartitionsToKeep = 0;
		int currSplitIntervalInMinutes = 0;
		int currNumPartitionsToSplitAhead = 0;
		
		for(int i=0;i<numDescriptorCombos;i++) {
			
			// note, if any of these have a number format exception, will throw a Runtimer out of here (which is ok)
			if(reductionFactors.hasMoreTokens())
				currReductionFactor = Integer.parseInt(reductionFactors.nextToken());
			if(numPartitionsToKeep.hasMoreTokens())
				currNumPartitionsToKeep = Integer.parseInt(numPartitionsToKeep.nextToken());
			if(splitIntervalInMinutes.hasMoreTokens())
				currSplitIntervalInMinutes = Integer.parseInt(splitIntervalInMinutes.nextToken());
			if(numPartitionsToSplitAhead.hasMoreTokens())
				currNumPartitionsToSplitAhead = Integer.parseInt(numPartitionsToSplitAhead.nextToken());
		
			for(AggregationType aggType:AggregationType.values()) {
				
				if(i > 0 && !aggType.getSupportsMultiRes())
					continue;
				
				EventTableDescriptor tableDescriptor = new EventTableDescriptor(aggType,
																					currReductionFactor,
																					resUtils.getResolutionTag(currReductionFactor),
																					currNumPartitionsToKeep,
																					currSplitIntervalInMinutes,
																					currNumPartitionsToSplitAhead);
				
				String hashKey = tableDescriptor.getHashKey();
				tableDescriptors.put(hashKey,tableDescriptor);
			}
		}
		
		return tableDescriptors;
		
	}
}

