package com.ning.arecibo.collector.guice;

import com.google.inject.AbstractModule;
import com.ning.arecibo.collector.process.CollectorEventProcessor;
import com.ning.arecibo.event.receiver.RESTEventReceiverModule;
import com.ning.arecibo.event.transport.EventSerializer;
import com.ning.arecibo.util.Logger;
import org.skife.config.ConfigurationObjectFactory;

import java.util.ArrayList;
import java.util.List;

public class CollectorRESTEventReceiverModule extends AbstractModule
{
    private static final Logger log = Logger.getLogger(CollectorRESTEventReceiverModule.class);

    private final List<Class<? extends EventSerializer>> serializers = new ArrayList<Class<? extends EventSerializer>>();

    @Override
    protected void configure()
    {
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);

        for (final String eventSerializer : config.getEventSerializers().split(",")) {
            try {
                serializers.add(Class.forName(eventSerializer).asSubclass(EventSerializer.class));
            }
            catch (ClassNotFoundException e) {
                log.warn("Unable to find class " + eventSerializer, e);
            }
            catch (ClassCastException e) {
                log.warn("Unable to cast class " + eventSerializer, e);
            }
        }

        install(new RESTEventReceiverModule(CollectorEventProcessor.class, "arecibo.collector:name=CollectorEventProcessor", serializers));
    }
}
