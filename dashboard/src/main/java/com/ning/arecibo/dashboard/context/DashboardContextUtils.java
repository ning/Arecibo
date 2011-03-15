package com.ning.arecibo.dashboard.context;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
//import java.text.DateFormat;

import com.ning.arecibo.util.Logger;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.ning.arecibo.dashboard.alert.AlertStatusComparator;
import com.ning.arecibo.dashboard.alert.AlertStatusManager;
import com.ning.arecibo.dashboard.alert.DashboardAlertStatus;
import com.ning.arecibo.dashboard.dao.ResolutionRequestType;
import com.ning.arecibo.dashboard.format.TimeFormatter;
import com.ning.arecibo.dashboard.graph.DashboardGraphUtils;
import com.ning.arecibo.dashboard.graph.DashboardGraphUtils.GraphGroupType;
import com.ning.arecibo.dashboard.table.DashboardTableBean;
import com.ning.arecibo.dashboard.table.DashboardTableBeanFactory;

import static com.ning.arecibo.dashboard.alert.AlertStatusManager.*;
import static com.ning.arecibo.dashboard.format.DashboardTableFormatter.*;
import static com.ning.arecibo.dashboard.graph.DashboardGraphUtils.*;

public class DashboardContextUtils
{
    private final static Logger log = Logger.getLogger(DashboardContextUtils.class);
    
    public final static String UNDEFINED_HOST_NAME = "undefined";
    public final static String UNDEFINED_TYPE_NAME = "undefined";
    public final static String UNDEFINED_PATH_NAME = "undefined";
    public final static String UNDEFINED_GROUPING_NAME = "undefined";
    
    public final static String UNSPECIFIED_PATH_NAME = "unspecified";
    
    public final static String GLOBAL_ZONE_TYPE = "arecibo-agent";
    public final static String GLOBAL_ZONE_PATH = UNSPECIFIED_PATH_NAME;

    public final static String TABLE_TITLE_H1_START = "<div style=\"font-size:125%\">";
    public final static String TABLE_TITLE_H1_END = "</div>";
    public final static String TABLE_TITLE_LR = "<br/>";
    public final static String TABLE_TITLE_H2_START = "<div style=\"font-size:75%\">"; 
    public final static String TABLE_TITLE_H2_END = "</div>"; 
    
    public final static String HTML_INDENT = "&nbsp;&nbsp;&nbsp;";
        
    public final static String DASHBOARD_RETURN_KEY_PREFIX = "return_";    
    public final static String DASHBOARD_SPARKLINE_SERVLET_PATH = "/DashboardSparkline";
    public final static String DASHBOARD_GRAPH_SERVLET_PATH = "/DashboardGraph";
    public final static String DASHBOARD_GRAPH_LEGEND_SERVLET_PATH = "/DashboardGraphLegend";
    public final static String DASHBOARD_DATA_SERVLET_PATH = "/DashboardData";
    
    public final static String DEFAULT_HOST_DOMAIN = "";
    public final static String HOST_CHECKBOX_VALID_PREFIX = "host_valid$";
    public final static String PATH_CHECKBOX_VALID_PREFIX = "path_valid$";
    public final static String GROUPING_CHECKBOX_VALID_PREFIX = "grouping_valid$";
    
    public final static String SHOW_ALL_DATA_VALUES = "show_all_values";
    public final static String PREV_SHOW_ALL_DATA_VALUES_FLAG = "prev_show_all_values";
    
    public final static String SHOW_GLOBAL_ZONE_VALUES = "show_global_zone_values";
    public final static String PREV_SHOW_GLOBAL_ZONE_VALUES_FLAG = "prev_show_global_zone_values";
    
    public final static String SHOW_HOSTS_WITH_AGGREGATES = "show_hosts_with_aggregates";
    public final static String SHOW_HOSTS_WITH_AGGREGATES_ON_UPDATE_BUTTON = "show_hosts_with_aggregates_on_update_button";
    
    public final static String UPDATE_BUTTON = "update_button";
    public final static String ALERTS_BUTTON = "alert_button";
        
    public final static String RELATED_SUBTITLE_SEPARATOR = "--related--";
    public final static String DEFAULT_SUBTITLE_SEPARATOR = "-----------";
    
    public final static String DEBUG_MODE_PARAM = "debug_mode";
    public final static String DEBUG_STATUS_TYPES = "arecibo-agent,arecibo-aggregator";
    
    public final static int MAX_ALERTS_PER_POPUP = 2;
    
    // perhaps this should be a context injected param
    public final static String DEBUG_STATUS_PORT = "8080"; 
    
    public final static String UTF_8 = "UTF-8";

    private final static String EVENT_TYPE_STR = "eventType";
    private final static String KEY_STR = "key";
    private final static String ALERT_STR = "alert";
    private final static String SUBTITLE_STR = "subTitle";

    private final static DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("MMM d, yyyy");
    private final static DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("HH:mm");
    private final static DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("MMM d, yyyy HH:mm");

    public static String getTableTitleTypeLink(DashboardTableContextManager contextManager,String type,String ... msg) {
        String linkMessage;
        if(msg.length > 0)
            linkMessage = msg[0];
        else
            linkMessage = type.toUpperCase();

		return "<a style=\"color:yellow\" href=\"../byGrouping?new_grouping=" + type +
		                                                                getAdditionalHostParams(contextManager) +
		                                                                getAdditionalOptionsParams(contextManager) +
		                                                                getShowHostsWithAggregatesOption() +
		                                                                "\">" + linkMessage + "</a>";
    }

    public static String getTableTitlePathWithTypeLink(DashboardTableContextManager contextManager,String path,String type,String ... msg) {
        String linkMessage;
        if(msg.length > 0)
            linkMessage = msg[0];
        else
            linkMessage = path.toUpperCase();

        return "<a style=\"color:yellow\" href=\"../byGrouping?new_path=" + path + 
                                                                        "&new_grouping=" + type + 
                                                                        getAdditionalHostParams(contextManager) +
		                                                                getAdditionalOptionsParams(contextManager) +
		                                                                getShowHostsWithAggregatesOption() +
                                                                        "\">" + linkMessage + "</a>";
    }
    
    public static String getTableTitleAdHocHostLink(DashboardTableContextManager contextManager,String host,String ... msg) {
        String linkMessage;
        if(msg.length > 0)
            linkMessage = msg[0];
        else
            linkMessage = host;

        return "<a style=\"color:yellow\" href=\"../byGrouping" + getAdditionalParamsWithNewHost(contextManager,host,true) +
		                                                                getAdditionalOptionsParams(contextManager) +
                                                                        "\">" + linkMessage + "</a>";
    }
    
    public static String getTableSubTitleAdHocHostLink(DashboardTableContextManager contextManager,String host,String ... msg) {
    	
    	if(host == null)
    		return "";
    	
        String linkMessage;
        if(msg.length > 0)
            linkMessage = msg[0];
        else
            linkMessage = host;

        return ": <a style=\"color:blue\" href=\"../byGrouping" + getAdditionalParamsWithNewHost(contextManager,host,true) +
		                                                                getAdditionalOptionsParams(contextManager) +
                                                                        "\">" + linkMessage + "</a>";
    }
    
