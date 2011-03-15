package com.ning.arecibo.collector.dao;

import java.util.concurrent.atomic.AtomicLong;

public class EventTableDescriptor {
	
	private final AggregationType aggType;
	private final int reductionFactor;
	private final String resolutionTag;
	private final int splitIntervalInMinutes;
	private final int numPartitionsToKeep;
	private final int splitNumPartitionsAhead;
	private final AtomicLong maxTs;
	
	public EventTableDescriptor(AggregationType aggType,
										int reductionFactor,
										String resolutionTag,
										int numPartitionsToKeep,
										int splitIntervalInMinutes,
										int splitNumPartitionsAhead) {
		
		this.aggType = aggType;
		this.reductionFactor = reductionFactor;
		this.resolutionTag = resolutionTag;
		this.splitIntervalInMinutes = splitIntervalInMinutes;
		this.numPartitionsToKeep = numPartitionsToKeep;
		this.splitNumPartitionsAhead = splitNumPartitionsAhead;
		this.maxTs = new AtomicLong(System.currentTimeMillis());
	}
	
	public AggregationType getAggregationType() {
		return aggType;
	}
	
	public int getReductionFactor() {
		return reductionFactor;
	}
	
	public int getSplitIntervalInMinutes() {
		return splitIntervalInMinutes;
	}
	
	public int getNumPartitionsToKeep() {
		return numPartitionsToKeep;
	}
	
	public int getSplitNumPartitionsAhead() {
		return splitNumPartitionsAhead;
	}
	
	public String getHashKey() {
		return getHashKey(this.aggType,this.resolutionTag);
	}
	
	public static String getHashKey(AggregationType aggType,String resolutionTag) {
		return aggType.toString() + ":" + resolutionTag;
	}
	
	public long getMaxTs() {
		return this.maxTs.get();
	}
	
	public void setMaxTs(long maxTs) {
		this.maxTs.set(maxTs);
	}
}
