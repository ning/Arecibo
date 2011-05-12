package com.ning.arecibo.event.publisher;

import java.util.concurrent.ExecutorService;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.ning.arecibo.event.publisher.cluster.RandomSelectorMaxUse;
import com.ning.arecibo.eventlogger.EventPublisher;
import com.ning.arecibo.util.FailsafeScheduledExecutor;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.NamedThreadFactory;
import com.ning.arecibo.util.service.RandomSelector;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceSelector;

public class HdfsEventPublisherModule implements Module
{
    final static Logger log = Logger.getLogger(HdfsEventPublisherModule.class);

	private final String senderType;
	private final String serviceName;

	public HdfsEventPublisherModule(String senderType, String serviceName)
	{
		this.senderType = senderType;
		this.serviceName = serviceName;
	}

	public void configure(Binder binder)
	{
		binder.bind(ExecutorService.class).annotatedWith(PublisherExecutor.class).toInstance(
			new FailsafeScheduledExecutor(50, new NamedThreadFactory("EventPublisher"))
		);

        binder.bind(Selector.class)
				.annotatedWith(RandomSelector.class)
				.toInstance(new ServiceSelector(serviceName));

        binder.bindConstant().annotatedWith(RandomSelectorMaxUse.class).to(Integer.getInteger("arecibo.event.maxSelectorUse", RandomSelectorMaxUse.DEFAULT));
        binder.bindConstant().annotatedWith(MaxEventBufferSize.class).to(Integer.getInteger("arecibo.event.maxBufferSize", MaxEventBufferSize.DEFAULT));
        binder.bindConstant().annotatedWith(MaxEventDispatchers.class).to(Integer.getInteger("arecibo.event.maxDispatchers", MaxEventDispatchers.DEFAULT));
        binder.bindConstant().annotatedWith(MaxDrainDelayInMS.class).to(Long.getLong("arecibo.event.maxDrainDelayInMS", MaxDrainDelayInMS.DEFAULT));
        binder.bindConstant().annotatedWith(SpooledEventExpirationInMS.class).to(Long.getLong("arecibo.event.spool.expirationInMS", SpooledEventExpirationInMS.DEFAULT));
        binder.bindConstant().annotatedWith(LocalSpoolRoot.class).to(System.getProperty("arecibo.event.spool.path", LocalSpoolRoot.DEFAULT));

        binder.bind(EventPublisher.class).to(HdfsEventPublisher.class).asEagerSingleton();
        binder.bind(String.class).annotatedWith(EventSenderType.class).toInstance(senderType);
        binder.bind(EventServiceChooser.class).to(RandomEventServiceChooser.class).asEagerSingleton();
        binder.bind(HdfsEventPublisher.class).asEagerSingleton();

        ExportBuilder builder = MBeanModule.newExporter(binder);

        builder.export(RandomEventServiceChooser.class).as("arecibo:type=HdfsEventServiceChooser");
        builder.export(HdfsEventPublisher.class).as("arecibo:name=HdfsEventPublisher");
	}
}
