package com.ning.arecibo.dashboard.format;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.MissingResourceException;

import org.jfree.chart.axis.TickUnits;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.dashboard.format.DashboardFormatUtils.*;

public class DashboardFormatManager implements DashboardTableFormatter,DashboardGraphFormatter
{
    private final static Logger log = Logger.getLogger(DashboardFormatManager.class);
    
    private final static String DASHBOARD_PROPS_SUFFIX = "_dashboard";
    private final static String HEADER_FORMAT_RESOURCE = "header_formats" + DASHBOARD_PROPS_SUFFIX;
    private final static String HEADER_SUBTITLES_RESOURCE = "header_subtitles" + DASHBOARD_PROPS_SUFFIX;
    private final static String SUBTITLE_PROPERTIES_RESOURCE = "subtitle" + DASHBOARD_PROPS_SUFFIX;
    
    private final static String SPECIAL_HEADER_KEYS = "level,datapoints,pollingInterval,numHosts";
    private final static String SPECIAL_HEADER_MIN_KEY_PREFIX = "min_";
    private final static String SPECIAL_HEADER_MAX_KEY_PREFIX = "max_";
    
    private final static String SUBTITLE_DISPLAY_TITLE_KEY_SUFFIX = ".displayTitle";
    private final static String SUBTITLE_DISPLAY_RANK_KEY_SUFFIX = ".displayRank";
    private final static String SUBTITLE_DISPLAY_ORDER_KEY_SUFFIX = ".order";
    private final static String SUBTITLE_DISPLAY_RELATED_GLOBAL_ZONE_SUFFIX = ".relatedGlobalZoneData";
    private final static String SUBTITLE_DISPLAY_LIMIT_TO_CORE_TYPE = ".limitToCoreType";
    private final static String HEADER_SUBTITLE_LIST_DELIMITER = ",";
    
    protected static final String[] levelTags = {
            "path",
            "type",
            "host"
       };
    
    private ResourceBundle headerFormatResource = null;
    private ResourceBundle headerSubtitlesResource = null;
    private ResourceBundle subtitlePropertiesResource = null;
    
    private Map<String,Integer> subTitleOrderMap = null;
    private Map<String,Integer> headerOrderMap = null;
    private List<String> relatedGlobalZoneSubTitleList = null;
    private Map<String,String> subtitlesLimitedToCoreType = null;
    
    public DashboardFormatManager() {
    }
    
    public void init() {
        log.info("Loading header format definitions from resource '" + HEADER_FORMAT_RESOURCE + ".properties'");
        headerFormatResource = ResourceBundle.getBundle(HEADER_FORMAT_RESOURCE); 
        
        log.info("Loading header subtitle definitions from resource '" + HEADER_SUBTITLES_RESOURCE + ".properties'");
        headerSubtitlesResource = ResourceBundle.getBundle(HEADER_SUBTITLES_RESOURCE); 
        
        log.info("Loading subtitle properties definitions from resource '" + SUBTITLE_PROPERTIES_RESOURCE + ".properties'");
        subtitlePropertiesResource = ResourceBundle.getBundle(SUBTITLE_PROPERTIES_RESOURCE); 
        
        // set up ordering maps
        subTitleOrderMap = new HashMap<String,Integer>();
        headerOrderMap = new HashMap<String,Integer>();
        relatedGlobalZoneSubTitleList = new ArrayList<String>();
        subtitlesLimitedToCoreType = new HashMap<String,String>();
        
        Set<String> subtitleProperties = subtitlePropertiesResource.keySet();
        for(String subtitleProperty:subtitleProperties) {
        	if(subtitleProperty.endsWith(SUBTITLE_DISPLAY_RANK_KEY_SUFFIX)) {
        		String subtitleName = subtitleProperty.substring(0,subtitleProperty.lastIndexOf(SUBTITLE_DISPLAY_RANK_KEY_SUFFIX));
        		String rankString = subtitlePropertiesResource.getString(subtitleProperty);
        		Integer rank = Integer.parseInt(rankString);
        		
        		try {
        			String displayTitle = subtitlePropertiesResource.getString(subtitleName + SUBTITLE_DISPLAY_TITLE_KEY_SUFFIX);
        			subTitleOrderMap.put(displayTitle,rank);
        		}
        		catch(MissingResourceException mrEx) {}
        	}
        	else if(subtitleProperty.endsWith(SUBTITLE_DISPLAY_ORDER_KEY_SUFFIX)) {
        		String orderString = subtitlePropertiesResource.getString(subtitleProperty);
        		String[] orderedHeaders = orderString.split(",");
        		
        		int i=0;
        		for(String header:orderedHeaders) {
        			headerOrderMap.put(header, new Integer(i++));
        		}
        	}
        	else if(subtitleProperty.endsWith(SUBTITLE_DISPLAY_RELATED_GLOBAL_ZONE_SUFFIX)) {
        		String subtitleName = subtitleProperty.substring(0,subtitleProperty.lastIndexOf(SUBTITLE_DISPLAY_RELATED_GLOBAL_ZONE_SUFFIX));
        		try {
        			String displayTitle = subtitlePropertiesResource.getString(subtitleName + SUBTITLE_DISPLAY_TITLE_KEY_SUFFIX);
        			relatedGlobalZoneSubTitleList.add(displayTitle);
        		}
        		catch(MissingResourceException mrEx) {}
        	}
        	else if(subtitleProperty.endsWith(SUBTITLE_DISPLAY_LIMIT_TO_CORE_TYPE)) {
        		String subtitleName = subtitleProperty.substring(0,subtitleProperty.lastIndexOf(SUBTITLE_DISPLAY_LIMIT_TO_CORE_TYPE));
        		try {
        			String coreType = subtitlePropertiesResource.getString(subtitleProperty);
        			subtitlesLimitedToCoreType.put(subtitleName, coreType);
        		}
        		catch(MissingResourceException mrEx) {}
        	}
        }
    }
    
