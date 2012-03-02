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
