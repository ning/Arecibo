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

package com.ning.arecibo.alert.objects;

import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;

public interface ConfigurableObject
{
    public Long getId();
    public String getLabel();
    public boolean isValid(ConfigManager configManager);
    public boolean configure(ConfigManager configManager,AlertManager alertManager, LoggingManager loggingManager);
    public boolean unconfigure(ConfigManager configManager,AlertManager alertManager);
    public boolean update(ConfigManager configManager,AlertManager alertManager, ConfigurableObject updateConfig);
}
