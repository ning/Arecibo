package com.ning.arecibo.util;

import com.google.inject.servlet.ServletModule;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.eclipse.jetty.server.Server;
import org.skife.config.ConfigurationObjectFactory;

import java.util.HashMap;

public class EmbeddedJettyJerseyModule extends ServletModule
{
    @Override
    protected void configureServlets()
    {
        EmbeddedJettyConfig config = new ConfigurationObjectFactory(System.getProperties()).build(EmbeddedJettyConfig.class);

        bind(EmbeddedJettyConfig.class).toInstance(config);
        bind(Server.class).toProvider(EmbeddedJettyJerseyProvider.class).asEagerSingleton();
        bind(JacksonJsonProvider.class).asEagerSingleton();

        serve("*").with(GuiceContainer.class, new HashMap<String, String>()
        {
            {
                put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.ning.arecibo.event.receiver");
            }
        });
    }
}
