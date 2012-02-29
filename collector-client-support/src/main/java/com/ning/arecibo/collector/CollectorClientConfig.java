package com.ning.arecibo.collector;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.Description;

public interface CollectorClientConfig
{
    @Config("arecibo.collectorClient.serviceLocatorKlass")
    @Default("com.ning.arecibo.collector.discovery.DefaultCollectorFinder")
    @Description("Service finder for the collector (by default, location is determined by fixed uri)")
    String getServiceLocatorClass();

    @Config("arecibo.collectorClient.collectorUri")
    @Default("http://127.0.0.1:8080")
    @Description("Used by DefaultCollectorFinder")
    String getCollectorUri();
}