    public static String getTableTitleDebugStatusHostLink(DashboardTableContextManager contextManager, String depType, String host)
    {
        Map<String,String[]> contextParams = contextManager.getParameterMap();
        
        if ( contextParams.containsKey(DEBUG_MODE_PARAM) && depType != null && DEBUG_STATUS_TYPES.contains(depType.toLowerCase()) ) {
            return String.format("<a style=\"color:yellow\" href=\"http://%s:" + DEBUG_STATUS_PORT + "/%s\"> %s </a>", 
                                 host, depType.toLowerCase().contains("agent") ? "status" : "", host) ;
        }
        else {
            return host ;
        }
    }    
    
    private static String getAdditionalHostParams(DashboardTableContextManager contextManager) {
        
        Map<String,String[]> contextParams = contextManager.getParameterMap();
        
        if(contextParams == null)
            return "";
        
        StringBuffer contextAddParams = new StringBuffer();
        Set<String> contextKeys = contextParams.keySet();
        
        if(contextKeys != null) {
        	for(String contextKey:contextKeys) {
            	if(contextKey.contains("host")) {
	                
                	String[] values = contextParams.get(contextKey);
                	for(String value:values) {
                    	try {
                        	contextAddParams.append("&" + contextKey + "=" + URLEncoder.encode(value,UTF_8));
                    	}
                    	catch(UnsupportedEncodingException ueEx){}
                	}
            	}
        	}
        }
        
        return contextAddParams.toString();
    }
    
    private static String getAdditionalParamsWithNewHost(DashboardTableContextManager contextManager,String newFqdnHost,boolean isStartingQueryString) {
        
        Map<String,String[]> contextParams = contextManager.getParameterMap();
        
        if(contextParams == null)
            return "";
        
        StringBuffer contextAddParams = new StringBuffer();
        Set<String> contextKeys = contextParams.keySet();
        
        if(contextKeys != null) {
        	for(String contextKey:contextKeys) {
                
            	String[] values = contextParams.get(contextKey);
            	for(String value:values) {
                
                	try {
                    	contextAddParams.append("&" + contextKey + "=" + URLEncoder.encode(value,UTF_8));
                	}
                	catch(UnsupportedEncodingException ueEx){}
            	}
            }
        }
        
        if(newFqdnHost != null) {
            try {
        	    contextAddParams.append("&host=" + URLEncoder.encode(newFqdnHost,UTF_8));
        	    contextAddParams.append("&" + HOST_CHECKBOX_VALID_PREFIX + newFqdnHost + "=true");
            }
            catch(UnsupportedEncodingException ueEx){}
        }
        
        if(isStartingQueryString)
            contextAddParams.setCharAt(0,'?');
        return contextAddParams.toString();
    }
    
    private static String getAdditionalOptionsParams(DashboardTableContextManager contextManager) {
        
        StringBuffer contextAddParams = new StringBuffer();
        
        if(contextManager.getShowAllDataValues()) {
            contextAddParams.append("&" + SHOW_ALL_DATA_VALUES + "=on");
        }
        
        if(contextManager.getShowGlobalZoneValues()) {
            contextAddParams.append("&" + SHOW_GLOBAL_ZONE_VALUES + "=on");
        }
        
        if(contextManager.getShowDebugMode()) {
            contextAddParams.append("&" + DEBUG_MODE_PARAM + "=on");
        }
        
        return contextAddParams.toString();
    }
    
    private static String getShowHostsWithAggregatesOption() {
        return "&" + SHOW_HOSTS_WITH_AGGREGATES + "=on";
    }	                                                                
		                                                           
    public static String getRelativeGraphPageUrl(DashboardContextManager contextManager,String graphType,
                                         String type,String path,String host,String eventType,String key) {
        
        String graphGroupType = GraphGroupType.BY_PARAM_LIST.toString();
        
        String contextPath = contextManager.getContextPath();
        Map<String,String[]> contextParams = (Map<String,String[]>) contextManager.getParameterMap();

        // must have a graphType
        if(graphType == null || graphType.length() == 0)
            return null;

        StringBuilder sb = new StringBuilder();
        sb.append(contextPath);
        sb.append("/graphs");

        try {
            sb.append("?graphType=" + graphType);
            sb.append("&graphGroupType=" + graphGroupType);

            if(host != null) {
                sb.append("&host=" + URLEncoder.encode(host,UTF_8));
            }

            if(path != null) {
                sb.append("&path=" + URLEncoder.encode(path,UTF_8));
            }

            if(type != null) {
                sb.append("&type=" + URLEncoder.encode(type,UTF_8));
            }

            if(eventType != null) {
                sb.append("&eventType=" + URLEncoder.encode(eventType,UTF_8));
            }

            if(key != null) {
                sb.append("&key=" + URLEncoder.encode(key,UTF_8));
            }

            Set<String> returnParamKeys = contextParams.keySet();
            
            if(returnParamKeys != null) {
            	for(String returnParamKey:returnParamKeys) {

                	String[] values = contextParams.get(returnParamKey);
	
                	if(values == null)
                    	continue;
                
                	for(String value:values) {
                    	String renamedKey = DASHBOARD_RETURN_KEY_PREFIX + returnParamKey;
                    	sb.append("&" + renamedKey + "=" + URLEncoder.encode(value,UTF_8));
                	}
                }
            }
        }
        catch (UnsupportedEncodingException ueEx) {}

        return sb.toString();
    }
    
    public static String getRelativeGraphPageUrl(DashboardContextManager contextManager,String graphType,
                                         String type,String path,String host,
                                         String title,String subTitle,
                                         List<String> eventTypes,List<String> keys,List<String> alertFlags) {
        
        String graphGroupType = GraphGroupType.BY_PARAM_LIST.toString();
        
        String contextPath = contextManager.getContextPath();
        Map<String,String[]> contextParams = (Map<String,String[]>) contextManager.getParameterMap();

        // must have a graphType
        if(graphType == null || graphType.length() == 0)
            return null;

        StringBuilder sb = new StringBuilder();
        sb.append(contextPath);
        sb.append("/graphs");

        try {
            sb.append("?graphType=" + graphType);
            sb.append("&graphGroupType=" + graphGroupType);

            if(host != null) {
                sb.append("&host=" + URLEncoder.encode(host,UTF_8));
            }

            if(path != null) {
                sb.append("&path=" + URLEncoder.encode(path,UTF_8));
            }

            if(type != null) {
                sb.append("&type=" + URLEncoder.encode(type,UTF_8));
            }
            
            if(title != null) {
            	sb.append("&title=" + URLEncoder.encode(title,UTF_8));
            }

            if(subTitle != null) {
            	sb.append("&subTitle=" + URLEncoder.encode(subTitle,UTF_8));
            }

            int count = 0;
            if(eventTypes != null) {
            	for(String eventType:eventTypes) {
                	if(eventType != null) {
                		sb.append("&eventType" + count++ + "=" + URLEncoder.encode(eventType,UTF_8));
            		}
            	}
            }

            count = 0;
            if(keys != null) {
            	for(String key:keys) {
                	if(key != null) {
                		sb.append("&key" + count++ + "=" + URLEncoder.encode(key,UTF_8));
                	}
            	}
            }

            count = 0;
            if(alertFlags != null) {
            	for(String alertFlag:alertFlags) {
                	if(alertFlag != null) {
                		sb.append("&alert" + count++ + "=" + URLEncoder.encode(alertFlag,UTF_8));
            		}
            	}
            }

            Set<String> returnParamKeys = contextParams.keySet();
            if(returnParamKeys != null) {
            	for(String returnParamKey:returnParamKeys) {

                	String[] values = contextParams.get(returnParamKey);

                	if(values == null)
                    	continue;
                
                	for(String value:values) {
                    	String renamedKey = DASHBOARD_RETURN_KEY_PREFIX + returnParamKey;
                    	sb.append("&" + renamedKey + "=" + URLEncoder.encode(value,UTF_8));
                	}
            	}
            }
        }
        catch (UnsupportedEncodingException ueEx) {}

        return sb.toString();
    }
    
