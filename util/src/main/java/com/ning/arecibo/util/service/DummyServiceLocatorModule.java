package com.ning.arecibo.util.service;

import com.google.inject.AbstractModule;

public class DummyServiceLocatorModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(ServiceLocator.class).to(DummyServiceLocator.class);
    }
}
