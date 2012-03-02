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

package com.ning.arecibo.aggregator.impl;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jetty.server.Server;
import org.weakref.jmx.Managed;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.ning.arecibo.aggregator.guice.SelfUUID;
import com.ning.arecibo.aggregator.plugin.AreciboMonitoringPlugin;
import com.ning.arecibo.client.RemoteAggregatorService;
import com.ning.arecibo.event.publisher.AreciboEventServiceChooser;
import com.ning.arecibo.event.publisher.EventPublisherConfig;
import com.ning.arecibo.event.publisher.EventServiceChooser;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.lang.Aggregator;
import com.ning.arecibo.util.EmbeddedJettyConfig;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.lifecycle.Lifecycle;
import com.ning.arecibo.util.lifecycle.LifecycleEvent;
import com.ning.arecibo.util.lifecycle.LifecycleListener;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.arecibo.util.service.ServiceNotAvailableException;

public class AggregatorServer
{
	private static final Logger log = Logger.getLogger(AggregatorServer.class);

	private final Server server;
	private final Lifecycle lifecycle;
	private final EmbeddedJettyConfig jettyConfig;
	private final AreciboEventServiceChooser chooser;
	private final AggregatorServiceImpl aggregatorServiceImpl;

	@Inject
	public AggregatorServer(Server server,
                            Lifecycle lifecycle,
                            final ServiceLocator serviceLocator,
                            EmbeddedJettyConfig jettyConfig,
                            final EventServiceChooser chooser,
                            EventPublisherConfig eventPublisherConfig,
                            @Named("UDPServerPort") int udpPort,
                            @SelfUUID UUID selfUUID,
                            Registry registry,
                            @Named("RMIRegistryPort") int rmiPort,
                            AggregatorRegistry aggregatorRegistry,
                            AggregatorServiceImpl aggregatorServiceImpl) throws AlreadyBoundException, RemoteException
    {
		this.server = server;
		this.lifecycle = lifecycle;
		this.jettyConfig = jettyConfig;
		this.chooser = (AreciboEventServiceChooser)chooser;
	    this.aggregatorServiceImpl = aggregatorServiceImpl;

	    registry.bind(RemoteAggregatorService.class.getSimpleName(), aggregatorServiceImpl);
	    registry.bind(AggregatorRegistry.class.getSimpleName(), aggregatorRegistry);

        Map<String, String> map = new HashMap<String, String>();
		map.put(EventService.HOST, jettyConfig.getHost());
		map.put(EventService.JETTY_PORT, String.valueOf(jettyConfig.getPort()));
		map.put(EventService.UDP_PORT, String.valueOf(udpPort));
		map.put(EventService.RMI_PORT, String.valueOf(rmiPort));

		ServiceDescriptor self = new ServiceDescriptor(selfUUID, eventPublisherConfig.getEventServiceName(), map);
		serviceLocator.advertiseLocalService(self);

		lifecycle.addListener(LifecycleEvent.STOP, new LifecycleListener()
		{
			public void onEvent(LifecycleEvent e)
			{
				chooser.stop();
				serviceLocator.stop();
			}
		});

		chooser.start();

	    try {
		    RemoteAggregatorRegistry aggReg = aggregatorServiceImpl.getRemoteAggregatorRegistryExcluding(self);
		    log.info("synchronizing aggregator defintions ...");
		    List<Aggregator> list = aggReg.getAggregatorsExcluding(AreciboMonitoringPlugin.NS);
		    for ( Aggregator a : list ) {
			    aggregatorRegistry.register(a);
		    }
	    }
	    catch (ServiceNotAvailableException e) {
		    log.info("no other service with name '%s' available, skipping synchronization", eventPublisherConfig.getEventServiceName());
	    }
    }

	@Managed
	public void softRestart()
	{
		try {
			aggregatorServiceImpl.softRestart();
		}
		catch (RemoteException e) {
			log.error(e);
		}
	}

	public void run() throws Exception
	{
		String name = getClass().getSimpleName();
		final long startTime = System.currentTimeMillis();
		log.info("Starting up %s on port %d", name, jettyConfig.getPort());

		lifecycle.fire(LifecycleEvent.START);

		final long secondsToStart = (System.currentTimeMillis() - startTime) / 1000;
		log.info("STARTUP COMPLETE: server started in %d:%02d", secondsToStart / 60, secondsToStart % 60);

		final Thread t = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				t.interrupt();
			}
		});

		try {
			Thread.currentThread().join();
		}
		catch(InterruptedException e) {
			// continue;
		}
		
		try {
			log.info("Shutting down %s", name);
			
			log.info("Stopping lifecycle");
			lifecycle.fire(LifecycleEvent.STOP);

			log.info("Stopping chooser");
			chooser.stop();
			
            log.info("Stopping server");
            server.stop();

            log.info("Shutdown completed");
		}
		catch(Exception e) {
			log.warn(e);
		}
	}
}
