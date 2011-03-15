package com.ning.arecibo.util.service;

import com.google.inject.Inject;

public class RandomServiceChooser implements ServiceChooser
{
    private final ServiceLocator serviceLocator;
    private final Selector selector;

    @Inject
    public RandomServiceChooser(ServiceLocator serviceLocator, @RandomSelector Selector selector)
    {
        this.serviceLocator = serviceLocator;
        this.selector = selector;
    }

    @Override
    public ServiceDescriptor getResponsibleService(String index)
    {
        try {
            return serviceLocator.selectServiceAtRandom(selector);
        }
        catch (ServiceNotAvailableException e) {
            return null;
        }
    }
}
