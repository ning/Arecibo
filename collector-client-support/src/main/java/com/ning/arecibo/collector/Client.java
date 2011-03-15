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