    @Override
    public String getFormattedValue(String lowerCaseAttr,HashMap<String,Object> perEventValuesMap) {
        
        String eventName = (String)perEventValuesMap.get("event");
        Object valueObj = perEventValuesMap.get(lowerCaseAttr);
        
    	String formatSpecifier = getFormatSpecifier(eventName,lowerCaseAttr);
        
        String formattedString = getFormattedString(formatSpecifier,valueObj,perEventValuesMap);
        if(formattedString != null)
            return formattedString;
        
        // default
        if(valueObj == null)
            return "null";
        else if(valueObj instanceof Number)
            return NumberRangeFormatter.formatAsOnes(valueObj);
        else
            return valueObj.toString();
        
    }
    
    @Override
    public String getFormattedValue(String lowerCaseEventName,String lowerCaseAttr,Object valueObj) {
        
    	String formatSpecifier = getFormatSpecifier(lowerCaseEventName,lowerCaseAttr);
        
        String formattedString = getFormattedString(formatSpecifier,valueObj,null);
        if(formattedString != null)
            return formattedString;
        
        // default
        if(valueObj == null)
            return "null";
        else if(valueObj instanceof Number)
            return NumberRangeFormatter.formatAsOnes(valueObj);
        else
            return valueObj.toString();
        
    }
    
    @Override
    public NumberFormat getNumberFormat(String lowerCaseEventName,String lowerCaseAttr) {
 
    	String formatSpecifier = getFormatSpecifier(lowerCaseEventName,lowerCaseAttr);
        
        NumberFormat numberFormat = getNumberFormatInstance(formatSpecifier);
        
        return numberFormat;
    }
        
    @Override
    public TickUnits getTickUnits(String lowerCaseEventName,String lowerCaseAttr) {
    	
    	String formatSpecifier = getFormatSpecifier(lowerCaseEventName,lowerCaseAttr);
        
        TickUnits tickUnitSource = getTickUnitsInstance(formatSpecifier);
        
        return tickUnitSource;
    }
    
    @Override
    public Double getAutoRangeMinimumSize(String lowerCaseEventName,String lowerCaseAttr,TickUnits tickUnits,Double minDataValue,Double maxDataValue) {
    	
    	String formatSpecifier = getFormatSpecifier(lowerCaseEventName,lowerCaseAttr);
    	
    	Double autoRangeMinimumSize = getAutoRangeMinimumSize(formatSpecifier,tickUnits,minDataValue,maxDataValue);
    	
    	return autoRangeMinimumSize;
    }
        
