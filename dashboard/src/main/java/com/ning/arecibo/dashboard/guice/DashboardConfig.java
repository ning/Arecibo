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

package com.ning.arecibo.dashboard.guice;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;
import org.skife.config.TimeSpan;

public interface DashboardConfig
{
    @Config("arecibo.dashboard.alert_host_override")
    @DefaultNull
    String getAlertHostOverride();

    @Config("arecibo.dashboard.alert_manager_enabled")
    @Default("false")
    boolean isAlertManagerEnabled();

    @Config("arecibo.dashboard.serviceLocatorKlass")
    @Default("com.ning.arecibo.util.service.DummyServiceLocator")
    String getServiceLocatorClass();

    @Config("arecibo.dashboard.galaxy.updateInterval")
    @Default("5m")
    TimeSpan getGalaxyUpdateInterval();
}
