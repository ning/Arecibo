package com.ning.arecibo.alert;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.management.MBeanServer;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.confdata.guice.AlertDataModule;
import com.ning.arecibo.alert.guice.AlertServiceModule;
import com.ning.arecibo.alert.guice.SelfUUID;
import com.ning.arecibo.alert.manage.AlertEventProcessor;
import com.ning.arecibo.client.AggregatorClientModule;
import com.ning.arecibo.event.receiver.RESTEventReceiverModule;
import com.ning.arecibo.event.receiver.UDPEventReceiverModule;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.util.EmbeddedJettyConfig;
import com.ning.arecibo.util.EmbeddedJettyJerseyModule;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.lifecycle.Lifecycle;
import com.ning.arecibo.util.lifecycle.LifecycleEvent;
import com.ning.arecibo.util.lifecycle.LifecycleModule;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;
import org.eclipse.jetty.server.Server;

public class AreciboAlertService
{
    private final static Logger log = Logger.getLogger(AreciboAlertService.class);
    
    public static final String SERVICE_NAME = AreciboAlertService.class.getSimpleName();
    public static final String SERVICE_HOST = "host";
    public static final String SERVICE_PORT = "port"; 
    
    private final Lifecycle lifecycle;
    private final Server server;
    private final ServiceLocator serviceLocator;
    private final EmbeddedJettyConfig jettyConfig;
    private final Integer udpPort;
    private final UUID selfUUID;
    private final ConfigManager confStatusManager;

    @Inject
    private AreciboAlertService(Lifecycle lifecycle,
                                Server server,
                                ServiceLocator serviceLocator,
                                EmbeddedJettyConfig jettyConfig,
                                @SelfUUID UUID selfUUID,
                                @Named("UDPServerPort") int udpPort,
                                ConfigManager confStatusManager) {
        this.lifecycle = lifecycle;
        this.server = server;
        this.serviceLocator = serviceLocator;
        this.jettyConfig = jettyConfig;
        this.udpPort = udpPort;
        this.selfUUID = selfUUID;
        this.confStatusManager = confStatusManager;
    }
    
    private void run() {
        final long startTime = System.currentTimeMillis();
        log.info("Starting up Alert Service on port %d", jettyConfig.getPort());
        
        
        // Start the confStatusManager
        confStatusManager.start();
        
        Map<String, String> map = new HashMap<String, String>();
        map.put(SERVICE_HOST, jettyConfig.getHost());
        map.put(SERVICE_PORT, String.valueOf(jettyConfig.getPort())); 
        //map.put(EventService.HOST, localIp); // this is redundant
        map.put(EventService.JETTY_PORT, String.valueOf(jettyConfig.getPort()));
        map.put(EventService.UDP_PORT, String.valueOf(udpPort)); 
        ServiceDescriptor self=new ServiceDescriptor(selfUUID,SERVICE_NAME,map);
        serviceLocator.advertiseLocalService(self);
        
        try {
            lifecycle.fire(LifecycleEvent.START);
            server.start();
        }
        catch (Exception ex) {
            log.error(ex);
            return;
        }
        

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
        catch (InterruptedException ex) {
            // continue;
        }
        
        try {
            log.info("Shutting Down Alert Service");

            log.info("Stopping configStatusManager");
            confStatusManager.stop();

            log.info("Stopping lifecycle manager");
            lifecycle.fire(LifecycleEvent.STOP);

            log.info("Stopping jetty server");
            server.stop();
            
            // never gets here for some reason
            log.info("Shutdown completed");
        } 
        catch (Exception e) {
            log.warn(e);
        }
    }
    
    public static void main(String[] args)
    {
        Injector injector = Guice.createInjector(Stage.PRODUCTION, 
                                                 new LifecycleModule(),
                                                 new AbstractModule() {
                                                     @Override
                                                     protected void configure() {
                                                         bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
                                                     }
                                                 },
                                                 new AlertDataModule(),
                                                 // TODO: need to bind an implementation of ServiceLocator
                                                 new EmbeddedJettyJerseyModule(),
                                                 new RESTEventReceiverModule(AlertEventProcessor.class, "arecibo.alert:name=AlertEventProcessor"),
                                                 new UDPEventReceiverModule(),
                                                 new AggregatorClientModule(),
                                                 new AlertServiceModule());    

        AreciboAlertService service = injector.getInstance(AreciboAlertService.class);
        try {
            service.run();
        }
        catch (Exception e) {
            log.error(e, "Unable to start. Exiting.");
            System.exit(-1);
        }
    }
}
