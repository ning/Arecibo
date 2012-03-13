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

    @Config("arecibo.alert.serviceLocatorKlass")
    @Default("com.ning.arecibo.util.service.DummyServiceLocator")
    String getServiceLocatorClass();

    @Config("arecibo.alert.extraGuiceModules")
    @Default("")
    String getExtraGuiceModules();
}
