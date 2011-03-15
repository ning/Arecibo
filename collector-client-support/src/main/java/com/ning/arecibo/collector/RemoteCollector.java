package com.ning.arecibo.collector;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Collection;
import java.util.List;
import com.ning.arecibo.event.MapEvent;

public interface RemoteCollector extends Remote
{
	public int[] getReductionFactors() throws RemoteException;
	public ResolutionTagGenerator getResolutionTagGenerator() throws RemoteException;

	public Map<String, MapEvent> getLastValuesForHost(long since,String host) throws RemoteException;
    public Map<String, MapEvent> getLastValuesForHost(long since,String host,String eventType) throws RemoteException;

	public Map<String, MapEvent> getLastValuesForType(long since,String type) throws RemoteException;
    public Map<String, MapEvent> getLastValuesForType(long since,String type,String eventType) throws RemoteException;

	public Map<String, MapEvent> getLastValuesForPathWithType(long since,String path,String type) throws RemoteException;
    public Map<String, MapEvent> getLastValuesForPathWithType(long since,String path,String type,String eventType) throws RemoteException;

    public List<String> getLastEventTypesForHost(long since,String host) throws RemoteException;
    public List<String> getLastEventTypesForType(long since,String type) throws RemoteException;
    public List<String> getLastEventTypesForPathWithType(long since,String path,String type) throws RemoteException;

	public Collection<String> getHosts() throws RemoteException;
	public Collection<String> getHosts(long since) throws RemoteException;
	public Collection<String> getHosts(long since, String type) throws RemoteException;
	public Collection<String> getHosts(long since, String type, String path) throws RemoteException;

	public Collection<String> getTypes() throws RemoteException;
	public Collection<String> getTypes(long since) throws RemoteException;
	public Collection<String> getTypes(long since, String type) throws RemoteException;

	public Collection<String> getPaths() throws RemoteException;
	public Collection<String> getPaths(long since) throws RemoteException;
	public Collection<String> getPaths(long since, String type) throws RemoteException;
}
