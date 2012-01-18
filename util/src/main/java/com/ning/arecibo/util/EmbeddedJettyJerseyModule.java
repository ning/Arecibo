package com.ning.arecibo.util;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.skife.config.ConfigurationObjectFactory;

import java.util.HashMap;
import java.util.List;

public class EmbeddedJettyJerseyModule extends ServletModule
{
    private static final String STATIC_URL_PATTERN = "/static/*";

    private final List<String> resources = Lists.newArrayList("com.ning.arecibo.event.receiver");

    public EmbeddedJettyJerseyModule()
    {
        this(ImmutableList.<String>of());
    }

    public EmbeddedJettyJerseyModule(final List<String> resources)
    {
        this.resources.addAll(resources);
    }

    @Override
    protected void configureServlets()
    {
        final EmbeddedJettyConfig config = new ConfigurationObjectFactory(System.getProperties()).build(EmbeddedJettyConfig.class);
        bind(EmbeddedJettyConfig.class).toInstance(config);

        bind(Server.class).toProvider(EmbeddedJettyJerseyProvider.class).asEagerSingleton();
        bind(JacksonJsonProvider.class).asEagerSingleton();

        bind(DefaultServlet.class).asEagerSingleton();
        serve(STATIC_URL_PATTERN).with(DefaultServlet.class);

        final String jerseyResources = Joiner.on(",").join(resources);
        serve("*").with(GuiceContainer.class, new HashMap<String, String>()
        {
            {
                put(PackagesResourceConfig.PROPERTY_PACKAGES, jerseyResources);
            }
        });
    }
}
