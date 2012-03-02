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
import com.ning.arecibo.alert.confdata.objects.ConfDataAcknowledgementLog;


public class AcknowledgementLogByAckTimeComparator implements Comparator<ConfDataAcknowledgementLog> {

    static final AcknowledgementLogByAckTimeComparator instance = new AcknowledgementLogByAckTimeComparator();

    public static AcknowledgementLogByAckTimeComparator getInstance() {
        return instance;
    }

    public int compare(ConfDataAcknowledgementLog o1, ConfDataAcknowledgementLog o2) {
        return o1.getAckTime().compareTo(o2.getAckTime());
    }
}
