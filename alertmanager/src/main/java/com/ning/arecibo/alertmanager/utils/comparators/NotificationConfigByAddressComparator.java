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

package com.ning.arecibo.alertmanager.utils.comparators;

import java.util.Comparator;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;


public class NotificationConfigByAddressComparator implements Comparator<ConfDataNotifConfig> {

    static final NotificationConfigByAddressComparator instance = new NotificationConfigByAddressComparator();

    public static NotificationConfigByAddressComparator getInstance() {
        return instance;
    }

    public int compare(ConfDataNotifConfig o1, ConfDataNotifConfig o2) {
        return o1.getAddress().compareTo(o2.getAddress());
    }
}
