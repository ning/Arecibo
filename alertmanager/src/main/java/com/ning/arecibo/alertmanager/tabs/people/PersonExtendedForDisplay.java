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

package com.ning.arecibo.alertmanager.tabs.people;

import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.confdata.objects.ConfDataPerson;

public class PersonExtendedForDisplay extends ConfDataPerson {

    private volatile String extendedNotificationConfig = null;

    public PersonExtendedForDisplay() {
        super();
    }

    @Override
    public boolean filterMatchesDataObject(ConfDataObject dataObject) {
        boolean superResult = super.filterMatchesDataObject(dataObject);

        if(!superResult)
            return false;

        // compare extended fields
        PersonExtendedForDisplay extendedDataObject = (PersonExtendedForDisplay) dataObject;
        if(!checkFilterMatch(extendedDataObject.getExtendedNotificationConfig(),this.extendedNotificationConfig))
            return false;
        
        return true;
    }

    public String getExtendedNotificationConfig() {
        return extendedNotificationConfig;
    }

    public void setExtendedNotificationConfig(String extendedNotificationConfig) {
        this.extendedNotificationConfig = extendedNotificationConfig;
    }
}
