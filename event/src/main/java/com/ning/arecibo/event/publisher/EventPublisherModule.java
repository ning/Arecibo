package com.ning.arecibo.event.publisher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.mogwee.executors.NamedThreadFactory;
import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.AbstractModule;
import com.ning.arecibo.eventlogger.EventPublisher;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.cron.JMXCronScheduler;
import com.ning.arecibo.util.service.ConsistentHashingConfig;
import com.ning.arecibo.util.service.ConsistentHashingSelector;
import com.ning.arecibo.util.service.ConsistentHashingServiceChooser;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceSelector;

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
	    ConfigurationObjectFactory configFactory = new ConfigurationObjectFactory(System.getProperties());
	    ConsistentHashingConfig consistentHashingConfig = configFactory.build(ConsistentHashingConfig.class);
	    EventPublisherConfig eventPublisherConfig = configFactory.build(EventPublisherConfig.class);

        bind(ConsistentHashingConfig.class).toInstance(consistentHashingConfig);
	    bind(EventPublisherConfig.class).toInstance(eventPublisherConfig);
        bind(ExecutorService.class).annotatedWith(PublisherExecutor.class).toInstance(				
				Executors.newFixedThreadPool(50, new NamedThreadFactory("EventPublisher"))
		);

        bind(Selector.class)
		    .annotatedWith(PublisherSelector.class)
		    .toInstance(new ServiceSelector(eventPublisherConfig.getEventServiceName()));
        bind(Selector.class)
            .annotatedWith(ConsistentHashingSelector.class)
            .toInstance(new ServiceSelector(eventPublisherConfig.getEventServiceName()));

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