    public static String getRelativeGraphPageUrl(DashboardContextManager contextManager,String tableBeanSignature,String subTitle) {
        
        String graphGroupType = GraphGroupType.BY_TABLE_BEAN_SUBTITLE.toString();
        
        String contextPath = contextManager.getContextPath();
        boolean showDebugMode = contextManager.getShowDebugMode();
        boolean showAllDataValues = contextManager.getShowAllDataValues();
        boolean showGlobalZoneValues = contextManager.getShowGlobalZoneValues();

        Map<String,String[]> contextParams = (Map<String,String[]>) contextManager.getParameterMap();

        StringBuilder sb = new StringBuilder();
        sb.append(contextPath);
        sb.append("/graphs");

        try {
            sb.append("?graphGroupType=" + graphGroupType);
            sb.append("&tableSignature=" + URLEncoder.encode(tableBeanSignature,UTF_8));
            
            if(subTitle != null)
            	sb.append("&subTitle=" + URLEncoder.encode(subTitle,UTF_8));

            if(showDebugMode)
                sb.append("&" + DEBUG_MODE_PARAM + "=on");

            if(showAllDataValues)
                sb.append("&" + SHOW_ALL_DATA_VALUES + "=on");

            if(showGlobalZoneValues)
                sb.append("&" + SHOW_GLOBAL_ZONE_VALUES + "=on");

            Set<String> returnParamKeys = contextParams.keySet();
            if(returnParamKeys != null) {
            	for(String returnParamKey:returnParamKeys) {

                	String[] values = contextParams.get(returnParamKey);

                	if(values == null)
                    	continue;
                
                	for(String value:values) {
                    	String renamedKey = DASHBOARD_RETURN_KEY_PREFIX + returnParamKey;
                    	sb.append("&" + renamedKey + "=" + URLEncoder.encode(value,UTF_8));
                	}
            	}
            }
        }
        catch (UnsupportedEncodingException ueEx) {}

        return sb.toString();
    }

    public static String getRelativeGraphPageByHostUrl(DashboardContextManager contextManager,String host,String subTitle,
                                                       Boolean showAllDataValues,Boolean showGlobalZoneValues,Boolean showDebugMode) {
        DashboardTableBeanFactory tableBeanFactory = contextManager.getTableBeanFactory();
        String tableSignature = tableBeanFactory.getTableBeanSignatureKey(DashboardTableBeanFactory.DASHBOARD_TABLE_BEAN_BY_HOST,host,showAllDataValues,false,showDebugMode);
        return getRelativeGraphPageUrl(contextManager,tableSignature,subTitle);
    }
    
    public static String getRelativeDashboardPageUrl(DashboardContextManager contextManager) {
        String contextPath = contextManager.getContextPath();
        Map<String,String[]> params = (Map<String,String[]>) contextManager.getParameterMap();

        StringBuilder sb = new StringBuilder();
        sb.append(contextPath);
        sb.append("/byGrouping");

        try {
            int paramCount = 0;

            Set<String> paramKeys = params.keySet();

            int startIndex = DASHBOARD_RETURN_KEY_PREFIX.length();

            if(paramKeys != null) {
	            for(String paramKey:paramKeys) {
	
	                if(!paramKey.startsWith(DASHBOARD_RETURN_KEY_PREFIX))
	                    continue;
	
	                String[] values = params.get(paramKey);
	                
	                if(values == null)
	                    continue;
	
	                for(String value:values) {
	                    String renamedKey = paramKey.substring(startIndex);
	
	                	sb.append((paramCount++ > 0)?"&":"?");
	                	sb.append(renamedKey + "=" + URLEncoder.encode(value,UTF_8));
	                }
	            }
            }
        }
        catch (UnsupportedEncodingException ueEx) {}

        return sb.toString();
    }    
    
    public static String getRelativeDashboardPageByHostUrl(DashboardContextManager contextManager,String host) {
    	int domainIndex = host.indexOf('.');
    	log.debug("host = " + host);
    	log.debug("    domainIndex = " + domainIndex);
    	if(domainIndex == -1) {
    		return getRelativeDashboardPageByHostUrl(contextManager, host, "local");
    	}
    	else {
    		if(domainIndex >= 0 && domainIndex < host.length() - 1) {
    			// break it up into the host name + host domain
    			return getRelativeDashboardPageByHostUrl(contextManager,host.substring(0,domainIndex),host.substring(domainIndex+1));
    		}
    		else {
    			// this shouldn't really happen, unless host ends with a '.'
    			return getRelativeDashboardPageByHostUrl(contextManager,host.substring(0,domainIndex));
    		}
    	}
    }
    
    public static String getRelativeDashboardPageByHostUrl(DashboardContextManager contextManager,String host,String hostDomain) {
    	String contextPath = getDashboardPageUri(contextManager) + "?new_host=" + host + "&new_host_domain=" + hostDomain;
    	return contextPath;
    }
    
    public static String getDashboardPageUri(DashboardContextManager contextManager) {
        String contextPath = contextManager.getContextPath();
        return contextPath + "/byGrouping";
    }
    
    public static String getAlertsPageUri(DashboardContextManager contextManager) {
        String contextPath = contextManager.getContextPath();
        return contextPath + "/alerts";
    }
    
    public static List<String> getGraphServletUrls(DashboardContextManager contextManager,Map<String,String> graphParams) {
        ArrayList<String> retList = new ArrayList<String>();
        
        // create subGraphParams, without any of the indexed fields, and other extraneous fields
        HashMap<String,String> subGraphParams = new HashMap<String,String>();
        Set<String> graphParamKeys = graphParams.keySet();
        
        if(graphParamKeys != null) {
	        for(String graphParamKey:graphParamKeys) {
	            
	            if(graphParamKey.startsWith(EVENT_TYPE_STR) ||
	                    graphParamKey.startsWith(KEY_STR) ||
	                    graphParamKey.startsWith(ALERT_STR) ||
	                    graphParamKey.startsWith(SUBTITLE_STR) ||
	                    graphParamKey.equals("title")) {
	                
	                continue;
	            }
	            
	            subGraphParams.put(graphParamKey, graphParams.get(graphParamKey));
	        }
        }
        
        int count=0;
        while(graphParams != null && graphParams.containsKey(EVENT_TYPE_STR + count)) {
            subGraphParams.put(EVENT_TYPE_STR,graphParams.get(EVENT_TYPE_STR + count));
            subGraphParams.put(KEY_STR,graphParams.get(KEY_STR + count));
            subGraphParams.put(ALERT_STR,graphParams.get(ALERT_STR + count));
            
            String graphUrl = getServletUrl(contextManager,DASHBOARD_GRAPH_SERVLET_PATH,subGraphParams);
            retList.add(graphUrl);
            count++;
        }
        
        return retList;
    }

