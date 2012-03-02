/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
