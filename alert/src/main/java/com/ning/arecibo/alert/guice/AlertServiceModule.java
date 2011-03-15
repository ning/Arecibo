package com.ning.arecibo.alert.guice;

import java.util.UUID;
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
import com.ning.arecibo.event.publisher.EventServiceName;
import com.ning.arecibo.util.Logger;

public class AlertServiceModule extends AbstractModule
{
    final static Logger log = Logger.getLogger(AlertServiceModule.class);

    @Override
	public void configure()
	{
        bindConstant().annotatedWith(EventServiceName.class).to(System.getProperty("arecibo.event.eventServiceName"));
        
        bindConstant().annotatedWith(ConfigUpdateInterval.class).to(Integer.getInteger("arecibo.alert.config_update_interval", ConfigUpdateInterval.DEFAULT));
        bindConstant().annotatedWith(EventHandlerBufferSize.class).to(Integer.getInteger("arecibo.alert.event_handler_buffer_size", EventHandlerBufferSize.DEFAULT));
        bindConstant().annotatedWith(EventHandlerNumThreads.class).to(Integer.getInteger("arecibo.alert.event_handler_num_threads", EventHandlerNumThreads.DEFAULT));
		bindConstant().annotatedWith(SMTPHost.class).to(System.getProperty("arecibo.alert.smtp_host", SMTPHost.DEFAULT));
		bindConstant().annotatedWith(FromEmailAddress.class).to(System.getProperty("arecibo.alert.from_email_address", FromEmailAddress.DEFAULT));

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
