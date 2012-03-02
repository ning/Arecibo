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

package com.ning.arecibo.dashboard.alert;

import java.util.Comparator;
import static com.ning.arecibo.dashboard.alert.AlertStatusManager.*;

public class AlertStatusComparator implements Comparator<DashboardAlertStatus>
{
    private final static AlertStatusComparator instance = new AlertStatusComparator();
    
    public static AlertStatusComparator getInstance() {
        return instance;
    }
    
    public int comparePossiblyNullStrings(String s1,String s2) {
        if(s1 == null) {
            if(s2 == null)
                return 0;
            else
                return -1;
        }
        else if(s2 == null)
            return 1;
        else
            return s1.compareTo(s2);
    }
    
    public int compare(DashboardAlertStatus as1, DashboardAlertStatus as2) {
        
        int comparison;
        if((comparison = as1.getThresholdConfigId().compareTo(as2.getThresholdConfigId())) != 0)
            return comparison;
        
        if((comparison = comparePossiblyNullStrings(as1.getAttribute(HOST_ATTR),as2.getAttribute(HOST_ATTR))) != 0)
            return comparison;
        
        if((comparison = comparePossiblyNullStrings(as1.getAttribute(TYPE_ATTR),as2.getAttribute(TYPE_ATTR))) != 0)
            return comparison;
        
        if((comparison = comparePossiblyNullStrings(as1.getAttribute(PATH_ATTR),as2.getAttribute(PATH_ATTR))) != 0)
            return comparison;
        
        // finally resort to the internal unique alertId
        return(as1.getAlertId().compareTo(as2.getAlertId()));
    }
}