    private String getFormatSpecifier(String lowerCaseEventName,String lowerCaseAttr) {
    	String formatSpecifier = null;
        
        // First try event specific format
        try {
            String eventSpecificFormatKey = lowerCaseAttr + "." + lowerCaseEventName;
            formatSpecifier = headerFormatResource.getString(eventSpecificFormatKey);
        }
        catch(MissingResourceException mrEx) {}
        
        // Next try attribute only format
        if(formatSpecifier == null) {
            try {
                formatSpecifier = headerFormatResource.getString(lowerCaseAttr);
            }
            catch(MissingResourceException mrEx2) {}
        }
        
        // finally try default
        if(formatSpecifier == null) {
            try {
                formatSpecifier = headerFormatResource.getString(DEFAULT_FORMAT);
            }
            catch(MissingResourceException mrEx2) {}
        }
        
        return formatSpecifier;
    }
        
    private String getFormattedString(String formatSpecifier,Object valueObj,HashMap<String,Object> perEventValuesMap) {
        
        try {
            if(formatSpecifier != null) {
                String[] specParts = formatSpecifier.split(":");
                if(specParts[0].equals(FORMAT_TYPE_NUMBER_RANGE)) {
                    return NumberRangeFormatter.formatAsOnes(valueObj);
                }
                else if(specParts[0].equals(FORMAT_TYPE_MEMORY)) {
                    return MemoryFormatter.formatAsBytes(valueObj);
                }
                else if(specParts[0].equals(FORMAT_TYPE_MEMORY_BLOCK)) {
                    return MemoryFormatter.formatAsBlocks(valueObj);
                }
                else if(specParts[0].equals(FORMAT_TYPE_MEMORY_KB)) {
                    return MemoryFormatter.formatAsKiloBytes(valueObj);
                }
                else if(specParts[0].equals(FORMAT_TYPE_MEMORY_MB)) {
                    return MemoryFormatter.formatAsMegaBytes(valueObj);
                }
                else if(specParts[0].equals(FORMAT_TYPE_BITS_OCTETS)) {
                    return BitsFormatter.formatAsOctets(valueObj);
                }
                else if(specParts[0].equals(FORMAT_TYPE_TIME_MS)) {
                    return TimeFormatter.formatAsMilliseconds(valueObj);
                }
                else if(specParts[0].equals(FORMAT_TYPE_TIME_NS)) {
                    return TimeFormatter.formatAsNanoseconds(valueObj);
                }
                else if(specParts[0].equals(FORMAT_TYPE_TIME_SEC)) {
                    return TimeFormatter.formatAsSeconds(valueObj);
                }
                else if(specParts[0].equals(FORMAT_TYPE_TIME_HR)) {
                    return TimeFormatter.formatAsHours(valueObj);
                }
                else if(specParts[0].equals(FORMAT_TYPE_PERCENTAGE)) {
                    if(specParts.length > 1) {
                        String[] constituentAttrs = specParts[1].split(",");
                        String pctString = PercentageFormatter.formatAsPctOfTotal(valueObj,constituentAttrs,perEventValuesMap);
                        if(specParts.length > 2) {
                            String underlyingValueString = getFormattedString(specParts[2],valueObj,perEventValuesMap);
                            
                            if(pctString == null || pctString.length() == 0) {
                                return underlyingValueString;
                            }
                            else {
                                return pctString + "&nbsp;<i><span style=\"font-size:75%\">(" + underlyingValueString +")</span></i>";
                            }
                        }
                        else
                            return pctString;
                    }
                    else {
                        return PercentageFormatter.formatAsPct(valueObj);
                    }
                }
                else if(specParts[0].equals(FORMAT_TYPE_PERCENTAGE_X100)) {
                    return PercentageFormatter.formatAsPctX100(valueObj);
                }
                else if(specParts[0].equals(FORMAT_TYPE_PERCENTAGE_XNANO)) {
                    return PercentageFormatter.formatAsPctXNano(valueObj);
                }
                else if(specParts[0].equals(FORMAT_TYPE_PER_SECOND)) {
                    if(specParts.length > 1) {
                        String prefix = getFormattedString(specParts[1],valueObj,perEventValuesMap);
                        return PerSecondFormatter.addPerSecondSuffix(prefix);
                    }
                    else {
                        return PerSecondFormatter.formatAsGenericPerSecond(valueObj);
                    }
                }
            }
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx);
        } 
        