    public static String getGraphServletUrl(DashboardContextManager contextManager,Map<String,String> graphParams) {

        return getServletUrl(contextManager,DASHBOARD_GRAPH_SERVLET_PATH,graphParams);
    }

    public static String getSparklineServletUrl(DashboardContextManager contextManager,Map<String,String> graphParams) {

        return getServletUrl(contextManager,DASHBOARD_SPARKLINE_SERVLET_PATH,graphParams);
    }

    public static String getDataServletUrl(DashboardContextManager contextManager,Map<String,String> graphParams) {

        return getServletUrl(contextManager, DASHBOARD_DATA_SERVLET_PATH,graphParams);
    }

    public static String getLegendServletUrl(DashboardContextManager contextManager,Map<String,String> graphParams) {

        return getServletUrl(contextManager,DASHBOARD_GRAPH_LEGEND_SERVLET_PATH,graphParams);
    }

    public static String getServletUrl(DashboardContextManager contextManager,String baseURI,Map<String,String> params) {
        String contextPath = contextManager.getContextPath();
        StringBuilder servletUrl = new StringBuilder(contextPath + baseURI);

        appendParamsToUri(servletUrl,params);

        return servletUrl.toString();
    }

    public static void appendParamsToUri(StringBuilder sb,Map<String,String> params) {
        boolean firstParamAdded = false;
        Set<String> mapKeys = params.keySet();

        if (mapKeys != null) {
            for (String mapKey : mapKeys) {

                if (!firstParamAdded) {
                    sb.append("?");
                    firstParamAdded = true;
                }
                else
                    sb.append("&");

                try {
                    String value = URLEncoder.encode(params.get(mapKey), UTF_8);
                    sb.append(mapKey + "=" + value);
                }
                catch (UnsupportedEncodingException ueEx) {
                }
            }
        }
    }
    
    public static boolean isAlertStatusAvailable(DashboardContextManager contextManager) {
        AlertStatusManager alertStatusManager = contextManager.getAlertStatusManager();
        return alertStatusManager.isAlertStatusAvailable();
    }
    
    public static int getActiveAlertCount(DashboardContextManager contextManager) {
        AlertStatusManager alertStatusManager = contextManager.getAlertStatusManager();
        return alertStatusManager.getNumMetricsInAlert();
    }
    
    public static List<DashboardAlertStatus> getActiveAlertList(DashboardContextManager contextManager) {
        
        AlertStatusManager alertStatusManager = contextManager.getAlertStatusManager();
        List<DashboardAlertStatus> alertStatii = alertStatusManager.getMetricsInAlert();
        
        if(alertStatii == null)
            return new ArrayList<DashboardAlertStatus>();
        
        Collections.sort(alertStatii,AlertStatusComparator.getInstance());
        return alertStatii;
    }
    
    public static String getHtmlAlertStatusSummaryString(DashboardContextManager contextManager) {
        
        AlertStatusManager alertStatusManager = contextManager.getAlertStatusManager();
        List<DashboardAlertStatus> alertStatii = alertStatusManager.getMetricsInAlert();
        
        Collections.sort(alertStatii,AlertStatusComparator.getInstance());
        
        StringBuilder sb = new StringBuilder();
        
        formatAlertListSummary(alertStatii,sb);
        
        return sb.toString();
    }
    
    public static List<String> getAlertFlags(DashboardContextManager contextManager,List<String> events,List<String> attrs,String type,String path,String host) {
        
        ArrayList<String> retList = new ArrayList<String>();
        
        AlertStatusManager alertStatusManager = contextManager.getAlertStatusManager();
        
        Iterator<String> eventIter = events.iterator();
        Iterator<String> attrIter = attrs.iterator();
        
        while(eventIter.hasNext() && attrIter.hasNext()) {
            List<DashboardAlertStatus> alertStatii = alertStatusManager.getMetricsInAlert(eventIter.next(),
                                                                                 attrIter.next(),
                                                                                 type,
                                                                                 path,
                                                                                 host);
            
            if(alertStatii == null)
                retList.add("");
            else
                retList.add("true");
        }
        
        return retList;
    }
    
    public static boolean hasNonEmptyStringsInList(List<String> list) {
    	if(list == null)
    		return false;
    	
        for(String string:list) {
            if(string.length() > 0)
                return true;
        }
        
        return false;
    }
 
    public static List<String> getPopupAlertStatusStrings(DashboardContextManager contextManager,Map<String,String> graphParams) {
        ArrayList<String> retList = new ArrayList<String>();
        
        int count=0;
        while(graphParams.containsKey(EVENT_TYPE_STR + count)) {
            
            String alertParam = graphParams.get(ALERT_STR + count);
            if(alertParam == null || alertParam.length() == 0){
                retList.add("");
            }
            else {
                String popupString = getPopupAlertStatusString(contextManager,
                                                           graphParams.get(EVENT_TYPE_STR + count),
                                                           graphParams.get(KEY_STR + count),
                                                           graphParams.get("type"),
                                                           graphParams.get("path"),
                                                           graphParams.get("host"));
            
                retList.add(popupString);
            }
            
            count++;
        }
        
        return retList;
    }
    
    public static List<String> getDataServletUrlsFromGraphUrls(DashboardContextManager contextManager,Map<String,String> graphParams,List<String> graphUrls) {
        ArrayList<String> retList = new ArrayList<String>();
        
        int count=0;
        while(graphParams.containsKey(EVENT_TYPE_STR + count)) {
            
            String dataUrl = graphUrls.get(count).replace(DASHBOARD_GRAPH_SERVLET_PATH, DASHBOARD_DATA_SERVLET_PATH);
            retList.add(dataUrl);
            
            count++;
        }
        
        return retList;
    }
    
