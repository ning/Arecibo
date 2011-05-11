package com.ning.arecibo.event.publisher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.AbstractModule;
import com.ning.arecibo.eventlogger.EventPublisher;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.NamedThreadFactory;
import com.ning.arecibo.util.cron.JMXCronScheduler;
import com.ning.arecibo.util.service.ConsistentHashingServiceChooser;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.VirtualNodes;

public class EventPublisherModule extends AbstractModule
{
    final static Logger log = Logger.getLogger(EventPublisherModule.class);
    
	private final String senderType;

	public EventPublisherModule(String senderType)
	{
		this.senderType = senderType;
	}

	@Override
	public void configure()
	{
	    bindConstant().annotatedWith(VirtualNodes.class).to(Integer.getInteger("arecibo.consistent.hash.nodes", VirtualNodes.DEFAULT));
        bindConstant().annotatedWith(MaxEventBufferSize.class).to(Integer.getInteger("arecibo.event.maxBufferSize", MaxEventBufferSize.DEFAULT));
        bindConstant().annotatedWith(MaxEventDispatchers.class).to(Integer.getInteger("arecibo.event.maxDispatchers", MaxEventDispatchers.DEFAULT));
        bindConstant().annotatedWith(MaxDrainDelayInMS.class).to(Long.getLong("arecibo.event.maxDrainDelayInMS", MaxDrainDelayInMS.DEFAULT));
        bindConstant().annotatedWith(SpooledEventExpirationInMS.class).to(Long.getLong("arecibo.event.spool.expirationInMS", SpooledEventExpirationInMS.DEFAULT));
        bindConstant().annotatedWith(LocalSpoolRoot.class).to(System.getProperty("arecibo.event.spool.path", LocalSpoolRoot.DEFAULT));
        bindConstant().annotatedWith(EventServiceName.class).to(System.getProperty("arecibo.event.eventServiceName"));

        // need a local instance here
        final String eventServiceName = System.getProperty("arecibo.event.eventServiceName");

        bind(ExecutorService.class).annotatedWith(PublisherExecutor.class).toInstance(				
				Executors.newFixedThreadPool(50, new NamedThreadFactory("EventPublisher"))
		);

        bind(Selector.class)
		      .annotatedWith(PublisherSelector.class)
		      .toInstance(new Selector(){
			public boolean match(ServiceDescriptor sd)
			{
				return sd.getName().equals(eventServiceName);
			}
		});

		bind(ConsistentHashingServiceChooser.class).asEagerSingleton();
		bind(ScheduledExecutorService.class).annotatedWith(JMXCronScheduler.class).toInstance(Executors.newScheduledThreadPool(1));
        bind(EventServiceChooser.class).to(AreciboEventServiceChooser.class).asEagerSingleton();
        bind(EventPublisher.class).to(AreciboEventPublisher.class).asEagerSingleton();
        bind(String.class).annotatedWith(EventSenderType.class).toInstance(senderType);

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(AreciboEventServiceChooser.class).as("arecibo:type=AreciboEventServiceChooser");
        builder.export(AreciboEventPublisher.class).as("arecibo:name=EventPublisher");
	}
}
