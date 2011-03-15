package com.ning.arecibo.event.receiver;

import java.util.List;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ning.arecibo.event.transport.EventSerializer;
import com.ning.arecibo.event.transport.JavaEventSerializer;
import com.ning.arecibo.event.transport.JsonEventSerializer;
import com.ning.arecibo.event.transport.MapEventSerializer;
import com.ning.arecibo.util.ArrayListProvider;

public class RESTEventReceiverModule extends AbstractModule
{
    public static final String ENDPOINT_PREFIX = "/1.0";
    public static final Named CONTAINER_ANNOTATION = Names.named(ENDPOINT_PREFIX);

    private final Class<? extends BaseEventProcessor> clazz;
	private final String jmxObjectName;

	public RESTEventReceiverModule(Class<? extends BaseEventProcessor> clazz, String jmxObjectName)
	{
		this.clazz = clazz;
		this.jmxObjectName = jmxObjectName;
	}

	@Override
	public void configure()
	{
		bind(clazz).asEagerSingleton();
		bind(BaseEventProcessor.class).to(clazz).asEagerSingleton();

		bind(new TypeLiteral<List<EventSerializer>>(){}).annotatedWith(EventSerializers.class)
		    .toProvider(new ArrayListProvider<EventSerializer>()
		                    .add(JavaEventSerializer.class)
		                    .add(JsonEventSerializer.class)
		                    .add(MapEventSerializer.class));

		bind(EventParser.class).asEagerSingleton();
		bind(RESTEventEndPoint.class).asEagerSingleton();

		ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(BaseEventProcessor.class).as(jmxObjectName);
	}
}
