package com.ning.arecibo.event.publisher;

import java.io.File;
import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.TimeSpan;

public interface EventPublisherConfig
{
    @Config("arecibo.event.maxSelectorUse")
    @Default("1000")
    int getRandomSelectorMaxUse();

    @Config("arecibo.event.maxBufferSize")
    @Default("250000")
    int getMaxEventBufferSize();

    @Config("arecibo.event.maxDispatchers")
    @Default("25")
    int getMaxEventDispatchers();

    @Config("arecibo.event.maxDrainDelay")
    @Default("1s")
    TimeSpan getMaxDrainDelay();

    @Config("arecibo.event.spool.expiration")
    @Default("1h")
    TimeSpan getSpooledEventExpiration();

    @Config("arecibo.event.spool.path")
    @Default("./spool")
    File getLocalSpoolRoot();

    @Config("arecibo.event.eventServiceName")
    String getEventServiceName();
}
