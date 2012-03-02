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

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.TimeUnit;

public class Client
{
	private final static long defaultTimeWindow = TimeUnit.MILLISECONDS.convert(30L, TimeUnit.MINUTES);
	public static void main(String args[]) throws Exception
	{
		Registry registry = LocateRegistry.getRegistry(Integer.parseInt(args[0])) ;
		RemoteCollector collector = (RemoteCollector) registry.lookup(RemoteCollector.class.getSimpleName());

		for ( String host : collector.getHosts(System.currentTimeMillis() - defaultTimeWindow) ) {
			System.out.println(host);
//			for ( MapEvent event : collector.getLastValuesForHost(host).values() ) {
//				System.out.println(event);
//			}
		}

		for ( String x : collector.getTypes(System.currentTimeMillis() - defaultTimeWindow) ) {
			System.out.println(x);
//			for ( MapEvent event : collector.getLastValuesForType(x).values() ) {
//				System.out.println(event);
//			}
		}

		for ( String x : collector.getPaths(System.currentTimeMillis() - defaultTimeWindow) ) {
			System.out.println(x);			
//			for ( MapEvent event : collector.getLastValuesForPathWithType(x).values() ) {
//				System.out.println(event);
//			}
		}


	}
}
