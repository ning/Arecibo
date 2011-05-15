package com.ning.arecibo.event.publisher;

import java.util.concurrent.ExecutorService;
import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.AbstractModule;
import com.ning.arecibo.eventlogger.EventPublisher;
import com.ning.arecibo.util.FailsafeScheduledExecutor;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.NamedThreadFactory;
import com.ning.arecibo.util.service.RandomSelector;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceSelector;

public class HdfsEventPublisherModule extends AbstractModule
{
    final static Logger log = Logger.getLogger(HdfsEventPublisherModule.class);

	private final String senderType;
	private final String serviceName;

	public HdfsEventPublisherModule(String senderType, String serviceName)
	{
		this.senderType = senderType;
		this.serviceName = serviceName;
	}

	@Override
	public void configure()
	{
		bind(ExecutorService.class).annotatedWith(PublisherExecutor.class).toInstance(
			new FailsafeScheduledExecutor(50, new NamedThreadFactory("EventPublisher"))
		);

        bind(Selector.class).annotatedWith(RandomSelector.class).toInstance(new ServiceSelector(serviceName));

        EventPublisherConfig config = new ConfigurationObjectFactory(System.getProperties()).build(EventPublisherConfig.class);

        bind(EventPublisherConfig.class).toInstance(config);
        bind(EventPublisher.class).to(HdfsEventPublisher.class).asEagerSingleton();
        bind(String.class).annotatedWith(EventSenderType.class).toInstance(senderType);
        bind(EventServiceChooser.class).to(RandomEventServiceChooser.class).asEagerSingleton();
        bind(HdfsEventPublisher.class).asEagerSingleton();

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(RandomEventServiceChooser.class).as("arecibo:type=HdfsEventServiceChooser");
        builder.export(HdfsEventPublisher.class).as("arecibo:name=HdfsEventPublisher");
	}
}
