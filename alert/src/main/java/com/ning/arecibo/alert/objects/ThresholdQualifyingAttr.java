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
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdQualifyingAttr;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;
import com.ning.arecibo.util.Logger;

public class ThresholdQualifyingAttr extends ConfDataThresholdQualifyingAttr implements ConfigurableObject
{
    private static final Logger log = Logger.getLogger(ThresholdQualifyingAttr.class);

    private volatile ThresholdConfig thresholdConfig = null;

    public ThresholdQualifyingAttr()
    {
    }

    @Override
    public synchronized boolean isValid(ConfigManager confManager)
    {

        // make sure our thresholdAlertConfig exists in the conf, and is valid
        if (!ConfigurableObjectUtils.checkNonNullAndValid(confManager.getThresholdConfig(this.thresholdConfigId), confManager)) {
            return false;
        }

        return true;
    }

    @Override
    public synchronized boolean configure(ConfigManager confManager, AlertManager alertManager, LoggingManager loggingManager)
    {

        this.thresholdConfig = confManager.getThresholdConfig(this.thresholdConfigId);
        if (!ConfigurableObjectUtils.checkNonNullAndLog(this.thresholdConfig, this.thresholdConfigId, "thresholdConfig", confManager)) {
            return false;
        }

        this.thresholdConfig.addThresholdQualifyingAttr(this);
        return true;
    }

    @Override
    public synchronized boolean unconfigure(ConfigManager confManager, AlertManager alertManager)
    {

        if (this.thresholdConfig != null) {
            this.thresholdConfig.removeThresholdQualifyingAttr(this);
            this.thresholdConfig = null;
        }

        return true;
    }

    @Override
    public synchronized boolean update(ConfigManager confManager, AlertManager alertManager, ConfigurableObject newConfig)
    {
        return ConfigurableObjectUtils.updateConfigurableObject((ConfDataObject) this, (ConfDataObject) newConfig);
    }
}
