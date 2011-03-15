package com.ning.arecibo.client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.ning.arecibo.lang.Aggregator;


public interface RemoteAggregatorService extends Remote
{
    String DEFAULT_NS = "dynamic";

    void register(Aggregator agg) throws RemoteException;
    void register(Aggregator agg, long leaseTime, TimeUnit leaseTimeUnits) throws RemoteException;
	void unregister(String fullName) throws RemoteException;

	List<String> getAggregatorFullNames(String namespace) throws RemoteException;
	void ping() throws RemoteException;

	void softRestart() throws RemoteException;

}
