package com.ning.arecibo.aggregator.impl;


import java.util.List;
import java.rmi.RemoteException;
import com.ning.arecibo.client.RemoteAggregatorService;
import com.ning.arecibo.lang.Aggregator;

public interface RemoteAggregatorRegistry extends RemoteAggregatorService
{
	List<Aggregator> getAggregators(String namespace) throws RemoteException;
	List<Aggregator> getAggregatorsExcluding(String namespace) throws RemoteException;
}
