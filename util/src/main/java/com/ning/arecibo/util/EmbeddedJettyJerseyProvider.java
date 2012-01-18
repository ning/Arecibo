package com.ning.arecibo.util;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

public class EmbeddedJettyJerseyProvider implements Provider<Server>
{
    private final EmbeddedJettyConfig config;
    private final Injector injector;

    @Inject
    public EmbeddedJettyJerseyProvider(EmbeddedJettyConfig config, Injector injector)
    {
        this.config = config;
        this.injector = injector;
    }

    @Override
    public Server get()
    {
        QueuedThreadPool threadPool = new QueuedThreadPool();

        threadPool.setMinThreads(config.getMinThreads());
        threadPool.setMaxThreads(config.getMaxThreads());
        threadPool.setMinThreads(config.getLowThreads());

        SelectChannelConnector connector = new SelectChannelConnector();

        connector.setHost(config.getHost());
        connector.setPort(config.getPort());
        connector.setMaxIdleTime(30000);
        connector.setAcceptors(2);
        connector.setStatsOn(true);
        connector.setLowResourcesConnections(20000);
        connector.setLowResourceMaxIdleTime(5000);
        connector.setAcceptQueueSize(config.getAcceptQueueSize());

        Server server = new Server(config.getPort());

        server.setThreadPool(threadPool);
        server.setConnectors(new Connector[] { connector });
        server.setGracefulShutdown(1000);

        ServletContextHandler root = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        root.setResourceBase(config.getResourceBase());

        // Make sure Guice filter all requests
        final FilterHolder filterHolder = new FilterHolder(GuiceFilter.class);
        root.addFilter(filterHolder, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));

        // Backend servlet for Guice - never used
        final ServletHolder sh = new ServletHolder(DefaultServlet.class);
        root.addServlet(sh, "/*");
        root.addEventListener(new GuiceServletContextListener() {
            @Override
            protected Injector getInjector()
            {
                return injector;
            }
        });
        root.addServlet(DefaultServlet.class, "/");
        return server;
    }

}
