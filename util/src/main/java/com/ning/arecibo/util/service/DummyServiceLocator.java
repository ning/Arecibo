package com.ning.arecibo.util.service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;

public class DummyServiceLocator implements ServiceLocator
{
    @Override
    public void start()
    {
    }

    @Override
    public void startReadOnly()
    {
    }

    @Override
    public void stop()
    {
    }

    @Override
    public void advertiseLocalService(ServiceDescriptor sd)
    {
    }

    @Override
    public void registerListener(Selector selector,
                                 Executor executor,
                                 ServiceListener listener)
    {
    }

    @Override
    public void unregisterListener(ServiceListener listener)
    {
    }

    @Override
    public Set<ServiceDescriptor> selectServices(Selector selector)
    {
        return Collections.emptySet();
    }

    @Override
    public ServiceDescriptor selectServiceAtRandom(Selector key) throws ServiceNotAvailableException
    {
        throw new ServiceNotAvailableException();
    }
}
