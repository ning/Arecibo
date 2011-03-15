package com.ning.arecibo.util;

import java.util.Properties;
import com.google.inject.Provider;

public class EmbeddedJettyConfigProvider implements Provider<EmbeddedJettyConfig>
{
    private final Properties props;

    public EmbeddedJettyConfigProvider()
    {
        props = System.getProperties();
    }

    public EmbeddedJettyConfigProvider(Properties props)
    {
        this.props = props;
    }

    @Override
    public EmbeddedJettyConfig get()
    {
        String host = props.getProperty("arecibo.host", "0.0.0.0");
        int port = Integer.valueOf(props.getProperty("arecibo.jetty.port", "8088"));
        int lowThreads = Integer.valueOf(props.getProperty("arecibo.jetty.threads.low", "10"));
        int minThreads = Integer.valueOf(props.getProperty("arecibo.jetty.threads.min", "1"));
        int maxThreads = Integer.valueOf(props.getProperty("arecibo.jetty.threads.max", "200"));
        int acceptQueueSize = Integer.valueOf(props.getProperty("arecibo.jetty.accept-queue", Integer.toString(maxThreads)));
        String requestLogPath = props.getProperty("arecibo.jetty.requestLog.logDir", "logs");

        return new EmbeddedJettyConfig(host, port, lowThreads, minThreads, maxThreads, acceptQueueSize, requestLogPath);
    }
}