        return null;
    }
    
    private NumberFormat getNumberFormatInstance(String formatSpecifier) {
        try {
            if(formatSpecifier != null) {
                String[] specParts = formatSpecifier.split(":");
                if(specParts[0].equals(FORMAT_TYPE_NUMBER_RANGE)) {
                    return NumberRangeFormatter.getNumberFormatInstance(6);
                }
                else if(specParts[0].equals(FORMAT_TYPE_MEMORY)) {
                    return MemoryFormatter.getNumberFormatInstance(specParts[0],6);
                }
                else if(specParts[0].equals(FORMAT_TYPE_MEMORY_KB)) {
                    return MemoryFormatter.getNumberFormatInstance(specParts[0],6);
                }
                else if(specParts[0].equals(FORMAT_TYPE_MEMORY_MB)) {
                    return MemoryFormatter.getNumberFormatInstance(specParts[0],6);
                }
                else if(specParts[0].equals(FORMAT_TYPE_BITS_OCTETS)) {
                    return BitsFormatter.getNumberFormatInstance(specParts[0],6);
                }
                else if(specParts[0].equals(FORMAT_TYPE_TIME_MS) ||
                        specParts[0].equals(FORMAT_TYPE_TIME_NS) ||
                        specParts[0].equals(FORMAT_TYPE_TIME_SEC) ||
                        specParts[0].equals(FORMAT_TYPE_TIME_HR)) {
                    return TimeFormatter.getNumberFormatInstance(specParts[0]);
                }
                else if(specParts[0].equals(FORMAT_TYPE_PERCENTAGE) ||
                        specParts[0].equals(FORMAT_TYPE_PERCENTAGE_X100) ||
                        specParts[0].equals(FORMAT_TYPE_PERCENTAGE_XNANO)) {
                	if(specParts.length > 2)
                		return getNumberFormatInstance(specParts[2]);
                	else
                		return PercentageFormatter.getNumberFormatInstance(specParts[0]);
                }
                else if(specParts[0].equals(FORMAT_TYPE_PER_SECOND)) {
                	if(specParts.length > 1) {
                		if(specParts[1].equals(FORMAT_TYPE_BITS_OCTETS)) {
                			return BitsPerSecondFormatter.getNumberFormatInstance(specParts[1],6);
                		}
                		else if(specParts[1].startsWith(FORMAT_TYPE_MEMORY)) {
                			return MemoryPerSecondFormatter.getNumberFormatInstance(specParts[1],6);
                		}
                	}
                    return NumberRangePerSecondFormatter.getNumberFormatInstance(6);
                }
            }
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx);
        } 
        
        return null;
    }
    
    
    private TickUnits getTickUnitsInstance(String formatSpecifier) {
        try {
            if(formatSpecifier != null) {
                String[] specParts = formatSpecifier.split(":");
                if(specParts[0].equals(FORMAT_TYPE_NUMBER_RANGE)) {
                    return NumberRangeFormatter.getStandardTickUnits();
                }
                else if(specParts[0].equals(FORMAT_TYPE_MEMORY)) {
                    return MemoryFormatter.getStandardTickUnits(specParts[0]);
                }
                else if(specParts[0].equals(FORMAT_TYPE_MEMORY_KB)) {
                    return MemoryFormatter.getStandardTickUnits(specParts[0]);
                }
                else if(specParts[0].equals(FORMAT_TYPE_MEMORY_MB)) {
                    return MemoryFormatter.getStandardTickUnits(specParts[0]);
                }
                else if(specParts[0].equals(FORMAT_TYPE_TIME_MS) ||
                        specParts[0].equals(FORMAT_TYPE_TIME_NS) ||
                        specParts[0].equals(FORMAT_TYPE_TIME_SEC) ||
                        specParts[0].equals(FORMAT_TYPE_TIME_HR)) {
                    return TimeFormatter.getStandardTickUnits(specParts[0]);
                }
                else if(specParts[0].equals(FORMAT_TYPE_PERCENTAGE) ||
                        specParts[0].equals(FORMAT_TYPE_PERCENTAGE_X100) ||
                        specParts[0].equals(FORMAT_TYPE_PERCENTAGE_XNANO)) {
                	if(specParts.length > 2)
                		return getTickUnitsInstance(specParts[2]);
                	else
                		return null;
                }
                else if(specParts[0].equals(FORMAT_TYPE_PER_SECOND)) {
                	if(specParts.length > 1) {
                		if(specParts[1].equals(FORMAT_TYPE_BITS_OCTETS)) {
                			return BitsPerSecondFormatter.getStandardTickUnits(specParts[1]);
                		}
                		else if(specParts[1].startsWith(FORMAT_TYPE_MEMORY)) {
                			return MemoryPerSecondFormatter.getStandardTickUnits(specParts[1]);
                		}
                	}	
                    return NumberRangePerSecondFormatter.getStandardTickUnits();
                }
            }
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx);
        } 
        
        return null;
    }
    
    private Double getAutoRangeMinimumSize(String formatSpecifier,TickUnits tickUnits,Double minDataValue,Double maxDataValue) {
    	
        try {
            if(formatSpecifier != null) {
                String[] specParts = formatSpecifier.split(":");
                /*
                if(specParts[0].equals(FORMAT_TYPE_MEMORY)) {
                    return MemoryFormatter.getAutoRangeMinimumSize(specParts[0]);
                }
                else if(specParts[0].equals(FORMAT_TYPE_MEMORY_KB)) {
                    return MemoryFormatter.getAutoRangeMinimumSize(specParts[0]);
                }
                else if(specParts[0].equals(FORMAT_TYPE_MEMORY_MB)) {
                    return MemoryFormatter.getAutoRangeMinimumSize(specParts[0]);
                }
                else*/
                if(specParts[0].equals(FORMAT_TYPE_PERCENTAGE) ||
                        specParts[0].equals(FORMAT_TYPE_PERCENTAGE_X100) ||
                        specParts[0].equals(FORMAT_TYPE_PERCENTAGE_XNANO)) {
                	if(specParts.length > 2)
                		return getAutoRangeMinimumSize(specParts[2],tickUnits,minDataValue,maxDataValue);
                }
                else {
                    if(minDataValue == null || maxDataValue == null)
                        return null;

                    double dataRange = maxDataValue - minDataValue;
                    if(dataRange == 0.0) {
                        // see if any decimal places
                        if(maxDataValue % 1.0 == 0.0 ||
                                maxDataValue % 0.1 == 0.0 ||
                                maxDataValue % 0.01 == 0.0) {
                            return null; //allow default behavior
                        }
                        else if(maxDataValue % 0.001 == 0.0) {
                            return tickUnits.getCeilingTickUnit(0.001).getSize();
                        }
                        else if(maxDataValue % 0.0001 == 0.0) {
                            return tickUnits.getCeilingTickUnit(0.0001).getSize();
                        }
                        else if(maxDataValue % 0.00001 == 0.0) {
                            return tickUnits.getCeilingTickUnit(0.00001).getSize();
                        }
                        else if(maxDataValue % 0.000001 == 0.0) {
                            return tickUnits.getCeilingTickUnit(0.000001).getSize();
                        }
                        else {
                            return tickUnits.getCeilingTickUnit(0.1).getSize();
                        }
                    }
                    else
                        return tickUnits.getCeilingTickUnit(dataRange).getSize();
                }
            }
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx);
        } 
        
        return null;
    }
    
    
    @Override
    public String getFormattedEventType(String event)
    {
        // remove the trailing "_<level>";
        for(String levelTag:levelTags) {
            int cutIndex = event.indexOf("_" + levelTag);
            if(cutIndex > -1) {
                return event.substring(0,cutIndex);
            }
        }
        
        return event; 
    }

    @Override
    public String getTimeSinceString(Timestamp ts) {
        long currMillis = System.currentTimeMillis();
        long eventMillis = ts.getTime();
        long diffMillis = currMillis - eventMillis;
        
        String timeSinceString = TimeFormatter.formatAsMilliseconds((double)diffMillis);
        return timeSinceString + " ago";
    }
    
    @Override
    public String getSubTitleFromKey(String key,String depType,boolean showAllValues,boolean showDebugHeaders)
    {
        if(!showDebugHeaders &&
                (SPECIAL_HEADER_KEYS.contains(key) ||
                    key.startsWith(SPECIAL_HEADER_MIN_KEY_PREFIX) ||
                    key.startsWith(SPECIAL_HEADER_MAX_KEY_PREFIX))) {
            return null; 
        }
        
        try {
            String subTitleList = headerSubtitlesResource.getString(key.toLowerCase());
        
        	StringTokenizer st = new StringTokenizer(subTitleList,HEADER_SUBTITLE_LIST_DELIMITER);
        	while(st.hasMoreTokens()) {
            	String subTitle = st.nextToken();
            	String displaySubTitle = getAllowedDisplaySubTitle(subTitle,depType);
            
            	if(displaySubTitle != null) {
                	return displaySubTitle;
            	}
        	}
        }
        catch(MissingResourceException mrEx) {}

        if(showAllValues)
            return "";
        else
            return null;
    }
    
    private String getAllowedDisplaySubTitle(String subTitle,String depType) {
        try {
            
            // if no depType to compare, we don't care about about limiting to coreType, take the first one
            if(depType != null) {
                String limitedToCoreType = subtitlesLimitedToCoreType.get(subTitle);
            	if(limitedToCoreType != null && !limitedToCoreType.equals(depType)) {
            	    return null;
            	}
            }
            
            String displaySubTitle = subtitlePropertiesResource.getString(subTitle + SUBTITLE_DISPLAY_TITLE_KEY_SUFFIX);
            return displaySubTitle;
        }
        catch(MissingResourceException mrEx) {
            return null;
        }
    }

	private int integerCompare(Integer i1,Integer i2) {
	    if(i1 == null) {
	        if(i2 == null)
	            return 0;
            else
                return 1;
        }
        else if(i2 == null)
        	return -1;
	    
        return i1.compareTo(i2);
	}

    @Override
    public Comparator<String> getSubTitleOrderingComparator() {
        return new Comparator<String>() {
           public int compare(String s1,String s2) {
        	   
               Integer i1 = subTitleOrderMap.get(s1);
               Integer i2 = subTitleOrderMap.get(s2);
               
               int compare = integerCompare(i1,i2);
               if(compare == 0)
            	   compare = s1.compareTo(s2);
               
               return compare;
           }
        };
    }
    
    @Override
    public Comparator<String> getKeyOrderingComparator(String subTitle) {
        return new Comparator<String>() {
           public int compare(String S1,String S2) {
        	   
        	   int delim1 = S1.indexOf(COMPOSITE_HEADER_DELIM);
        	   int delim2 = S2.indexOf(COMPOSITE_HEADER_DELIM);
        	   
        	   String s1 = S1.substring(0,delim1).toLowerCase();
        	   String s2 = S2.substring(0,delim2).toLowerCase();
        	   
               Integer i1 = headerOrderMap.get(s1);
               Integer i2 = headerOrderMap.get(s2);
               
               int compare = integerCompare(i1,i2);
               if(compare == 0)
            	   compare = S1.compareTo(S2);
               
               return compare;
           }
        };
    }

    @Override
    public boolean isRelatedGlobalZoneSubTitle(String subTitle)
    {
        if(relatedGlobalZoneSubTitleList == null)
            return false;
        
        return relatedGlobalZoneSubTitleList.contains(subTitle);
    }
    
    @Override
    public List<String> getRelatedGlobalZoneSubTitles() {
    	if(relatedGlobalZoneSubTitleList == null)
    		return null;
    	
    	// make a copy, don't trust it not to get corrupted
    	ArrayList<String> retList = new ArrayList<String>();
    	retList.addAll(relatedGlobalZoneSubTitleList);
    	return retList;
    }
    
    @Override
    public boolean isSubtitleOkForCoreType(String subTitle,String coreType) {
        if(subtitlesLimitedToCoreType == null)
            return true;
        
        String limitedCoreType = subtitlesLimitedToCoreType.get(subTitle);
        if(limitedCoreType != null) {
            if(limitedCoreType.contains(coreType))
                return true;
            else
                return false;
        }
        else
            return true;
    }
}
