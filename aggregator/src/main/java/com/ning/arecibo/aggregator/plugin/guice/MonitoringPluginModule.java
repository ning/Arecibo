package com.ning.arecibo.aggregator.plugin.guice;

import java.util.StringTokenizer;

import com.google.inject.AbstractModule;

public class MonitoringPluginModule extends AbstractModule
{
    @Override
    public void configure()
    {
        bindConstant().annotatedWith(ReceiverServiceName.class).to(System.getProperty(ReceiverServiceName.PROPERTY_NAME));
        bindConstant().annotatedWith(BaseLevelTimeWindowSeconds.class).to(Integer.getInteger(BaseLevelTimeWindowSeconds.PROPERTY_NAME, BaseLevelTimeWindowSeconds.DEFAULT));
        bindConstant().annotatedWith(BaseLevelBatchIntervalSeconds.class).to(Integer.getInteger(BaseLevelBatchIntervalSeconds.PROPERTY_NAME, BaseLevelBatchIntervalSeconds.DEFAULT));
        
		String reductionFactorList = System.getProperty(ReductionFactorList.PROPERTY_NAME, ReductionFactorList.DEFAULT);
		int[] reductionFactors = getReductionFactors(reductionFactorList);
		
		bind(int[].class).annotatedWith(ReductionFactors.class).toInstance(reductionFactors);
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
}