    public static List<String> getLegendServletUrlsFromGraphUrls(DashboardContextManager contextManager,Map<String,String> graphParams,List<String> graphUrls) {
        ArrayList<String> retList = new ArrayList<String>();

        String subTitle = graphParams.get("subTitle");
        String showAllDataValues = graphParams.get(SHOW_ALL_DATA_VALUES);
        String showGlobalZoneValues = graphParams.get(SHOW_GLOBAL_ZONE_VALUES);
        String showDebugMode = graphParams.get(DEBUG_MODE_PARAM);
        
        int count=0;
        while(graphParams.containsKey(EVENT_TYPE_STR + count)) {
            
            StringBuilder legendUrl = new StringBuilder(graphUrls.get(count).replace(DASHBOARD_GRAPH_SERVLET_PATH, DASHBOARD_GRAPH_LEGEND_SERVLET_PATH));

            if(subTitle != null) {
                try {
                    legendUrl.append("&subTitle=" + URLEncoder.encode(subTitle,UTF_8));
                } catch(UnsupportedEncodingException ueEx){}
            }
            if(showAllDataValues != null)
                legendUrl.append("&" + SHOW_ALL_DATA_VALUES + "=" + showAllDataValues);
            if(showGlobalZoneValues != null)
                legendUrl.append("&" + SHOW_GLOBAL_ZONE_VALUES + "=" + showGlobalZoneValues);
            if(showDebugMode != null)
                legendUrl.append("&" + DEBUG_MODE_PARAM + "=" + showDebugMode);

            retList.add(legendUrl.toString());
            
            count++;
        }
        
        return retList;
    }
    
    public static List<String> getSubTitles(DashboardContextManager contextManager,Map<String,String> graphParams) {
        ArrayList<String> retList = new ArrayList<String>();
        
        int count=0;
        while(graphParams.containsKey(SUBTITLE_STR + count)) {
            
            String subTitleParam = graphParams.get(SUBTITLE_STR + count);
            if(subTitleParam == null || subTitleParam.length() == 0){
                retList.add("");
            }
            else {
                retList.add(subTitleParam);
            }
            
            count++;
        }
        
        return retList;
    }
    
    public static String getPopupAlertStatusString(DashboardContextManager contextManager,Map<String,String> graphParams) {
        
        String alertParam;
        if((alertParam = graphParams.get("alert")) == null || alertParam.length() == 0)
            return "";
        
        return getPopupAlertStatusString(contextManager,graphParams.get("eventType"),
                                                         graphParams.get("key"),
                                                         graphParams.get("type"),
                                                         graphParams.get("path"),
                                                         graphParams.get("host"));
    }
    
    private static String getAlertStatusString(DashboardContextManager contextManager,String eventType,String attribute,String type,String path, String host) {
        
        
        AlertStatusManager alertStatusManager = contextManager.getAlertStatusManager();
        
        List<DashboardAlertStatus> alertStatii = alertStatusManager.getMetricsInAlert(eventType,
                                                                             attribute,
                                                                             type,
                                                                             path,
                                                                             host);
        
        if(alertStatii == null)
            return "";
        
        Collections.sort(alertStatii,AlertStatusComparator.getInstance());
        
        StringBuilder sb = new StringBuilder();
        
        int numAlerts = alertStatii.size();
        if(numAlerts > 1) {
            sb.append("<b>There are " + numAlerts + " active alerts for this metric</b><br>");
        }
        
        if(numAlerts > MAX_ALERTS_PER_POPUP) {
            formatAlertListSummary(alertStatii,sb);
        }
        else {
            formatAlertList(alertStatii,sb,eventType,attribute);
        }
        
        return sb.toString();
    }
    
    public static String getPopupAlertStatusString(DashboardContextManager contextManager,String eventType,String attribute,String type,String path, String host) {
        
        String alertStatusString = getAlertStatusString(contextManager,eventType,attribute,type,path,host);
        
        return getPopupString(alertStatusString);
    }
    
    
    public static String getPopupString(String html) {
        return "onmouseover=\"Tip('" + html + "'" + ",BGCOLOR,'#FFDAB9')\" onmouseout=\"UnTip()\"";
    }
    
    public static String getPopupString(String html,int width,int height) {
        return "onmouseover=\"Tip('" + html + "'" + ",BGCOLOR,'#FFDAB9',WIDTH," + width + ",HEIGHT," + height + ")\" onmouseout=\"UnTip()\"";
    }
    
    private static void formatAlertListSummary(Iterable<DashboardAlertStatus> alertStatii,StringBuilder sb) {
        String currRangeConfigId = "";
        int currRangeConfigIdCount = -1;
        DashboardAlertStatus lastAlertStatusInRange = null;
        
        if(alertStatii != null) {
	        for(DashboardAlertStatus alertStatus:alertStatii) {
	            String currConfigId = alertStatus.getThresholdConfigId();
	            
	            if(currConfigId.equals(currRangeConfigId)) {
	                currRangeConfigIdCount++;
	            }
	            else {
	                if(lastAlertStatusInRange != null) {
	                    String instancesString = (currRangeConfigIdCount == 1)?"instance":"instances";
	                    sb.append("<br>");
	                    sb.append("<b>Alert " + lastAlertStatusInRange.getThresholdConfigId() + ": " + currRangeConfigIdCount + " " + instancesString + "</b><br>");
	                    sb.append(HTML_INDENT + "<b>" + lastAlertStatusInRange.getEventType() + "</b> -> <b>" + lastAlertStatusInRange.getAttributeType() + "</b><br>");
	                    sb.append(HTML_INDENT + "<b>description</b>:" + HTML_INDENT + lastAlertStatusInRange.getShortDescription() + "<br>");
	                }
	                currRangeConfigIdCount = 1;
	                currRangeConfigId = currConfigId;
	            }
	            lastAlertStatusInRange = alertStatus;
	        }
        }
        
        // finish up last range
        if(lastAlertStatusInRange != null) {
            String instancesString = (currRangeConfigIdCount == 1)?"instance":"instances";
            sb.append("<br>");
            sb.append("<b>Alert " + lastAlertStatusInRange.getThresholdConfigId() + ": " + currRangeConfigIdCount + " " + instancesString + "</b><br>");
            sb.append(HTML_INDENT + "<b>" + lastAlertStatusInRange.getEventType() + "</b> -> <b>" + lastAlertStatusInRange.getAttributeType() + "</b><br>");
            sb.append(HTML_INDENT + "<b>description</b>:" + HTML_INDENT + lastAlertStatusInRange.getShortDescription() + "<br>");
        }
        sb.append("<br>");
    }
    
    private static void formatAlertListTableSummary(Iterable<DashboardAlertStatus> alertStatii,StringBuilder sb) {
        
        String currRangeConfigId = "";
        int currRangeConfigIdCount = 0;
        DashboardAlertStatus lastAlertStatusInRange = null;
        
        if(alertStatii != null) {
	        for(DashboardAlertStatus alertStatus:alertStatii) {
	            String currConfigId = alertStatus.getThresholdConfigId();
	            if(currConfigId.equals(currRangeConfigId)) {
	                currRangeConfigIdCount++;
	            }
	            else {
	                if(lastAlertStatusInRange != null) {
	                    String instancesString = (currRangeConfigIdCount == 1)?"instance":"instances";
	                    sb.append("<br>");
	                    sb.append("<b>Alert " + lastAlertStatusInRange.getThresholdConfigId() + ": " + currRangeConfigIdCount + " " + instancesString + "</b><br>");
	                    sb.append(HTML_INDENT + "<b>description</b>:" + HTML_INDENT + lastAlertStatusInRange.getShortDescription() + "<br>");
	                }
	                currRangeConfigIdCount = 1;
	                currRangeConfigId = currConfigId;
	            }
	            lastAlertStatusInRange = alertStatus;
	        }
        }
        
        // finish up last range
        if(lastAlertStatusInRange != null) {
            String instancesString = (currRangeConfigIdCount == 1)?"instance":"instances";
            sb.append("<br>");
            sb.append("<b>Alert " + lastAlertStatusInRange.getThresholdConfigId() + ": " + currRangeConfigIdCount + " " + instancesString + "</b><br>");
            sb.append(HTML_INDENT + "<b>description</b>:" + HTML_INDENT + lastAlertStatusInRange.getShortDescription() + "<br>");
        }
    } 
    
