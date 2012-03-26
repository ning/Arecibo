/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.util;

import com.google.common.collect.ImmutableMap;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.skife.config.ConfigurationObjectFactory;

import java.util.HashMap;
import java.util.Map;

public class EmbeddedJettyJerseyModule extends ServletModule
{
    private static final String STATIC_URL_PATTERN = "/static/*";

    private final Map<String, String> resources = new HashMap<String, String>();

    public EmbeddedJettyJerseyModule()
    {
        this(ImmutableMap.<String, String>of());
    }

    public EmbeddedJettyJerseyModule(final Map<String, String> resources)
    {
        this.resources.putAll(resources);
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

        for (final String urlPattern : resources.keySet()) {
            serveRegex(urlPattern).with(GuiceContainer.class, new HashMap<String, String>()
            {
                {
                    put(PackagesResourceConfig.PROPERTY_PACKAGES, resources.get(urlPattern));
                }
            });
        }
    }
}
