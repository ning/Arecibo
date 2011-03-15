package com.ning.arecibo.util;

import org.mortbay.jetty.Server;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class EmbeddedJettyJerseyModule extends ServletModule
{
    @Override
    protected void configureServlets()
    {
        bind(EmbeddedJettyConfig.class).toProvider(EmbeddedJettyConfigProvider.class).asEagerSingleton();
        bind(Server.class).toProvider(EmbeddedJettyJerseyProvider.class).asEagerSingleton();
        serve("*").with(GuiceContainer.class);
    }
}
