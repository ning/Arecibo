package com.ning.arecibo.util;

public class EmbeddedJettyConfig
{
    private final String host;
    private final int port;
    private final int lowThreads;
    private final int minThreads;
    private final int maxThreads;
    private final int acceptQueueSize;
    private final String requestLogPath;

    public EmbeddedJettyConfig(String host,
                               int port,
                               int lowThreads,
                               int minThreads,
                               int maxThreads,
                               int acceptQueueSize,
                               String requestLogPath)
    {
        this.host = host;
        this.port = port;
        this.lowThreads = lowThreads;
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
        this.acceptQueueSize = acceptQueueSize;
        this.requestLogPath = requestLogPath;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public int getLowThreads()
    {
        return lowThreads;
    }

    public int getMinThreads()
    {
        return minThreads;
    }

    public int getMaxThreads()
    {
        return maxThreads;
    }

    public int getAcceptQueueSize()
    {
        return acceptQueueSize;
    }

    public String getRequestLogPath()
    {
        return requestLogPath;
    }
}
