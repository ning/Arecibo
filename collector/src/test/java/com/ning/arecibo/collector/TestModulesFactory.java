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

package com.ning.arecibo.collector;

import com.ning.arecibo.collector.guice.CollectorRESTEventReceiverModule;
import com.ning.arecibo.event.receiver.UDPEventReceiverModule;
import com.ning.arecibo.util.EmbeddedJettyJerseyModule;
import com.ning.arecibo.util.lifecycle.LifecycleModule;
import com.ning.arecibo.util.rmi.RMIModule;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.testng.IModuleFactory;
import org.testng.ITestContext;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

public class TestModulesFactory implements IModuleFactory
{
    public static final String TEST_JETTY_HOST = "TestJettyPort";
    public static final String TEST_JETTY_PORT = "TestJettyPort";

    private static final File basePath = new File(System.getProperty("java.io.tmpdir"), "TestModulesFactory-" + System.currentTimeMillis());

    @Override
    public Module createModule(final ITestContext context, final Class<?> testClass)
    {
        // Make tmp dir
        if (!basePath.mkdir()) {
            throw new IllegalStateException("Unable to create " + basePath.toString());
        }

        // Find free port
        final int port;
        try {
            final ServerSocket socket = new ServerSocket(0);
            port = socket.getLocalPort();
            socket.close();
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }

        System.setProperty("arecibo.collector.timelines.chunksToAggregate", "2");
        System.setProperty("arecibo.collector.timelines.spoolDir", basePath.getAbsolutePath());
        System.setProperty("arecibo.jetty.port", String.valueOf(port));

        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(String.class).annotatedWith(Names.named(TEST_JETTY_HOST)).toInstance("127.0.0.1");
                bind(Integer.class).annotatedWith(Names.named(TEST_JETTY_PORT)).toInstance(port);
                install(new LifecycleModule());
                install(new EmbeddedJettyJerseyModule("(.)*/rest/.*",
                                                      ImmutableList.<String>of("com.ning.arecibo.event.receiver",
                                                                               "com.ning.arecibo.collector.resources",
                                                                               "com.ning.arecibo.util.jaxrs"
                                                      )));
                install(new UDPEventReceiverModule());
                install(new RMIModule());
                install(new CollectorTestModule());
                install(new CollectorRESTEventReceiverModule());
            }
        };
    }
}