    private static void formatAlertList(Iterable<DashboardAlertStatus> alertStatii,StringBuilder sb) {
        formatAlertList(alertStatii,sb,null,null);
    }
    
    private static void formatAlertList(Iterable<DashboardAlertStatus> alertStatii,StringBuilder sb,String eventType,String attributeType) {
        
        boolean discoverEventType = (eventType == null)?true:false;
        boolean discoverAttributeType = (attributeType == null)?true:false;
        
        if(alertStatii != null) {
	        for(DashboardAlertStatus alertStatus:alertStatii) {
	            
	            if(discoverEventType)
	                eventType = alertStatus.getEventType();
	            if(discoverAttributeType)
	                attributeType = alertStatus.getAttributeType();
	            
	            String deployedConfigSubPath = alertStatus.getAttribute(PATH_ATTR);
	            String deployedType = alertStatus.getAttribute(TYPE_ATTR);
	            String hostName = alertStatus.getAttribute(HOST_ATTR);
	            String attributeValue = alertStatus.getAttribute(attributeType);
	            
	            String timeInAlert = null;
	            try {
	                long timeInAlertMillis = Long.parseLong(alertStatus.getTimeInAlert());
	                timeInAlert = TimeFormatter.formatAsMilliseconds(timeInAlertMillis);
	            }
	            catch(NumberFormatException numEx) {
	                log.warn("Got number format exception parsing timeInAlert",numEx);
	                log.info(numEx);
	            }
	        
	            sb.append("<br>");
	            sb.append("<b>Alert " + alertStatus.getThresholdConfigId() + ":</b><br>");
	            sb.append(HTML_INDENT + "<b>description</b>:" + HTML_INDENT + alertStatus.getShortDescription() + "<br>");
	            
	            if(attributeValue != null)
	                sb.append(HTML_INDENT + "<b>currentValue</b>:" + HTML_INDENT + attributeValue + "<br>");
	            
	            sb.append(HTML_INDENT + "<b>escalationStatus</b>:" + HTML_INDENT + alertStatus.getActivationStatus() + "<br>");
	            
	            if(timeInAlert != null)
	                sb.append(HTML_INDENT + "<b>timeInAlert</b>:" + HTML_INDENT + timeInAlert + "<br>");
	        
	            sb.append("<br>");
	            sb.append(HTML_INDENT + "<b>eventType</b>:" + HTML_INDENT + eventType + "<br>");
	            sb.append(HTML_INDENT + "<b>attributeType</b>:" + HTML_INDENT + attributeType + "<br>");
	            if(hostName != null)
	                sb.append(HTML_INDENT + "<b>hostName</b>:" + HTML_INDENT + hostName + "<br>");
	            if(deployedType != null)
	                sb.append(HTML_INDENT + "<b>deployedType</b>:" + HTML_INDENT + deployedType + "<br>");
	            if(deployedConfigSubPath != null)
	                sb.append(HTML_INDENT + "<b>deployedScope</b>:" + HTML_INDENT + deployedConfigSubPath + "<br>");
	            sb.append("<br>");
	        }
        }
    }
    
    public static Map<String,String> getMultipleGraphParams(DashboardContextManager contextManager) {
        
        String graphGroupType = contextManager.getPrimaryParamValue("graphGroupType");
        
        if(graphGroupType != null && graphGroupType.equals(GraphGroupType.BY_TABLE_BEAN_SUBTITLE.toString())) {
            return getMultipleGraphParamsByTableBeanSubTitle(contextManager);
        }
        else {
            return getMultipleGraphParamsByParamList(contextManager);
        }
    }
    
    private static Map<String,String> getMultipleGraphParamsByTableBeanSubTitle(DashboardContextManager contextManager) {
        
        DashboardTableContextManager tableContextManager = (DashboardTableContextManager)contextManager;
        
        String tableBeanSignature = contextManager.getPrimaryParamValue("tableSignature");
        Boolean debugModeEnabled = contextManager.getShowDebugMode();
        Boolean showAllDataValues = tableContextManager.getShowAllDataValues();
        Boolean showGlobalZoneValues = tableContextManager.getShowGlobalZoneValues();
        
        DashboardTableBeanFactory tableBeanFactory = contextManager.getTableBeanFactory();
        DashboardTableBean tableBean = tableBeanFactory.getTableBeanBySignature(tableContextManager,
        											DashboardTableBeanFactory.DASHBOARD_TABLE_CACHE_LONG_TIMEOUT,tableBeanSignature);

        String timeFrom = getAdjustedTimeFromValue(contextManager);
        String timeWindow = contextManager.getPrimaryParamValue("timeWindow");
        String maxTSPerGraphString = contextManager.getPrimaryParamValue("maxTSPerGraph");
        String graphMultipleSeriesType = contextManager.getPrimaryParamValue("graphMultipleSeriesType");
        String resolutionRequest = contextManager.getPrimaryParamValue("resolutionRequest");

        Map<String,String> multipleGraphParams = getGraphParams(null,null,null,null,null,null,null,timeWindow,timeFrom);

        if(tableBean == null) {
            // return an empty map if we got a bad request for some reason
            return multipleGraphParams;
        }


        String title = tableBean.getPlainTableTitle();
        Set<String> subTitles = tableBean.getSubTitles();
        
        String currSubTitle = contextManager.getPrimaryParamValue("subTitle");
        
        if(currSubTitle == null) {
        	// default to the first in the list
        	Iterator<String> subTitleIter = subTitles.iterator();
        	if(subTitleIter.hasNext())
        		currSubTitle = subTitleIter.next();
        	else
        		currSubTitle = "";
        }
        
        boolean foundCurrSubTitleData = addCurrentSubTitleInfoToMultipleGraphParams(tableContextManager,multipleGraphParams,tableBean,currSubTitle);
        
        // now build list of related subTitles
        // make sure we are at the parent, if a joined table
        DashboardTableBean parentTableBean = tableBean.getJoinedParentTable();
        if(parentTableBean != null) {
        	if(!foundCurrSubTitleData) {
        		foundCurrSubTitleData = addCurrentSubTitleInfoToMultipleGraphParams(tableContextManager,multipleGraphParams,parentTableBean,currSubTitle);
        	}
        	
        	tableBean = parentTableBean;
        }
        
        int count=0;
        if(subTitles != null) {
        	for(String subTitle:subTitles) {
            	multipleGraphParams.put(SUBTITLE_STR + count,subTitle);
            	count++;
        	}
        }
        
        // now do child joined tables
        Iterable<DashboardTableBean> childTables = tableBean.getJoinedChildTables();
        if(childTables != null) {
        	
        	multipleGraphParams.put(SUBTITLE_STR + count++,RELATED_SUBTITLE_SEPARATOR);
        	
        	for(DashboardTableBean childTable:childTables) {
        		
        		if(!foundCurrSubTitleData) {
        			foundCurrSubTitleData = addCurrentSubTitleInfoToMultipleGraphParams(tableContextManager,multipleGraphParams,childTable,currSubTitle);
        		}
        		
        		subTitles = childTable.getSubTitles();
        		
        		if(subTitles != null) {
        			for(String subTitle:subTitles) {
        				multipleGraphParams.put(SUBTITLE_STR + count,subTitle);
            			count++;
        			}
        		}
        	}
        }
        
        if(title != null)
            multipleGraphParams.put("title",title);
        if(currSubTitle != null)
            multipleGraphParams.put("subTitle",currSubTitle);
        if(maxTSPerGraphString != null)
            multipleGraphParams.put("maxTSPerGraph",maxTSPerGraphString);
        if(graphMultipleSeriesType != null)
            multipleGraphParams.put("graphMultipleSeriesType",graphMultipleSeriesType);
        if(resolutionRequest != null)
            multipleGraphParams.put("resolutionRequest",resolutionRequest);
        if(debugModeEnabled)
            multipleGraphParams.put(DEBUG_MODE_PARAM,"on");
        if(showAllDataValues)
            multipleGraphParams.put(SHOW_ALL_DATA_VALUES,"on");
        if(showGlobalZoneValues)
            multipleGraphParams.put(SHOW_GLOBAL_ZONE_VALUES,"on");

        return multipleGraphParams; 
    }
    
