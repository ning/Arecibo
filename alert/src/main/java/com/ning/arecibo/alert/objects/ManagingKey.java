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
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.sql.Timestamp;

import com.ning.arecibo.util.Logger;


import org.joda.time.LocalTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.confdata.enums.ManagingKeyActionType;
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKey;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.logging.LoggingManager;
import com.ning.arecibo.alert.manage.AlertManager;

public class ManagingKey extends ConfDataManagingKey implements ConfigurableObject
{
    private final static Logger log = Logger.getLogger(ManagingKey.class);

    private final ConcurrentSkipListSet<ManagingKeyMapping> managingKeyMappings;

    public ManagingKey() {
        this.managingKeyMappings = new ConcurrentSkipListSet<ManagingKeyMapping>(ConfigurableObjectComparator.getInstance());
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        super.toStringBuilder(sb);

        if (managingKeyMappings.size() > 0) {
            sb.append("    linked managingKeyMapping ids:\n");
            for (ManagingKeyMapping managingKeyMapping : managingKeyMappings) {
                sb.append(String.format("        %s\n", managingKeyMapping.getLabel()));
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
        
        for (ManagingKeyMapping managingKeyMapping : managingKeyMappings) {
            managingKeyMapping.unconfigure(confManager, alertManager);
        }

        return true;
    }

    @Override
    public synchronized boolean update(ConfigManager confManager, AlertManager alertManager, ConfigurableObject newConfig) {
        return ConfigurableObjectUtils.updateConfigurableObject((ConfDataObject)this,(ConfDataObject)newConfig);
    }


    
    public void addManagingKeyMapping(ManagingKeyMapping managingKeyMapping) {
        if (!this.managingKeyMappings.contains(managingKeyMapping))
            this.managingKeyMappings.add(managingKeyMapping);
    }

    public void removeManagingKeyMapping(ManagingKeyMapping managingKeyMapping) {
        this.managingKeyMappings.remove(managingKeyMapping);
    }

    public Set<ManagingKeyMapping> getManagingKeyMappings() {
        return this.managingKeyMappings;
    }

    private boolean testActivation() {
        return testActivation(System.currentTimeMillis());
    }

    private synchronized boolean testActivation(long testMillis) {

        // see if we have an overriding 'activatedIndefinitely' flag
        if(this.getActivatedIndefinitely() != null && this.getActivatedIndefinitely()) {
            return true;
        }

        // see if we have an overriding 'activatedUntilTs'
        if(this.getActivatedUntilTs() != null && this.getActivatedUntilTs().after(new Timestamp(testMillis))) {
            return true;
        }

        // check scheduled time of day
        boolean todValid = false;
        if(this.getAutoActivateTODStartMs() != null && this.getAutoActivateTODEndMs() != null) {

            LocalTime lt = new LocalTime(testMillis, DateTimeZone.forOffsetHours(0));
            int millisOfDay = lt.getMillisOfDay();

            if(this.getAutoActivateTODStartMs() <= this.getAutoActivateTODEndMs()) {
                if(millisOfDay < this.getAutoActivateTODStartMs() || millisOfDay > this.getAutoActivateTODEndMs()) {
                    return false;
                }
            }
            else if(millisOfDay < this.getAutoActivateTODStartMs() && millisOfDay > this.getAutoActivateTODEndMs()) {
                return false;
            }
            
            todValid = true;
        }

        // check scheduled day of week
        boolean dowValid = false;
        if(this.getAutoActivateDOWStart() != null && this.getAutoActivateDOWEnd() != null) {

            LocalDate ld = new LocalDate(testMillis, DateTimeZone.forOffsetHours(0));
            int dayOfWeek = ld.getDayOfWeek();

            if(this.getAutoActivateDOWStart() <= this.getAutoActivateDOWEnd()) {
                if(dayOfWeek < this.getAutoActivateDOWStart() || dayOfWeek > this.getAutoActivateDOWEnd()) {
                    return false;
                }
            }
            else if(dayOfWeek < this.getAutoActivateDOWStart() && dayOfWeek > this.getAutoActivateDOWEnd()) {
                return false;
            }

            dowValid = true;
        }

        // if we get here, all good
        if(todValid || dowValid)
            return true;

        return false;
    }

    public static ManagingKeyActionType getAction(List<ManagingKey> managingKeyList) {

        // init default
        ManagingKeyActionType retAction = ManagingKeyActionType.NO_ACTION;

        for(ManagingKey managingKey:managingKeyList) {

            synchronized(managingKey) {
                if(managingKey.testActivation()) {
                    if(managingKey.getAction().getLevel() > retAction.getLevel()) {
                        retAction = managingKey.getAction();
                    }
                }
            }

        }

        return retAction;
    }
}
