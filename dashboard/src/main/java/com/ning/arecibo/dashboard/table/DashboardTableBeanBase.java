package com.ning.arecibo.dashboard.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.Timestamp;
import com.ning.arecibo.dashboard.context.DashboardTableContextManager;
import com.ning.arecibo.dashboard.format.DashboardTableFormatter;

import com.ning.arecibo.util.Logger;
import static com.ning.arecibo.dashboard.format.DashboardTableFormatter.*;

public abstract class DashboardTableBeanBase implements DashboardTableBean
{
    private final static Logger log = Logger.getLogger(DashboardTableBeanBase.class);
    
    protected final static String HIDDEN_EVENT_FIELDS = "ts,timeSince,event";
    protected final static String ORIGINAL_CASE_EVENT_SUFFIX = "_originalCase";
    
    protected NavigableMap<String,NavigableMap<String,String>> compositeDisplayTable = null;
    protected HashMap<String,HashMap<String,Object>> eventValuesMap = null;
    
    protected boolean discoverDepPath = false;
    protected boolean discoverDepType = false;
    protected String depType = null;
    protected String depPath = null;
    protected String host = null;
    protected boolean showAllDataValues = false;
    protected boolean limitToRelatedGlobalZoneData = false;
    protected boolean showSpecialDebugValues = false;
    protected DashboardTableFormatter formatter = null;
    
    protected DashboardTableContextManager contextManager = null;
    protected String tableBeanSignature = null;
    
    protected ConcurrentHashMap<String,DashboardTableBean> childTables = null;
    protected DashboardTableBean parentTable = null;
    protected String parentTableSignature = null;

    public abstract void initBean();
    public abstract String getTableTitle();
    
    public String getTableBeanSignature() {
        return this.tableBeanSignature;
    }
    
    public String getTableHost() {
        return this.host;
    }
    
    public String getTableDepPath() {
        return this.depPath;
    }
    
    public String getTableDepType() {
        return this.depType;
    }
    
    public final Set<String> getSubTitles() {
    	if(this.compositeDisplayTable != null)
    		return this.compositeDisplayTable.keySet();
    	else
    		return null;
    }
    
    public final Set<String> getCompositeHeadersBySubTitle(String subTitle) {
        if(this.compositeDisplayTable == null)
            return null;

        Map<String,String> subTable = this.compositeDisplayTable.get(subTitle);
        if(subTable != null)
            return subTable.keySet();
        else
            return null;
    }
    
    public final List<String> getEventsBySubTitle(String subTitle) {
        ArrayList<String> retList = new ArrayList<String>();
        
        Set<String> headers = getCompositeHeadersBySubTitle(subTitle);
        
        if(headers == null)
        	return retList;
        
        for(String header:headers) {
            String event = getEventFromCompositeHeader(header);
            retList.add(event);
        }
        
        return retList;
    }
    
    public final List<String> getAttributesBySubTitle(String subTitle) {
        ArrayList<String> retList = new ArrayList<String>();
        
        Set<String> headers = getCompositeHeadersBySubTitle(subTitle);
        
        if(headers == null)
        	return retList;
        
        for(String header:headers) {
            String attr = getAttrFromCompositeHeader(header);
            retList.add(attr);
        }
        
        return retList;
    }
    
    public final String getValueStringByCompositeHeader(String subTitle,String header) {
        if(this.compositeDisplayTable == null)
            return null;

        Map<String,String> subTable = this.compositeDisplayTable.get(subTitle);
        String valueString = subTable.get(header);
        
        return valueString;
    }
    
    public final String getDatapointsStringByCompositeHeader(String header) {
    	return getEventValueByCompositeHeader(header,"datapoints");
    }
    
    public final String getPollingIntervalStringByCompositeHeader(String header) {
    	return getEventValueByCompositeHeader(header,"pollingInterval");
    }
    
    public final String getTimeSinceStringByCompositeHeader(String header) {
    	return getEventValueByCompositeHeader(header,"timeSince");
    }
    
    public final String getMinValueStringByCompositeHeader(String header) {
    	String minAttr = "min_" + header;
    	return getEventValueByCompositeHeader(header,minAttr);
    }
    
    public final String getMaxValueStringByCompositeHeader(String header) {
    	String minAttr = "max_" + header;
    	return getEventValueByCompositeHeader(header,minAttr);
    }
    
