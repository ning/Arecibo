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

package com.ning.arecibo.dashboard.format;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.sql.Timestamp;

public interface DashboardTableFormatter
{
    public final static String COMPOSITE_HEADER_DELIM = "$";
    public final static String EVENT_TYPE_SUBTITLE_PREFIX = " ==>";
    
    public Comparator<String> getSubTitleOrderingComparator();
    public Comparator<String> getKeyOrderingComparator(String subTitle);
    public String getSubTitleFromKey(String key,String depType,boolean showAllValues,boolean showSpecialDebugValues);
    public String getFormattedEventType(String event);
    public String getFormattedValue(String attr,HashMap<String,Object> perEventValuesMap);
    public String getTimeSinceString(Timestamp ts);
    public boolean isRelatedGlobalZoneSubTitle(String subTitle);
    public List<String> getRelatedGlobalZoneSubTitles();
    public boolean isSubtitleOkForCoreType(String subTitle,String coreType);
}
