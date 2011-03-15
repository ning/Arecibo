package com.ning.arecibo.event.publisher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.ning.arecibo.eventlogger.EventPublisher;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.NamedThreadFactory;
import com.ning.arecibo.util.service.ConsistentHashingServiceChooser;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.VirtualNodes;

public class EventPublisherModule implements Module
{
    final static Logger log = Logger.getLogger(EventPublisherModule.class);
    
	private final String senderType;

	public EventPublisherModule(String senderType)
	{
		this.senderType = senderType;
	}

	public void configure(Binder binder)
	{
	    binder.bindConstant().annotatedWith(VirtualNodes.class).to(Integer.getInteger("arecibo.consistent.hash.nodes", VirtualNodes.DEFAULT));
        binder.bindConstant().annotatedWith(MaxEventBufferSize.class).to(Integer.getInteger("arecibo.event.maxBufferSize", MaxEventBufferSize.DEFAULT));
        binder.bindConstant().annotatedWith(MaxEventDispatchers.class).to(Integer.getInteger("arecibo.event.maxDispatchers", MaxEventDispatchers.DEFAULT));
        binder.bindConstant().annotatedWith(MaxDrainDelayInMS.class).to(Long.getLong("arecibo.event.maxDrainDelayInMS", MaxDrainDelayInMS.DEFAULT));
        binder.bindConstant().annotatedWith(SpooledEventExpirationInMS.class).to(Long.getLong("arecibo.event.spool.expirationInMS", SpooledEventExpirationInMS.DEFAULT));
        binder.bindConstant().annotatedWith(LocalSpoolRoot.class).to(System.getProperty("arecibo.event.spool.path", LocalSpoolRoot.DEFAULT));
        binder.bindConstant().annotatedWith(EventServiceName.class).to(System.getProperty("arecibo.event.eventServiceName"));

        // need a local instance here
        final String eventServiceName = System.getProperty("arecibo.event.eventServiceName");

        binder.bind(ExecutorService.class).annotatedWith(PublisherExecutor.class).toInstance(				
				Executors.newFixedThreadPool(50, new NamedThreadFactory("EventPublisher"))
		);

        binder.bind(Selector.class)
		      .annotatedWith(PublisherSelector.class)
		      .toInstance(new Selector(){
			public boolean match(ServiceDescriptor sd)
			{
				return sd.getName().equals(eventServiceName);
			}
		});

		binder.bind(ConsistentHashingServiceChooser.class).asEagerSingleton();
        binder.bind(EventServiceChooser.class).to(AreciboEventServiceChooser.class).asEagerSingleton();
        binder.bind(EventPublisher.class).to(AreciboEventPublisher.class).asEagerSingleton();
        binder.bind(String.class).annotatedWith(EventSenderType.class).toInstance(senderType);

        ExportBuilder builder = MBeanModule.newExporter(binder);

        builder.export(AreciboEventServiceChooser.class).as("arecibo:type=AreciboEventServiceChooser");
        builder.export(AreciboEventPublisher.class).as("arecibo:name=EventPublisher");
	}
}