    protected final String getEventValueByCompositeHeader(String header,String attr) {
    	
    	if(this.eventValuesMap == null)
    		return null;
    	
        String event = getEventFromCompositeHeader(header);
        
    	HashMap<String,Object> perEventValuesMap = this.eventValuesMap.get(event);
    	
    	if(perEventValuesMap == null)
    	    return null;
    	
    	Object valueObj = perEventValuesMap.get(attr);
    	if(valueObj != null)
    		return valueObj.toString();
    	else
    		return null;
    }
    
    protected final void buildFormattedDisplayTable(Map<String,Map<String,Object>> dbResults) {
        
	    // do 2 passes through the data in order to achieve formatting
	    // that requires interdependent values
        groupResultsByEventType(dbResults);
        formatAndArrangeTable();
    }
        
    private void groupResultsByEventType(Map<String,Map<String,Object>> dbResults) {    
        
        // first pass
        Set<String> dbKeys = dbResults.keySet();
        for(String dbKey:dbKeys) {
        	
        	Map<String,Object> subMap = dbResults.get(dbKey);
        	
        	if(subMap == null) {
        		log.warn("Found empty subMap for key = '" + dbKey + "'");
        		continue;
        	}
        	
        	/*
        	if(log.isDebugEnabled()) {
        		log.debug("keys:");
        		Set<String> keys = subMap.keySet();
        		for(String key:keys) {
        			log.debug("\t" + key);
        		}
        	}
        	*/
        	
        	Object attrObj = subMap.get("attr");
        	if(attrObj == null) {
        		log.warn("Could not find 'attr' key for submap keyed with '" + dbKey + "'");
        		continue;
        	}
        		
        	Object valueObj = subMap.get("value");
        	if(valueObj == null) {
        	    //we get null values for string valued event attributes, such as for *_level
        		continue;
        	}
        	
        	Object eventObj = subMap.get("event");
        	if(eventObj == null) {
        		log.warn("Could not find 'event' key for submap keyed with '" + dbKey + "'");
        		continue;
        	}
        	
        	if(this.depType == null && this.discoverDepType) {
        	    Object depTypeObj = subMap.get("dep_type");
        		if(depTypeObj != null) {
        		    this.depType = depTypeObj.toString();
        		}
        	}
        		
        	if(this.depPath == null && this.discoverDepPath) {
        	    Object depPathObj = subMap.get("dep_path");
        	    if(depPathObj != null) {
        		    this.depPath = depPathObj.toString();
        		}
            }
        	
        	String attr = attrObj.toString();
        	
        	String event = this.formatter.getFormattedEventType(eventObj.toString());
        	
        	// for now keep in original case
            String subTitle = this.formatter.getSubTitleFromKey(attr,depType,showAllDataValues,showSpecialDebugValues);
            
            //TODO: make this more efficient, shouldn't have to include everything here just for a few formatting opportunities
            // if no subtitle mapping, it means we won't show this attribute
            /*
             * we actually want to keep the value here, since we may need it later to do formatting
            if(subTitle == null)
            	continue;
            	*/
            
            if(limitToRelatedGlobalZoneData) {
            	if(!this.formatter.isRelatedGlobalZoneSubTitle(subTitle)) {
            		continue;
            	}
            }
            
        	
            // update cummulative eventValues map
            // so we can keep track of min/max/timestamp per event
            if(this.eventValuesMap == null)
            	this.eventValuesMap = new HashMap<String,HashMap<String,Object>>();
            
            HashMap<String,Object> perEventValuesMap = this.eventValuesMap.get(event);
            if(perEventValuesMap == null) {
            	perEventValuesMap = new HashMap<String,Object>();
            	perEventValuesMap.put("event",event);
            	
            	Object tsObj = subMap.get("ts");
            	if(tsObj != null) {
            		String timeSinceString = this.formatter.getTimeSinceString((Timestamp)tsObj);
            		perEventValuesMap.put("ts",tsObj);
            		perEventValuesMap.put("timeSince",timeSinceString);
            	}
            	
            	this.eventValuesMap.put(event,perEventValuesMap);
            }
            
            // need to keep a lower case version for matching with formatting files
            // (and protect against case changes, which has happened a few times)
            perEventValuesMap.put(attr.toLowerCase() + ORIGINAL_CASE_EVENT_SUFFIX, attr);
            perEventValuesMap.put(attr.toLowerCase(), valueObj);
        }
    }
            
