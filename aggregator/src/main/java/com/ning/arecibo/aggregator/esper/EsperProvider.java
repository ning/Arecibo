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
