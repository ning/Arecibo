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
