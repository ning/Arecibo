package com.ning.arecibo.client;

import com.google.inject.Module;
import com.google.inject.Binder;

public class AggregatorClientModule implements Module
{
	public void configure(Binder binder)
	{
		binder.bind(AggregatorService.class).asEagerSingleton();
	}
}
