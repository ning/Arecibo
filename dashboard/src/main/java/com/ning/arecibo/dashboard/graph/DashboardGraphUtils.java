package com.ning.arecibo.dashboard.graph;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class DashboardGraphUtils
{
    public final static int DEFAULT_SPARKLINE_WIDTH = 80;
    public final static int DEFAULT_SPARKLINE_HEIGHT = 30;
    public final static int DEFAULT_SPARKLINE_REFERENCE_RESOLUTION_WIDTH = 40;
    
    public final static int DEFAULT_GRAPH_WIDTH = 500;
    public final static int DEFAULT_GRAPH_HEIGHT = 300;
    public final static int DEFAULT_GRAPH_REFERENCE_RESOLUTION_WIDTH = 250;
    
    public final static int DEFAULT_PER_LEGEND_HEIGHT = 18;
    public final static int DEFAULT_MAX_TIME_SERIES_PER_GRAPH = 32;
    
    public final static long LONG_ONE_SECOND = 1000L;
    public final static long LONG_ONE_MINUTE = 60L * LONG_ONE_SECOND;
    public final static long LONG_TEN_MINUTES = 10L * LONG_ONE_MINUTE;
    public final static long LONG_THIRTY_MINUTES = 30L * LONG_ONE_MINUTE;
    public final static long LONG_ONE_HOUR = 60L * LONG_ONE_MINUTE;
    public final static long LONG_TWO_HOURS = 2L * LONG_ONE_HOUR;
    public final static long LONG_SIX_HOURS = 6L * LONG_ONE_HOUR;
    public final static long LONG_TWELVE_HOURS = 12L * LONG_ONE_HOUR;
    public final static long LONG_ONE_DAY = 24L * LONG_ONE_HOUR;
    public final static long LONG_TWO_DAYS = 2 * LONG_ONE_DAY;
    
    public enum TimeWindow {
        //ONE_MINUTE(LONG_ONE_MINUTE,"1 Minute"),
    	TEN_MINUTES(LONG_TEN_MINUTES,"10 Minutes"),
    	THIRTY_MINUTES(LONG_THIRTY_MINUTES,"30 Minutes"),
    	ONE_HOUR(LONG_ONE_HOUR,"1 Hour"),
    	TWO_HOURS(LONG_TWO_HOURS,"2 Hours"),
    	SIX_HOURS(LONG_SIX_HOURS,"6 Hours"),
    	TWELVE_HOURS(LONG_TWELVE_HOURS,"12 Hours"),
    	ONE_DAY(LONG_ONE_DAY,"1 Day"),
    	TWO_DAYS(LONG_TWO_DAYS,"2 Days");
        
        private long millis;
        private String displayName;
        
        TimeWindow(long millis,String displayName) {
            this.millis = millis;
            this.displayName = displayName;
        }
        
        public long getMillis() {
            return this.millis;
        }
        
        public String getDisplayName() {
            return this.displayName;
        }
        
        public static TimeWindow getDefaultTimeWindow() {
            return TimeWindow.ONE_HOUR;
        }
    }
    
    public enum GraphType {
        BY_HOST,
        BY_PATH_WITH_TYPE,
        BY_TYPE,
        BY_GLOBAL_ZONE_LIST_FOR_PATH_WITH_TYPE,
        BY_GLOBAL_ZONE_LIST_FOR_TYPE
    }
    
    public enum GraphMultipleSeriesType {
    	OVER_HOSTS,
    	OVER_TYPES,
    	OVER_PATHS_WITH_TYPE,
    	OVER_PATH_WITH_TYPES,
    	OVER_PATHS_WITH_TYPES
    }
    
    public enum GraphGroupType {
        BY_PARAM_LIST, //TODO: do we still need this?
        BY_TABLE_BEAN_SUBTITLE
    }
    
    public static Duration parseDuration(String durationString,Long defaultMillis) {
    	
        if(durationString != null) {
        	// try as millis
        	try {
        		long durationMillis = Long.parseLong(durationString);
        		return new Duration(durationMillis);
        	}
        	catch(NumberFormatException numEx) {}
        
        	// try as ISO 8601 Duration
        	try {
				return new Duration(durationString);
			}
			catch(IllegalArgumentException iaEx) {}
        }
		
		// use default
        if(defaultMillis != null)
        	return new Duration(defaultMillis);
        
        return null;
    }
    
    public static Long parseDurationMillis(String durationString,Long defaultMillis) {
    	Duration duration = parseDuration(durationString,defaultMillis);
    	
    	if(duration == null)
    		return null;
    	else
    		return duration.getMillis();
    }
    
    public static DateTime parseDateTime(String dateTimeString,Long defaultMillis) {
    	
        if(dateTimeString != null) {
        	// try as millis
        	try {
        		long dateTimeMillis = Long.parseLong(dateTimeString);
        		return new DateTime(dateTimeMillis);
        	}
        	catch(NumberFormatException numEx) {}
        
        	// try as ISO 8601 DateTime
        	try {
				return new DateTime(dateTimeString);
			}
			catch(IllegalArgumentException iaEx) {}
        }
		
		// use default
        if(defaultMillis != null)
        	return new DateTime(defaultMillis);
        
        return null;
    }
    
    public static Long parseDateTimeMillis(String dateTimeString,Long defaultMillis) {
    	DateTime dateTime = parseDateTime(dateTimeString,defaultMillis);
    	
    	if(dateTime == null)
    		return null;
    	else
    		return dateTime.getMillis();
    }
}
