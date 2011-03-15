package com.ning.arecibo.dashboard.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import com.ning.arecibo.dashboard.context.ContextMbeanManager;
import com.ning.arecibo.dashboard.context.DashboardContextManager;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAO;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAOException;
import com.ning.arecibo.dashboard.dao.ResolutionRequest;
import com.ning.arecibo.dashboard.dao.ResolutionRequestType;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;

import com.ning.arecibo.util.Logger;


import static com.ning.arecibo.dashboard.graph.DashboardGraphUtils.*;

public class DashboardDataServlet extends HttpServlet
{
    private final static Logger log = Logger.getLogger(DashboardDataServlet.class);
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
        
        long startMs = System.currentTimeMillis();
        
        DashboardContextManager contextManager = new DashboardContextManager(request);
        
        DashboardCollectorDAO collectorDAO = contextManager.getCollectorDAO();
        ContextMbeanManager mbeanManager = contextManager.getMbeanManager();
        boolean showDebugMode = contextManager.getShowDebugMode(); 

        String graphType = contextManager.getPrimaryParamValue("graphType");
        String graphMultipleSeriesType = contextManager.getPrimaryParamValue("graphMultipleSeriesType");
        String eventType = contextManager.getPrimaryParamValue("eventType");
        String attributeType = contextManager.getPrimaryParamValue("attributeType");
        String timeWindowString = contextManager.getPrimaryParamValue("timeWindow");
        String timeFromString = contextManager.getPrimaryParamValue("timeFrom"); 
        String resolutionRequestString = contextManager.getPrimaryParamValue("resolutionRequest");
 
        Long timeWindow = DashboardGraphUtils.parseDurationMillis(timeWindowString,TimeWindow.getDefaultTimeWindow().getMillis());
        Long timeFrom = DashboardGraphUtils.parseDateTimeMillis(timeFromString,null);

        if(attributeType == null) {
            // backwards compatibility
            attributeType = contextManager.getPrimaryParamValue("key");
        }
        
        int maxTSPerGraph = DEFAULT_MAX_TIME_SERIES_PER_GRAPH;
        String maxTSPerGraphString = contextManager.getPrimaryParamValue("maxTSPerGraph");
        
        if(maxTSPerGraphString != null) {
            try {
                maxTSPerGraph = Integer.parseInt(maxTSPerGraphString);
            }
            catch(NumberFormatException nfEx) {}
        }
        
        if(graphMultipleSeriesType == null)
        	graphMultipleSeriesType = GraphMultipleSeriesType.OVER_HOSTS.toString();
        
        ResolutionRequest resolutionRequest = null;
        if(resolutionRequestString != null) {
        	
        	// see if we can parse a numerical fixed resolution
        	try {
        		int reduction = Integer.parseInt(resolutionRequestString);
        		resolutionRequest = new ResolutionRequest(ResolutionRequestType.FIXED,reduction);
        	}
        	catch(NumberFormatException nfEx) {}
        	
        	if(resolutionRequest == null) {
        	
        		// see if we can parse an enum type
        		try {
        			ResolutionRequestType reqType = ResolutionRequestType.valueOf(resolutionRequestString);
        			resolutionRequest = new ResolutionRequest(reqType,DashboardGraphUtils.DEFAULT_GRAPH_REFERENCE_RESOLUTION_WIDTH);
        		}
        		catch(IllegalStateException isEx) {}
        	}
        	
        	if(resolutionRequest == null) {
        		// return the default
        		resolutionRequest = new ResolutionRequest(ResolutionRequestType.BEST_FIT,DashboardGraphUtils.DEFAULT_GRAPH_REFERENCE_RESOLUTION_WIDTH);
        	}
        }
        else {
        	resolutionRequest = new ResolutionRequest(ResolutionRequestType.BEST_FIT,DashboardGraphUtils.DEFAULT_GRAPH_REFERENCE_RESOLUTION_WIDTH);
        }
        
        Map<String,List<Map<String,Object>>> valueMaps = new TreeMap<String,List<Map<String,Object>>>(DashboardGraphLegendComparator.getInstance());
        
