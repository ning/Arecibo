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

package com.ning.arecibo.alertmanager.tabs.notificationgroups;

import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroup;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;

public class NotifGroupExtendedForDisplay extends ConfDataNotifGroup {

    private volatile String extendedNotificationConfigs = null;

    public NotifGroupExtendedForDisplay() {
        super();
    }

    @Override
    public boolean filterMatchesDataObject(ConfDataObject dataObject) {
        boolean superResult = super.filterMatchesDataObject(dataObject);

        if(!superResult)
            return false;

        // compare extended fields
        NotifGroupExtendedForDisplay extendedDataObject = (NotifGroupExtendedForDisplay) dataObject;
        if(!checkFilterMatch(extendedDataObject.getExtendedNotificationConfigs(),this.extendedNotificationConfigs))
            return false;

        return true;
    }

    public String getExtendedNotificationConfigs() {
        return extendedNotificationConfigs;
    }

    public void setExtendedNotificationConfigs(String extendedNotificationConfigs) {
        this.extendedNotificationConfigs = extendedNotificationConfigs;
    }
}
