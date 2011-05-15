package com.ning.arecibo.util;

import org.mortbay.jetty.Server;
import org.skife.config.ConfigurationObjectFactory;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class EmbeddedJettyJerseyModule extends ServletModule
{
    @Override
    protected void configureServlets()
    {
        EmbeddedJettyConfig config = new ConfigurationObjectFactory(System.getProperties()).build(EmbeddedJettyConfig.class);

        bind(EmbeddedJettyConfig.class).toInstance(config);
        bind(Server.class).toProvider(EmbeddedJettyJerseyProvider.class).asEagerSingleton();
        serve("*").with(GuiceContainer.class);
    }
}
