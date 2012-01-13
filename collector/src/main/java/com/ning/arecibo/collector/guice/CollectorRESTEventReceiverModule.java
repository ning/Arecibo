package com.ning.arecibo.collector.guice;

import com.google.inject.AbstractModule;
import com.ning.arecibo.collector.process.CollectorEventProcessor;
import com.ning.arecibo.event.receiver.RESTEventReceiverModule;

public class CollectorRESTEventReceiverModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        install(new RESTEventReceiverModule(CollectorEventProcessor.class, "arecibo.collector:name=CollectorEventProcessor"));
    }
}