    private static boolean addCurrentSubTitleInfoToMultipleGraphParams(DashboardTableContextManager tableContextManager,Map<String,String> multipleGraphParams,
    																	DashboardTableBean tableBean,String currSubTitle) {
    	
    	List<String> events = tableBean.getEventsBySubTitle(currSubTitle);
    	if(events == null || events.size() == 0) {
    		log.debug("Couldn't find subTitle info for '" + currSubTitle + ", in table " + tableBean.getPlainTableTitle());
    		return false;
    	}
    	
    	List<String> attrs = tableBean.getAttributesBySubTitle(currSubTitle);
    	if(attrs == null || attrs.size() == 0) {
    		log.debug("Couldn't find subTitle info for '" + currSubTitle + ", in table " + tableBean.getPlainTableTitle());
    		return false;
    	}
    	
        String host = tableBean.getTableHost();
        String path = tableBean.getTableDepPath();
        String type = tableBean.getTableDepType();
        String graphType = tableBean.getTableGraphType().toString();
        
        if(host != null)
        	multipleGraphParams.put("host",host);
        if(path != null)
        	multipleGraphParams.put("path",path);
        if(type != null)
        	multipleGraphParams.put("type",type);
        if(graphType != null)
        	multipleGraphParams.put("graphType",graphType);
        
        
    	List<String> alertFlags = getAlertFlags(tableContextManager,events,attrs,type,path,host); 
    
    	Iterator<String> eventIter = events.iterator();
    	Iterator<String> attrIter = attrs.iterator();
    	Iterator<String> alertFlagIter = alertFlags.iterator();
    
    	int count = 0;
    	while(eventIter.hasNext() && attrIter.hasNext() && alertFlagIter.hasNext()) {
        	multipleGraphParams.put(EVENT_TYPE_STR + count, eventIter.next());
        	multipleGraphParams.put(KEY_STR + count,attrIter.next());
        	multipleGraphParams.put(ALERT_STR + count,alertFlagIter.next());
        	count++;
    	} 
    	
    	log.debug("found subTitle info for '" + currSubTitle + ", in table " + tableBean.getPlainTableTitle());
    	return true;
    }
    
    private static Map<String,String> getMultipleGraphParamsByParamList(DashboardContextManager contextManager) {
        
        String timeFrom = getAdjustedTimeFromValue(contextManager);
        String host = contextManager.getPrimaryParamValue("host");
        String path = contextManager.getPrimaryParamValue("path");
        String type = contextManager.getPrimaryParamValue("type");
        String graphType = contextManager.getPrimaryParamValue("graphType");
        String timeWindow = contextManager.getPrimaryParamValue("timeWindow");
        
        Map<String,String> multipleGraphParams = getGraphParams(graphType,type,path,host,null,null,null,timeWindow,timeFrom);
        
        String eventType;
        int count = 0;
        
        while((eventType = contextManager.getPrimaryParamValue(EVENT_TYPE_STR + count)) != null) {
            multipleGraphParams.put(EVENT_TYPE_STR + count, eventType);
            multipleGraphParams.put(KEY_STR + count,contextManager.getPrimaryParamValue(KEY_STR + count));
            multipleGraphParams.put(ALERT_STR + count,contextManager.getPrimaryParamValue(ALERT_STR + count));
            count++;
        }
        
        String title = contextManager.getPrimaryParamValue("title");
        String subTitle = contextManager.getPrimaryParamValue("subTitle");
        
        if(title != null)
            multipleGraphParams.put("title",title);
        if(subTitle != null)
            multipleGraphParams.put("subTitle",subTitle);
        
        return multipleGraphParams; 
    }
    
    public static Map<String,String> getGraphParams(String graphType,String type,String path,String host,
                                                    String eventType,String key,String alert,
                                                    String timeWindow) {
        
        return getGraphParams(graphType,
                              type,
                              path,
                              host,
                              eventType,
                              key,
                              alert,
                              timeWindow,
                              null);
    }
    
    public static Map<String,String> getGraphParams(String graphType,String type,String path,String host,
                                                    String eventType,String key,String alert,
                                                    String timeWindow,String timeFrom) {
        HashMap<String,String> graphParams = new HashMap<String,String>();
    
    	if(eventType != null) {
        	graphParams.put("eventType",eventType);
    	}
    
    	if(key != null) {
        	graphParams.put("key",key);
    	}
    	
    	if(alert != null && alert.length() > 0) {
    	    graphParams.put("alert","true");
        }
    
    	if(timeWindow != null) {
        	graphParams.put("timeWindow",timeWindow);
    	}

    	if(timeFrom != null) {
        	graphParams.put("timeFrom",timeFrom);
    	}

    	if(host == null)
        	host = DashboardContextUtils.UNDEFINED_HOST_NAME;

    	if(path == null)
        	path = DashboardContextUtils.UNDEFINED_PATH_NAME;
    	
    	if(type == null)
        	type = DashboardContextUtils.UNDEFINED_TYPE_NAME;

        graphParams.put("host",host);
        graphParams.put("path",path);
        graphParams.put("type",type);

    	if(graphType != null)
        	graphParams.put("graphType",graphType);
    	
        return graphParams;
    }
    
