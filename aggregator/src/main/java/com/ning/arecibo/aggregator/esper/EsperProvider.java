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

package com.ning.arecibo.aggregator.esper;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;

import java.util.Collection;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class EsperProvider
{
	private static final ConcurrentHashMap<String, EPServiceProvider> map = new ConcurrentHashMap<String, EPServiceProvider>();

	public static Collection<EPServiceProvider> getAllProviders()
	{
		return map.values();
	}

	public static EPServiceProvider getProvider(String ns)
	{
		//return EPServiceProviderManager.getProvider(ns);
		EPServiceProvider ep = EPServiceProviderManager.getProvider(ns);
		if ( map.putIfAbsent(ns, ep) == null ) {
			// DO global ep registration stuff here
		}
		return map.get(ns) ;
	}

	public static void reset()
	{
		ArrayList<EPServiceProvider> list = new ArrayList<EPServiceProvider>(getAllProviders());
		for ( EPServiceProvider esp :  list) {
			esp.destroy();
		}
		map.clear();		
	}
}