    private void formatAndArrangeTable() {
        
        if(eventValuesMap == null)
            return;
        
        NavigableMap<String, NavigableMap<String,String>> displayMap = 
            new TreeMap<String, NavigableMap<String,String>>(this.formatter.getSubTitleOrderingComparator());
        
        Set<String> events = this.eventValuesMap.keySet();
        for(String event:events) {
             
            HashMap<String,Object> eventValues = this.eventValuesMap.get(event);
            Set<String> attrs = eventValues.keySet();
            
            // first build a list of attrs to work on, but do as separate iteration
            // to avoid a concurrent modification exception (the call to 'getFormattedValue' can modify the eventValues)
            ArrayList<String> attrList = new ArrayList<String>();
            for(String attr:attrs) {
            	
                // these values are here for displaying per event data, don't need for per attr data
                if(HIDDEN_EVENT_FIELDS.contains(attr)) {
                    continue;
                }

                if(attr.contains(ORIGINAL_CASE_EVENT_SUFFIX)) {
                	continue;
                }
                
                attrList.add(attr);
            }
                
            // now go through the attrList
            for(String attr:attrList) {
            	// for now use originalCase
                String subTitle = this.formatter.getSubTitleFromKey((String)eventValues.get(attr + ORIGINAL_CASE_EVENT_SUFFIX),depType,showAllDataValues,showSpecialDebugValues);
                
                // if it's null, it means we won't show this attribute
                if(subTitle == null)
                	continue;
                
                if(subTitle.length() > 0) {
                	// this belongs to a default subTitle
                	NavigableMap<String,String> subTitleMap = displayMap.get(subTitle);
                	if(subTitleMap == null) {
                    	subTitleMap = new TreeMap<String,String>(this.formatter.getKeyOrderingComparator(subTitle));
                    	displayMap.put(subTitle, subTitleMap);
                	}
                
                	String formattedValue = this.formatter.getFormattedValue(attr, eventValues);
	                
                	String attrOriginalCase = (String)eventValues.get(attr + ORIGINAL_CASE_EVENT_SUFFIX);
                	String attrKey = getAttrCompositeHeader(attrOriginalCase,event);
                
                	subTitleMap.put(attrKey,formattedValue);
                }
                
                if(showAllDataValues && !limitToRelatedGlobalZoneData) {
	                // place all  attr's in a per event subtitle (even if they also belong to a default subtitle
	                subTitle = EVENT_TYPE_SUBTITLE_PREFIX + event;
	                
	                // this belongs to a default subTitle
	                NavigableMap<String,String> subTitleMap = displayMap.get(subTitle);
	                if(subTitleMap == null) {
	                	subTitleMap = new TreeMap<String,String>(this.formatter.getKeyOrderingComparator(subTitle));
	                    displayMap.put(subTitle, subTitleMap);
	                }
	                
	                String formattedValue = this.formatter.getFormattedValue(attr, eventValues);
		                
	                String attrOriginalCase = (String)eventValues.get(attr + ORIGINAL_CASE_EVENT_SUFFIX);
	                String attrKey = getAttrCompositeHeader(attrOriginalCase,event);
	                
	                subTitleMap.put(attrKey,formattedValue);
                }
            }
        }
        
        this.compositeDisplayTable = displayMap;
    }
    
    private String getAttrCompositeHeader(String attr,String event) {
        return attr + COMPOSITE_HEADER_DELIM + event;
    }
    
    public String getAttrFromCompositeHeader(String compositeKey) {
        int delimIndex = compositeKey.indexOf(COMPOSITE_HEADER_DELIM);
        
        return compositeKey.substring(0,delimIndex);
    }
    
    public String getEventFromCompositeHeader(String compositeKey) {
        int delimIndex = compositeKey.indexOf(COMPOSITE_HEADER_DELIM);
        
        return compositeKey.substring(delimIndex+1);
    }
    
    public void joinChildTable(DashboardTableBean childTable) {
        
        if(this.childTables == null) {
            this.childTables = new ConcurrentHashMap<String,DashboardTableBean>();
        }
        String childSignature = childTable.getTableBeanSignature();
        this.childTables.put(childSignature,childTable);
        
        childTable.joinParentTable((DashboardTableBean)this);
    }
    
    public Iterable<DashboardTableBean> getJoinedChildTables() {
        if(this.childTables == null) {
            return null;
        }
        return this.childTables.values();
    }
    
    public void joinParentTable(DashboardTableBean parentTable) {
    	
    	this.parentTableSignature = parentTable.getTableBeanSignature();
    	this.parentTable = parentTable;
    }
    
    public DashboardTableBean getJoinedParentTable() {
    	return this.parentTable;
    }
}