        try {
            if(graphType == null || graphType.length() == 0)
                throw new IllegalStateException("graphType parameter must be provided");
            
            if(graphType.equals(GraphType.BY_HOST.toString())) {
            
                String host = contextManager.getPrimaryParamValue("host");
                if(host == null || host.length() == 0)
                    throw new IllegalStateException("missing host param with graphType = " + graphType);
                
                List<Map<String,Object>> values = collectorDAO.getValuesForHostEvent(host,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                valueMaps.put(host, values);
            }
            else if(graphType.equals(GraphType.BY_PATH_WITH_TYPE.toString())) {
            
                String path = contextManager.getPrimaryParamValue("path");
                if(path == null || path.length() == 0)
                    throw new IllegalStateException("missing path param with graphType = " + graphType);
                
                String type = contextManager.getPrimaryParamValue("type");
                if(type == null || type.length() == 0)
                    throw new IllegalStateException("missing type param with graphType = " + graphType);
                
                List<String> hostsForPathWithType = collectorDAO.getHostsForPathWithType(path, type);
                int count = 0;
                for(String hostForPathWithType:hostsForPathWithType) {
                    List<Map<String,Object>> values = collectorDAO.getValuesForHostEvent(hostForPathWithType,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                    if(values == null || values.size() == 0)
                        continue;
                    
                	valueMaps.put(hostForPathWithType, values);
                	
                	if(++count > maxTSPerGraph)
                	    break;
                }

                if (count != 1 || showDebugMode) {
                    List<Map<String,Object>> values = collectorDAO.getValuesForPathWithTypeEvent(path,type,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                    valueMaps.put(path + "_" + type, values);
                }
            }
            else if(graphType.equals(GraphType.BY_TYPE.toString())) {
            
                String type = contextManager.getPrimaryParamValue("type");
                if(type == null || type.length() == 0)
                    throw new IllegalStateException("missing type param with graphType = " + graphType);

                List<String> hostsForType = collectorDAO.getHostsForType(type);
                int count = 0;
                for(String hostForType:hostsForType) {
                    List<Map<String,Object>> values = collectorDAO.getValuesForHostEvent(hostForType,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                    if(values == null || values.size() == 0)
                        continue;
                    
                	valueMaps.put(hostForType, values);
                	
                	if(++count > maxTSPerGraph)
                	    break;
                }

                if (count != 1 || showDebugMode) {
                    List<Map<String,Object>> values = collectorDAO.getValuesForTypeEvent(type,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                    valueMaps.put(type, values);
                }
            }
            else if(graphType.equals(GraphType.BY_GLOBAL_ZONE_LIST_FOR_PATH_WITH_TYPE.toString())) {
            	
                String path = contextManager.getPrimaryParamValue("path");
                if(path == null || path.length() == 0)
                    throw new IllegalStateException("missing path param with graphType = " + graphType);
                
                String type = contextManager.getPrimaryParamValue("type");
                if(type == null || type.length() == 0)
                    throw new IllegalStateException("missing type param with graphType = " + graphType);
                
                List<String> hostsInPathWithType = collectorDAO.getHostsForPathWithType(path,type);
                ArrayList<String> globalZoneHosts = new ArrayList<String>();
                
                GalaxyStatusManager galaxyStatusManager = contextManager.getGalaxyStatusManager();
        
                int count = 0;
                for(String host:hostsInPathWithType) {
                	String globalZone = galaxyStatusManager.getGlobalZone(host);
                	if(!globalZoneHosts.contains(globalZone)) {
                		globalZoneHosts.add(globalZone);
                		
                		List<Map<String,Object>> values = collectorDAO.getValuesForHostEvent(globalZone,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                		if(values == null || values.size() == 0)
                			continue;
                		
                		valueMaps.put(globalZone, values);
                		
                		if(++count > maxTSPerGraph)
                	    	break;
                	}
                }
            }
            else if(graphType.equals(GraphType.BY_GLOBAL_ZONE_LIST_FOR_TYPE.toString())) {
            	
                String type = contextManager.getPrimaryParamValue("type");
                if(type == null || type.length() == 0)
                    throw new IllegalStateException("missing type param with graphType = " + graphType);
                
                List<String> hostsInType = collectorDAO.getHostsForType(type);
                ArrayList<String> globalZoneHosts = new ArrayList<String>();
                
                GalaxyStatusManager galaxyStatusManager = contextManager.getGalaxyStatusManager();
        
                int count = 0;
                for(String host:hostsInType) {
                	String globalZone = galaxyStatusManager.getGlobalZone(host);
                	if(!globalZoneHosts.contains(globalZone)) {
                		globalZoneHosts.add(globalZone);
                		
                		List<Map<String,Object>> values = collectorDAO.getValuesForHostEvent(globalZone,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                		if(values == null || values.size() == 0) {
                			continue;
                		}
                		
                		valueMaps.put(globalZone, values);
                		
                		if(++count > maxTSPerGraph)
                	    	break;
                	}
                }
            } 
            else {
                throw new IllegalStateException("Unrecognized graphType = '" + graphType + "'");
            }
        }
        catch(DashboardCollectorDAOException ddEx) {
            log.warn(ddEx);
            valueMaps.clear();
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx);
            valueMaps.clear();
        }
        
        response.setContentType("text/plain");
    	Writer writer = response.getWriter();
    	
        if(valueMaps != null && valueMaps.size() > 0) {
            
            int count = 0;
            Set<String> dataSeriesNames = valueMaps.keySet();
            
            
            for(String dataSeriesName:dataSeriesNames) {
            	
                List<Map<String,Object>> values = valueMaps.get(dataSeriesName);
                if(values == null || values.size() == 0)
                    continue;
                
                // add all the data values
                for(Map<String,Object> valueMap:values) {
                    Timestamp ts = (Timestamp)valueMap.get("ts");
                    DateTime dt = new DateTime(ts.getTime());
                    Number num = (Number)valueMap.get("value");
        	            
                    writer.write(dataSeriesName + "," + num + "," + dt.toString() + "\n");
                }
                
                count++;
                
                if(count >= maxTSPerGraph)
                    break;
            }
        }
        
        incrementRequestCount(mbeanManager);
        updateRequestTotalMs(mbeanManager,System.currentTimeMillis() - startMs);
    }
    
    private void incrementRequestCount(ContextMbeanManager mbeanManager) {
        mbeanManager.incrementDataRequestCount();
    }
    
    private void updateRequestTotalMs(ContextMbeanManager mbeanManager,long updateMs) {
    	mbeanManager.updateDataRequestTotalMs(updateMs);
    }
}
