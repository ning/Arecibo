package com.ning.arecibo.util;

import org.skife.config.Config;
import org.skife.config.Default;

public abstract class EmbeddedJettyConfig
{
    @Config("arecibo.host")
    @Default("0.0.0.0")
    public abstract String getHost();

    @Config("arecibo.jetty.port")
    @Default("8088")
    public abstract int getPort();

    @Config("arecibo.jetty.threads.low")
    @Default("10")
    public abstract int getLowThreads();

    @Config("arecibo.jetty.threads.min")
    @Default("1")
    public abstract int getMinThreads();

    @Config("arecibo.jetty.threads.max")
    @Default("200")
    public abstract int getMaxThreads();

    @Config("arecibo.jetty.accept-queue")
    @Default("200")
    public abstract int getAcceptQueueSize();

    @Config("arecibo.jetty.requestLog.logDir")
    @Default("logs")
    public abstract String getRequestLogPath();

    @Config("arecibo.jetty.resourceBase")
    @Default("src/main/webapp")
    public abstract String getResourceBase();
}
