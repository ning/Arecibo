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
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKeyMapping;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;
import com.ning.arecibo.util.Logger;

public class ManagingKeyMapping extends ConfDataManagingKeyMapping implements ConfigurableObject
{
    private static final Logger log = Logger.getLogger(ManagingKeyMapping.class);

    private volatile AlertingConfig alertingConfig = null;
    private volatile ManagingKey managingKey = null;

    public ManagingKeyMapping()
    {
    }

    @Override
    public void toStringBuilder(StringBuilder sb)
    {
        super.toStringBuilder(sb);
    }

    @Override
    public synchronized boolean isValid(ConfigManager confManager)
    {

        // make sure our alertingConfig exists in the conf, and is valid
        if (!ConfigurableObjectUtils.checkNonNullAndValid(confManager.getAlertingConfig(this.alertingConfigId), confManager)) {
            return false;
        }

        // make sure our managingKey exists in the conf, and is valid
        if (!ConfigurableObjectUtils.checkNonNullAndValid(confManager.getManagingKey(this.managingKeyId), confManager)) {
            return false;
        }

        return true;
    }

    @Override
    public synchronized boolean configure(ConfigManager confManager, AlertManager alertManager, LoggingManager loggingManager)
    {

        this.alertingConfig = confManager.getAlertingConfig(this.alertingConfigId);
        if (!ConfigurableObjectUtils.checkNonNullAndLog(this.alertingConfig, this.alertingConfigId, "alertConfig", confManager)) {
            return false;
        }

        this.managingKey = confManager.getManagingKey(this.managingKeyId);
        if (!ConfigurableObjectUtils.checkNonNullAndLog(this.managingKey, this.managingKeyId, "managingKey", confManager)) {
            return false;
        }

        this.alertingConfig.addManagingKeyMapping(this);
        this.managingKey.addManagingKeyMapping(this);

        return true;
    }

    @Override
    public synchronized boolean unconfigure(ConfigManager confManager, AlertManager alertManager)
    {

        if (this.alertingConfig != null) {
            this.alertingConfig.removeManagingKeyMapping(this);
            this.alertingConfig = null;
        }

        if (this.managingKey != null) {
            this.managingKey.removeManagingKeyMapping(this);
            this.managingKey = null;
        }

        return true;
    }

    @Override
    public synchronized boolean update(ConfigManager confManager, AlertManager alertManager, ConfigurableObject newConfig)
    {
        return ConfigurableObjectUtils.updateConfigurableObject((ConfDataObject) this, (ConfDataObject) newConfig);
    }
}