    private static String getAdjustedTimeFromValue(DashboardContextManager contextManager) {
        
        
        // adjust any time range params
        String timeWindowShiftHalfBack = contextManager.getPrimaryParamValue("timeWindowShiftHalfBack");
        String timeWindowShiftHalfForward = contextManager.getPrimaryParamValue("timeWindowShiftHalfForward");
        String timeWindowString = contextManager.getPrimaryParamValue("timeWindow");
        String timeFromString = contextManager.getPrimaryParamValue("timeFrom");
        String timeFromUpdate = contextManager.getPrimaryParamValue("timeFromUpdate");
        String timeFromUpdateSubmit = contextManager.getPrimaryParamValue("timeFromUpdateSubmit");

        Long timeWindow;
        if(timeWindowString != null) {
            timeWindow = Long.parseLong(timeWindowString);
        }
        else
            return null;

        boolean timeFromWasNull;
        Long timeFrom;
        if(timeFromUpdateSubmit != null && timeFromUpdate != null) {
            timeFrom = dateTimeFormatter.parseMillis(timeFromUpdate);
            timeFromWasNull = false;
        }
        else if(timeFromString != null) {
            timeFrom = Long.parseLong(timeFromString);
            timeFromWasNull = false;
        }
        else {
            timeFrom = System.currentTimeMillis() - timeWindow;
            timeFromWasNull = true;
        }

        if(timeWindowShiftHalfBack != null) {
            timeFrom -= timeWindow / 2L;
            return timeFrom.toString();
        }
        else if(timeWindowShiftHalfForward != null) {
            timeFrom += timeWindow / 2L;
            if(timeFrom > System.currentTimeMillis() - timeWindow)
                return null;
            else
                return timeFrom.toString();
        }
        
        if(timeFromWasNull)
            return null;
        else
            return timeFrom.toString();
    } 
    
    public static Long getGraphTimeWindowParameter(Map<String,String> graphParams) {
        String timeWindowString = graphParams.get("timeWindow");
        if(timeWindowString == null) {
            return null;
        }
        
        try {
            Long timeWindow = Long.parseLong(timeWindowString);
            return timeWindow;
        }
        catch(NumberFormatException nfEx) {
            log.warn(nfEx);
            return null;
        }
    }
    
    public static Long getGraphTimeFromParameter(Map<String,String> graphParams) {
        String timeFromString = graphParams.get("timeFrom");
        if(timeFromString == null) {
            return null;
        }

        try {
            Long timeFrom = Long.parseLong(timeFromString);
            return timeFrom;
        }
        catch(NumberFormatException nfEx) {
            log.warn(nfEx);
            return null;
        }
    }
    
    public static Long getGraphTimeFromDerived(Map<String,String> graphParams,Long timeWindow) {
        Long timeFrom = getGraphTimeFromParameter(graphParams);
        if(timeFrom == null)
            timeFrom = System.currentTimeMillis() - timeWindow;
        
        return timeFrom;
    }
    
    public static Long getGraphTimeToDerived(Map<String,String> graphParams) {
        Long timeWindow = getGraphTimeWindowParameter(graphParams);
        if(timeWindow == null)
            timeWindow = DashboardGraphUtils.TimeWindow.getDefaultTimeWindow().getMillis();
        
        Long timeFrom = getGraphTimeFromDerived(graphParams,timeWindow);
        
        return timeFrom + timeWindow;
    }
    
    public static String getFormattedDateFromMillis(Long millis) {
        return dateFormatter.print(millis);
    }

    public static String getFormattedTimeFromMillis(Long millis) {
        return timeFormatter.print(millis);
    }

    public static String getFormattedDateTimeFromMillis(Long millis) {
        return dateTimeFormatter.print(millis);
    }
    
    public static List<String> getGraphPageTimeWindowRefreshParams(DashboardContextManager contextManager) {
        
        ArrayList<String> retList = new ArrayList<String>();
        Map<String,String[]> contextParams = (Map<String,String[]>)contextManager.getParameterMap();
        
        Set<String> contextKeys = contextParams.keySet();
        
        if(contextKeys != null) {
	    	for(String contextKey:contextKeys) {
	        
	        	if(contextKey.equals("subTitle") || 
	        	        contextKey.startsWith("timeWindow") || 
	        	        contextKey.startsWith("timeFrom") ||
	        	        contextKey.equals("maxTSPerGraph") ||
	        	        contextKey.equals("resolutionRequest"))
	            	continue;
	        	
	        	String[] values = contextParams.get(contextKey);
	        	if(values == null)
	        	    continue;
	        	
	        	for(String value:values) {
	        	    String hiddenHtmlInputTag = "<input type=\"hidden\" name=\"" + contextKey + "\" value=\"" + value + "\"\\>";
	        	    retList.add(hiddenHtmlInputTag);
	        	}
	    	}
        }
    	
    	return retList;
    }
        
    public static String getMultipleGraphPageTitle(Map<String,String> multipleGraphParams) {
        return multipleGraphParams.get("title");
    } 
    
    public static String getMultipleGraphPageSubTitle(Map<String,String> multipleGraphParams) {
        return multipleGraphParams.get("subTitle");
    }
    
    public static int getMaxTSPerGraph(Map<String,String> multipleGraphParams) {
        String maxTSPerGraphString = multipleGraphParams.get("maxTSPerGraph");
        if(maxTSPerGraphString != null) {
            try {
                int maxTSPerGraph = Integer.parseInt(maxTSPerGraphString);
                return maxTSPerGraph;
            }
            catch(NumberFormatException nfEx) {
                log.info(nfEx);
            }
        }
    
        return DEFAULT_MAX_TIME_SERIES_PER_GRAPH;
    }
    
    public static String getResolutionRequest(Map<String,String> multipleGraphParams) {
        String resolutionRequestString = multipleGraphParams.get("resolutionRequest");
        if(resolutionRequestString != null) {
        	return resolutionRequestString;
        }
    
        return ResolutionRequestType.BEST_FIT.toString();
    }
    
    public static List<String> addToListUnique(List<String> list,String value) {
        
        if(list == null)
            list = new ArrayList<String>();
            
        if(!list.contains(value))
            list.add(value);
        
        return list;
    } 
    
    public static boolean isSubTitlePerEventSpecific(String subTitle) {
    	if(subTitle.startsWith(EVENT_TYPE_SUBTITLE_PREFIX)) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }

    public static String getGraphTitle(String graphType,String host,String type,String path,String eventType,String attributeType) {
        if(graphType.equals(GraphType.BY_HOST.toString())) {
            return eventType + "->" + attributeType + " for " + host;
        }
        else if(graphType.equals(GraphType.BY_PATH_WITH_TYPE.toString())) {
            return eventType + "->" + attributeType + " for " + path + " & " + type;
        }
        else if(graphType.equals(GraphType.BY_TYPE.toString())) {
            return eventType + "->" + attributeType + " for " + type;
        }
        else if(graphType.equals(GraphType.BY_GLOBAL_ZONE_LIST_FOR_PATH_WITH_TYPE.toString())) {
            return eventType + "->" + attributeType + " for " + path + " & " + type;
        }
        else if(graphType.equals(GraphType.BY_GLOBAL_ZONE_LIST_FOR_TYPE.toString())) {
            return eventType + "->" + attributeType + " for " + type;
        }
        return null;
    }
}
