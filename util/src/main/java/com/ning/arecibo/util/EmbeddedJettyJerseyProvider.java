package com.ning.arecibo.util;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.thread.QueuedThreadPool;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;

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
        threadPool.setLowThreads(config.getLowThreads());
        threadPool.setSpawnOrShrinkAt(2);

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

        Context root = new Context(server, "/", Context.SESSIONS);

        root.addFilter(GuiceFilter.class, "/*", 0);
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
