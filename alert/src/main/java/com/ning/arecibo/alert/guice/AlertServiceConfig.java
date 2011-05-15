package com.ning.arecibo.alert.guice;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.TimeSpan;

public interface AlertServiceConfig
{
    @Config("arecibo.alert.config_update_interval")
    @Default("5m")
    TimeSpan getConfigUpdateInterval();

    @Config("arecibo.alert.event_handler_buffer_size")
    @Default("1024")
    int getEventHandlerBufferSize();

    @Config("arecibo.alert.event_handler_num_threads")
    @Default("25")
    int getEventHandlerNumThreads();

    @Config("arecibo.alert.smtp_host")
    @Default("smtp")
    String getSMTPHost();

    @Config("arecibo.alert.from_email_address")
    @Default("arecibo_alerts@example.com")
    String getFromEmailAddress();
}
