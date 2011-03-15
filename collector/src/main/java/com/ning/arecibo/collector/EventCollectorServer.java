package com.ning.arecibo.collector;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import org.mortbay.jetty.Server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import com.ning.arecibo.collector.guice.CollectorModule;
import com.ning.arecibo.collector.guice.CollectorServiceName;
import com.ning.arecibo.collector.process.CollectorEventProcessor;
import com.ning.arecibo.event.receiver.RESTEventReceiverModule;
import com.ning.arecibo.event.receiver.UDPEventReceiverModule;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.util.EmbeddedJettyConfig;
import com.ning.arecibo.util.EmbeddedJettyJerseyModule;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.lifecycle.Lifecycle;
import com.ning.arecibo.util.lifecycle.LifecycleEvent;
import com.ning.arecibo.util.lifecycle.LifecycleModule;
import com.ning.arecibo.util.rmi.RMIModule;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;

public class EventCollectorServer
{
	private static final Logger log = Logger.getLogger(EventCollectorServer.class);
	private final Server server;
	private final Lifecycle lifecycle;
	private final ServiceLocator serviceLocator;
    private final String serviceName;
	private final EmbeddedJettyConfig config;
	private final int udpPort;
	private final int rmiPort;
	
    public static final String NAME = EventCollectorServer.class.getSimpleName();

	@Inject
	public EventCollectorServer(Server server,
	                            Lifecycle lifecycle,
	                            ServiceLocator serviceLocator,
                                @CollectorServiceName String serviceName,
                                EmbeddedJettyConfig config,
	                            @Named("UDPServerPort") int udpPort,
	                            @Named("RMIRegistryPort") int rmiPort)
	{
		this.server = server;
		this.lifecycle = lifecycle;
		this.serviceLocator = serviceLocator;
        this.serviceName = serviceName;
		this.config = config;
		this.udpPort = udpPort;
		this.rmiPort = rmiPort;
	}

	public void run() throws Exception
	{
		// advertise event endpoints
		Map<String, String> map = new HashMap<String, String>();
		map.put(EventService.HOST, config.getHost());
		map.put(EventService.JETTY_PORT, String.valueOf(config.getPort()));
		map.put(EventService.UDP_PORT, String.valueOf(udpPort));
		map.put(EventService.RMI_PORT, String.valueOf(rmiPort));
		ServiceDescriptor self=new ServiceDescriptor(serviceName,map);

		// advertise on beacon
		serviceLocator.advertiseLocalService(self);
		
		
		String name = getClass().getSimpleName();
		final long startTime = System.currentTimeMillis();
		log.info("Starting up %s on port %d", name, config.getPort());

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
		server.start();

		try {
			Thread.currentThread().join();
		}
		catch(InterruptedException e) {
			// continue
		}
		
		try {
			log.info("Shutting down %s", name);

			log.info("Stopping lifecycle");
			lifecycle.fire(LifecycleEvent.STOP);

			log.info("Stopping jetty server");
            server.stop();
			
			log.info("Shutdown completed");
		}
		catch(Exception e) {
			log.warn(e);
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		Injector injector = Guice.createInjector(Stage.PRODUCTION,
            new LifecycleModule(),
            // TODO: need to bind an implementation of ServiceLocator
            new EmbeddedJettyJerseyModule(),
			new RESTEventReceiverModule(CollectorEventProcessor.class, "arecibo.collector:name=CollectorEventProcessor"),
            new UDPEventReceiverModule(),
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
                }
            },
			new RMIModule(),
		    new CollectorModule());

		EventCollectorServer server = injector.getInstance(EventCollectorServer.class);
		server.run();
	}	
}
