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

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.confdata.objects.ConfDataPerson;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;

import com.ning.arecibo.util.Logger;



public class Person extends ConfDataPerson implements ConfigurableObject
{
    private final static Logger log = Logger.getLogger(Person.class);

    private final ConcurrentSkipListSet<NotifConfig> notifConfigs;

    public Person() {
        this.notifConfigs = new ConcurrentSkipListSet<NotifConfig>(ConfigurableObjectComparator.getInstance());
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        super.toStringBuilder(sb);

        if(notifConfigs.size() > 0) {
            sb.append("    linked notifConfig ids:\n");
            for(NotifConfig notifConfig: notifConfigs) {
                sb.append(String.format("         %s\n",notifConfig.getLabel()));
            }
        }
    }

    @Override
    public boolean isValid(ConfigManager configManager) {
        return true;
    }

    @Override
    public boolean configure(ConfigManager confManager,AlertManager alertManager, LoggingManager loggingManager) {
        return true;
    }

    @Override
    public boolean unconfigure(ConfigManager confManager,AlertManager alertManager) {

        for(NotifConfig notifConfig: notifConfigs) {
            notifConfig.unconfigure(confManager,alertManager);
        }

        return true;
    }

    @Override
    public synchronized boolean update(ConfigManager confManager,AlertManager alertManager, ConfigurableObject newConfig) {
        return ConfigurableObjectUtils.updateConfigurableObject((ConfDataObject) this, (ConfDataObject) newConfig);
    }

    
    public void addNotifConfig(NotifConfig notifConfig) {
        if(!this.notifConfigs.contains(notifConfig))
            this.notifConfigs.add(notifConfig);
    }

    public void removeNotifConfig(NotifConfig notifConfig) {
        this.notifConfigs.remove(notifConfig);
    }

    public Set<NotifConfig> getNotifConfigs() {
        return this.notifConfigs;
    }
}
