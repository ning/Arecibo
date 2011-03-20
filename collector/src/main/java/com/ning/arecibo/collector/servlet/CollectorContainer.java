package com.ning.arecibo.collector.servlet;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.ning.arecibo.event.receiver.RESTEventEndPoint;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.WebApplication;

@Singleton
public class CollectorContainer extends GuiceContainer
{
    private final RESTEventEndPoint endpointSingleton;

    @Inject
    public CollectorContainer(Injector injector)
    {
        super(injector);
        this.endpointSingleton = injector.getInstance(RESTEventEndPoint.class);
    }

    @Override
    public void initiate(ResourceConfig rc, WebApplication wa)
    {
        rc.getExplicitRootResources().put("singleton", endpointSingleton);
        super.initiate(rc, wa);
    }
}
