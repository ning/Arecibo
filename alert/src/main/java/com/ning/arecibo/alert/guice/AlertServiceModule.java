package com.ning.arecibo.alert.guice;

import java.util.UUID;
import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.AbstractModule;
import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.email.EmailManager;
import com.ning.arecibo.alert.endpoint.AlertStatusEndPoint;
import com.ning.arecibo.alert.endpoint.ConfigEventSendEndPoint;
import com.ning.arecibo.alert.endpoint.ConfigStatusEndPoint;
import com.ning.arecibo.alert.endpoint.ConfigUpdateEndPoint;
import com.ning.arecibo.alert.endpoint.JSONAlertStatusEndPoint;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;
import com.ning.arecibo.alert.manage.AsynchronousEventHandler;
import com.ning.arecibo.event.publisher.EventPublisherConfig;
import com.ning.arecibo.util.Logger;

public class AlertServiceModule extends AbstractModule
{
    final static Logger log = Logger.getLogger(AlertServiceModule.class);

    @Override
	public void configure()
	{
        ConfigurationObjectFactory configFactory = new ConfigurationObjectFactory(System.getProperties());
        EventPublisherConfig eventPublisherConfig = configFactory.build(EventPublisherConfig.class);

        bind(EventPublisherConfig.class).toInstance(eventPublisherConfig);
		bind(AsynchronousEventHandler.class).asEagerSingleton();	

        bind(ConfigManager.class).asEagerSingleton();
        bind(AlertManager.class).asEagerSingleton();
        bind(LoggingManager.class).asEagerSingleton();
        bind(EmailManager.class).asEagerSingleton();

        bind(AlertStatusEndPoint.class).asEagerSingleton();
        bind(JSONAlertStatusEndPoint.class).asEagerSingleton();
        bind(ConfigStatusEndPoint.class).asEagerSingleton();
        bind(ConfigUpdateEndPoint.class).asEagerSingleton();
        bind(ConfigEventSendEndPoint.class).asEagerSingleton();

        bind(UUID.class).annotatedWith(SelfUUID.class).toInstance(UUID.randomUUID());

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(AsynchronousEventHandler.class).as("arecibo.alert:name=AsynchronousEventHandler");
	}
}
