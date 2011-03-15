package com.ning.arecibo.dashboard.dao;

import java.util.ArrayList;
import java.util.List;

import static com.ning.arecibo.dashboard.dao.ResolutionRequestType.*;

public class ResolutionRequest {
	
	final ResolutionRequestType requestType;
	final int requestVal;
	volatile Integer selectedReduction = null;
	
	public ResolutionRequest(ResolutionRequestType requestType) {
		this(requestType,-1);
	}
	
	public ResolutionRequest(ResolutionRequestType requestType,int requestVal) {
		this.requestType = requestType;
		this.requestVal = requestVal;
	}
	
	public ResolutionRequestType getRequestType() {
		return this.requestType;
	}
	
	public List<Integer> getPreferredRequestSequence(long baseReductionMillis,long timeWindow,int[] availableReductionFactors) {
		// assume inconming available factors are in order from lowest reduction to highest reduction
		if(requestType == HIGHEST) {
			List<Integer> retList = new ArrayList<Integer>(1);
			retList.add(availableReductionFactors[0]);
			return retList;
		}
		if(requestType == HIGHESTAVAIL) {
			List<Integer> retList = new ArrayList<Integer>(1);
			for(int index = 0; index < availableReductionFactors.length; index++) {
				retList.add(availableReductionFactors[index]);
			}
			return retList;
		}
		else if(requestType == LOWEST) {
			List<Integer> retList = new ArrayList<Integer>(1);
			retList.add(availableReductionFactors[availableReductionFactors.length-1]);
			return retList;
		}
		else if(requestType == LOWESTAVAIL) {
			List<Integer> retList = new ArrayList<Integer>(1);
			for(int index = availableReductionFactors.length - 1; index >= 0; index--) {
				retList.add(availableReductionFactors[index]);
			}
			return retList;
		}
		else if(requestType == FIXED) {
			int fixedReduction = this.requestVal;
			List<Integer> retList = new ArrayList<Integer>(1);
			retList.add(fixedReduction);
			return retList;
		}
		else if(requestType == BEST_FIT) {
			List<Integer> retList = new ArrayList<Integer>(availableReductionFactors.length);
			
			int bestFit = getBestFitReductionFactor(baseReductionMillis,timeWindow,availableReductionFactors);
			retList.add(bestFit);
			
			// loop through the higher reductions
			int nextReduction = bestFit;
			while((nextReduction = getNextHigherReductionFactor(availableReductionFactors,nextReduction)) != -1) {
				retList.add(nextReduction);
			}
			
			// loop through the lower reductions (as a last resort!)
			nextReduction = bestFit;
			while((nextReduction = getNextLowerReductionFactor(availableReductionFactors,nextReduction)) != -1) {
				retList.add(nextReduction);
			}
			
			return retList;
		}
		else {
			// shouldn't get here
			return null;
		}
	}
	
	public void setSelectedReduction(int selectedReduction) {
		this.selectedReduction = selectedReduction;
	}
	
	public Integer getSelectedReduction() {
		return this.selectedReduction;
	}
	
	private int getBestFitReductionFactor(long baseReductionMillis,long timeWindow,int[] reductionFactors) {
		
		int maxDataPoints = this.requestVal;
		
		// should optimize this search, pre-calculate range buckets
		long baseDataPoints = timeWindow / baseReductionMillis;
		
		for(int reductionFactor:reductionFactors) {
			long numDataPoints = baseDataPoints / (long)reductionFactor;
			if(numDataPoints <= maxDataPoints)
				return reductionFactor;
		}
		
		// return lowest res we have if we get here
		return reductionFactors[reductionFactors.length - 1];
	}

	private int getNextHigherReductionFactor(int[] reductionFactors,int startReductionFactor) {
		
		// find the next higher reduction factor in the sequence
		int lastReductionFactor = -1;
		for(int redFactor:reductionFactors) {
			
			if(lastReductionFactor == startReductionFactor)
				return redFactor;
			
			lastReductionFactor = redFactor;
		}
		
		return -1;
	}

	private int getNextLowerReductionFactor(int[] reductionFactors,int startReductionFactor) {
		
		// find the next lower reduction factor in the sequence
		int lastReductionFactor = -1;
		for(int redFactor:reductionFactors) {
			
			if(redFactor == startReductionFactor)
				return lastReductionFactor;
			
			lastReductionFactor = redFactor;
		}
		
		return -1;
	}	
}
