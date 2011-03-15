package com.ning.arecibo.dashboard.graph;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.awt.Color;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Timestamp;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.Plot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import com.ning.arecibo.dashboard.context.ContextMbeanManager;
import com.ning.arecibo.dashboard.context.DashboardContextManager;
import com.ning.arecibo.dashboard.context.DashboardContextUtils;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAO;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAOException;
import com.ning.arecibo.dashboard.dao.ResolutionRequest;
import com.ning.arecibo.dashboard.dao.ResolutionRequestType;
import com.ning.arecibo.dashboard.format.DashboardGraphFormatter;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;
import com.ning.arecibo.dashboard.graph.DashboardGraphUtils.GraphMultipleSeriesType;
import com.ning.arecibo.dashboard.graph.DashboardGraphUtils.GraphType;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.dashboard.context.DashboardContextUtils.*;
import static com.ning.arecibo.dashboard.graph.DashboardGraphUtils.*;

public class DashboardGraphLegendServlet extends HttpServlet
{
    private final static Logger log = Logger.getLogger(DashboardGraphLegendServlet.class);
    
    private final static String TABLE_START = "<table border=\"2\" cellspacing=\"0\" cellpadding=\"0\" ";
    private final static String BGCOLOR_HEADER_SECTION_START = "<th>";
    private final static String HEADER_SECTION_END = "</th>";
    private final static String BGCOLOR_DATA_SECTION_START = "<td><span style=\"display:none\">";
    private final static String DATA_SECTION_END = "</td>";
 
 
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
        
        long startMs = System.currentTimeMillis();
        
        DashboardContextManager contextManager = new DashboardContextManager(request);
        
        DashboardCollectorDAO collectorDAO = contextManager.getCollectorDAO();
        DashboardGraphFormatter formatter = contextManager.getGraphFormatter();
        ContextMbeanManager mbeanManager = contextManager.getMbeanManager();
        boolean showDebugMode = contextManager.getShowDebugMode();
        boolean showAllDataValues = contextManager.getShowAllDataValues();
        boolean showGlobalZoneValues = contextManager.getShowGlobalZoneValues();

        String graphType = contextManager.getPrimaryParamValue("graphType");
        String graphMultipleSeriesType = contextManager.getPrimaryParamValue("graphMultipleSeriesType");
        String eventType = contextManager.getPrimaryParamValue("eventType");
        String attributeType = contextManager.getPrimaryParamValue("attributeType");
        String timeWindowString = contextManager.getPrimaryParamValue("timeWindow");
        String timeFromString = contextManager.getPrimaryParamValue("timeFrom"); 
        String resolutionRequestString = contextManager.getPrimaryParamValue("resolutionRequest");

        String subTitle = contextManager.getPrimaryParamValue("subTitle");

        Long timeWindow = DashboardGraphUtils.parseDurationMillis(timeWindowString,TimeWindow.getDefaultTimeWindow().getMillis());
        Long timeFrom = DashboardGraphUtils.parseDateTimeMillis(timeFromString,null);

        if(attributeType == null) {
            // backwards compatibility
            attributeType = contextManager.getPrimaryParamValue("key");
        }

        int maxTSPerGraph = DashboardGraphUtils.DEFAULT_MAX_TIME_SERIES_PER_GRAPH;
        String maxTSPerGraphString = contextManager.getPrimaryParamValue("maxTSPerGraph");
        
        if(maxTSPerGraphString != null) {
            try {
                maxTSPerGraph = Integer.parseInt(maxTSPerGraphString);
            }
            catch(NumberFormatException nfEx) {}
        }
        
        if(graphMultipleSeriesType == null)
        	graphMultipleSeriesType = GraphMultipleSeriesType.OVER_HOSTS.toString(); 
        
        String lowerCaseEventType = eventType.toLowerCase();
        String lowerCaseKey = attributeType.toLowerCase();
        
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

        Map<String,String> perSeriesServletUrlsMap = null;
        Map<String,String> perSeriesServletParamsMap = null;
            	
        try {
            if(graphType == null || graphType.length() == 0)
                throw new IllegalStateException("graphType parameter must be provided");
            
            // add direct links to individual graphs except for those already BY_HOST
            if(!graphType.equals(DashboardGraphUtils.GraphType.BY_HOST.toString())) {
            	perSeriesServletUrlsMap = new HashMap<String,String>();
            	perSeriesServletParamsMap = new HashMap<String,String>();
            	
        		perSeriesServletParamsMap.put("eventType", eventType);
        		perSeriesServletParamsMap.put("key", attributeType);
        		if(timeWindowString != null)
        			perSeriesServletParamsMap.put("timeWindow", timeWindowString);
        		if(timeFromString != null)
        			perSeriesServletParamsMap.put("timeFrom", timeFromString);
        		if(maxTSPerGraphString != null)
        			perSeriesServletParamsMap.put("maxTSPerGraph", maxTSPerGraphString);
        		if(resolutionRequestString != null)
        			perSeriesServletParamsMap.put("resolutionRequest", resolutionRequestString);
        		
        		if(graphMultipleSeriesType.equals(GraphMultipleSeriesType.OVER_HOSTS.toString())) {
        			perSeriesServletParamsMap.put("graphType", GraphType.BY_HOST.toString());
        		}
        		else if(graphMultipleSeriesType.equals(GraphMultipleSeriesType.OVER_TYPES.toString())) {
        			perSeriesServletParamsMap.put("graphType", GraphType.BY_TYPE.toString());
        		}
        		
        		//TODO: Add handling for "alert" flag here, 
        		// ignoring for now, needs to be refined for individual hosts
            }
        
            if(graphType.equals(DashboardGraphUtils.GraphType.BY_HOST.toString())) {
            
                String host = contextManager.getPrimaryParamValue("host");
                if(host == null || host.length() == 0)
                    throw new IllegalStateException("missing host param with graphType = " + graphType);
                
                List<Map<String,Object>> values = collectorDAO.getValuesForHostEvent(host,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                valueMaps.put(host, values);
            }
            else if(graphType.equals(DashboardGraphUtils.GraphType.BY_PATH_WITH_TYPE.toString())) {
            
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
                	
                	perSeriesServletParamsMap.put("host", hostForPathWithType);
                	String servletUrl = DashboardContextUtils.getGraphServletUrl(contextManager, perSeriesServletParamsMap);
                	perSeriesServletUrlsMap.put(hostForPathWithType,servletUrl);
                	
                	if(++count > maxTSPerGraph)
                	    break;
                }

                if (count != 1 || showDebugMode) {
                    List<Map<String,Object>> values = collectorDAO.getValuesForPathWithTypeEvent(path,type,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                    valueMaps.put(path + "_" + type + " aggregated", values);
                }
            }
            else if(graphType.equals(DashboardGraphUtils.GraphType.BY_TYPE.toString())) {
            
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
                	
                	perSeriesServletParamsMap.put("host", hostForType);
                	String servletUrl = DashboardContextUtils.getGraphServletUrl(contextManager, perSeriesServletParamsMap);
                	perSeriesServletUrlsMap.put(hostForType,servletUrl);
                	
                	if(++count > maxTSPerGraph)
                	    break;
                }

                if (count != 1 || showDebugMode) {
                    List<Map<String,Object>> values = collectorDAO.getValuesForTypeEvent(type,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                    valueMaps.put(type + " aggregated", values);
                }
            }
            else if(graphType.equals(DashboardGraphUtils.GraphType.BY_GLOBAL_ZONE_LIST_FOR_PATH_WITH_TYPE.toString())) {
            	
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
                		
                		perSeriesServletParamsMap.put("host", globalZone);
                		String servletUrl = DashboardContextUtils.getGraphServletUrl(contextManager, perSeriesServletParamsMap);
                		perSeriesServletUrlsMap.put(globalZone,servletUrl);
                	
                		if(++count > maxTSPerGraph)
                	    	break;
                	}
                }
            }
            else if(graphType.equals(DashboardGraphUtils.GraphType.BY_GLOBAL_ZONE_LIST_FOR_TYPE.toString())) {
            	
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
                		if(values == null || values.size() == 0)
                			continue;
                		
                		valueMaps.put(globalZone, values);
                		
                		perSeriesServletParamsMap.put("host", globalZone);
                		String servletUrl = DashboardContextUtils.getGraphServletUrl(contextManager, perSeriesServletParamsMap);
                		perSeriesServletUrlsMap.put(globalZone,servletUrl);
                	
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
        
        TimeSeriesCollection dataset = new TimeSeriesCollection();
            
        ArrayList<String> seriesLabels = new ArrayList<String>();
        
        Double globalMin = null;
        Double globalMax = null;
        
        Timestamp timeFromTs = null;
        Timestamp timeToTs = null;
        if(valueMaps != null && valueMaps.size() > 0) {
            
            int count = 0;
            Set<String> dataSeriesNames = valueMaps.keySet();
            
            
            for(String dataSeriesName:dataSeriesNames) {
                
                List<Map<String,Object>> values = valueMaps.get(dataSeriesName);
                if(values == null || values.size() == 0)
                    continue;
                
                // Create a time series chart
                TimeSeries tsValues = new TimeSeries(dataSeriesName, Millisecond.class);
                
                
                // add all the data values
                Double min = null;
                Double max = null;
                double total = 0.0;
                for(Map<String,Object> valueMap:values) {
                    Timestamp ts = (Timestamp)valueMap.get("ts");
                    Number num = (Number)valueMap.get("value");
                    double value = num.doubleValue();
        	            
                    tsValues.addOrUpdate(new Millisecond(ts),num);
                    
                    if(min == null || min > value)
                        min = value;
                    if(max == null || max < value)
                        max = value;
                    total += value;
                }
                
                double avg = 0.0;
                if(values.size() > 0)
                	avg = total / (double)values.size();
                
                if(min != null) {
                	if(globalMin == null || globalMin > min)
                		globalMin = min;
                }
                
                if(max != null) {
                	if(globalMax == null || globalMax < max)
                		globalMax = max;
                }
                
                String formattedDataSeriesName;
                String dataSeriesUrl;
                if((perSeriesServletUrlsMap != null) && 
                		((dataSeriesUrl = perSeriesServletUrlsMap.get(dataSeriesName)) != null)) {
                	//formattedDataSeriesName = "<a href=\"" + dataSeriesUrl + "\" target=\"blank\">" + dataSeriesName + "</a>";
                	
                	String popupHtml = "<img src=&quot;" + dataSeriesUrl + "&quot; width=&quot;" + DashboardGraphUtils.DEFAULT_GRAPH_WIDTH + 
                														"&quot; height=&quot;" + DashboardGraphUtils.DEFAULT_GRAPH_HEIGHT + "&quot;";
                	
                	String popupString = DashboardContextUtils.getPopupString(popupHtml,
                															DashboardGraphUtils.DEFAULT_GRAPH_WIDTH,
                															DashboardGraphUtils.DEFAULT_GRAPH_HEIGHT);
															 
                	if(graphMultipleSeriesType.equals(GraphMultipleSeriesType.OVER_HOSTS.toString())) {
                		String hostUrl = DashboardContextUtils.getRelativeGraphPageByHostUrl(contextManager,dataSeriesName,subTitle,showAllDataValues,showGlobalZoneValues,showDebugMode);
                		formattedDataSeriesName = "<div " + popupString + "><a href='" + hostUrl + "'>"+ dataSeriesName + "</a></div>";
                	}
                	else {
                		formattedDataSeriesName = "<div " + popupString + ">" + dataSeriesName + "</div>";
                	}
                }
                else {
                	formattedDataSeriesName = dataSeriesName;
                }
                
                String minString = formatter.getFormattedValue(lowerCaseEventType,lowerCaseKey,min);
                String maxString = formatter.getFormattedValue(lowerCaseEventType,lowerCaseKey,max);
                String avgString = formatter.getFormattedValue(lowerCaseEventType,lowerCaseKey,avg);
                
                String minSortable = String.format("%.20f",min);
                String maxSortable = String.format("%.20f",max);
                String avgSortable = String.format("%.20f",avg);
                
                String label = BGCOLOR_DATA_SECTION_START + dataSeriesName + "</span>" + formattedDataSeriesName +
                                DATA_SECTION_END + BGCOLOR_DATA_SECTION_START + minSortable + "</span>" + minString +
                                DATA_SECTION_END + BGCOLOR_DATA_SECTION_START + maxSortable + "</span>" + maxString +
                                DATA_SECTION_END + BGCOLOR_DATA_SECTION_START + avgSortable + "</span>" + avgString + DATA_SECTION_END;
                
                seriesLabels.add(label);
                
                // add null at beginning, to establish start time range
                if(timeFrom != null)
                    timeFromTs = new Timestamp(timeFrom);
                else
                    timeFromTs = new Timestamp(System.currentTimeMillis() - timeWindow);
                tsValues.addOrUpdate(new Millisecond(timeFromTs), null);
                
                // add null at end to establish end time range
                if(timeFrom != null)
                    timeToTs = new Timestamp(timeFrom + timeWindow);
                else
                    timeToTs = new Timestamp(System.currentTimeMillis());
                tsValues.addOrUpdate(new Millisecond(timeToTs), null);
                
                dataset.addSeries(tsValues);
                count++;
                
                if(count >= maxTSPerGraph)
                    break;
            }
        }
        
        String formattedDateFrom;
        if(timeFromTs != null)
            formattedDateFrom = DashboardContextUtils.getFormattedDateFromMillis(timeFromTs.getTime());
        else
            formattedDateFrom = "";
        
        String formattedDateTo;
        if(timeToTs != null)
            formattedDateTo = DashboardContextUtils.getFormattedDateFromMillis(timeToTs.getTime());
        else
            formattedDateTo = "";
        
        String timeLegend;
        if(formattedDateFrom.equals(formattedDateTo))
            timeLegend = formattedDateFrom + " GMT";
        else
            timeLegend = formattedDateFrom + " -- " + formattedDateTo + " GMT";
           
        
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                          "",
                          timeLegend,
                attributeType,
                          dataset,
                          false,
                          false,
                          false);
        
        Plot plot = chart.getPlot();
        
        if(chart == null) {
            response.setContentType("text/html");
        	Writer writer = response.getWriter();
        	writer.write("No chart generated");
        }
        else {
        
            // note need to actually write the chart, in order to recover the proper legend color ordering
            // TODO: Cache the legends/graphs, so don't need to recreate on each legend request, etc.
            OutputStream os = new ByteArrayOutputStream();
            ChartUtilities.writeChartAsPNG(os,
                                           chart,
                                           DashboardGraphUtils.DEFAULT_GRAPH_WIDTH,
                                           DashboardGraphUtils.DEFAULT_GRAPH_HEIGHT);
            os.close();
            
            LegendItemCollection legendItems = plot.getLegendItems();
            Iterator<LegendItem> legendIter = (Iterator<LegendItem>)legendItems.iterator();
            Iterator<String> seriesLabelIter = seriesLabels.iterator();
            
            StringBuilder legendHtml = new StringBuilder();
			String legendId = "legend-" + ((int)(Math.random()*1000));

			legendHtml.append("<div class='pager' id='"+legendId+"Pager'><form onsubmit='return false;''>");
			legendHtml.append("<input class='pagedisplay' size='3' type='text' readonly/>");
			legendHtml.append("<img class='first' src='../images/first.png'/>");
			legendHtml.append("<img class='prev' src='../images/prev.png'/>");
			legendHtml.append("<img class='next' src='../images/next.png'/>");
			legendHtml.append("<img class='last' src='../images/last.png'/>");
			legendHtml.append("<select class='pagesize'><option value='10' selected='selected'>10</option><option value='25'>25</option><option value='50'>50</option></select></form></div>");

			
            legendHtml.append(TABLE_START + " id='" + legendId + "'>");
            legendHtml.append("<thead><tr>" +
                              BGCOLOR_HEADER_SECTION_START + HEADER_SECTION_END + BGCOLOR_HEADER_SECTION_START + HEADER_SECTION_END +
                              BGCOLOR_HEADER_SECTION_START + "<b>min</b>" + HEADER_SECTION_END +
                              BGCOLOR_HEADER_SECTION_START + "<b>max</b>" + HEADER_SECTION_END +
                              BGCOLOR_HEADER_SECTION_START + "<b>avg</b>" + HEADER_SECTION_END + 
                              "</tr></thead>");
            legendHtml.append("<tbody>");
            
            while(legendIter.hasNext() && seriesLabelIter.hasNext()) {
            	
                LegendItem legendItem = legendIter.next();
                String seriesLabel = seriesLabelIter.next();
                
                Color lineColor = (Color)legendItem.getLinePaint();
                String hexColor = Integer.toHexString(lineColor.getRGB() & 0x00ffffff);
                
                // add back the leading 0's
                while(hexColor.length() < 6) {
                    hexColor = "0" + hexColor;
                }
     
                legendHtml.append("<tr>");
                legendHtml.append(BGCOLOR_DATA_SECTION_START + hexColor + "</span>" + 
                		"<div style=\"width: 18px; height: 18px; background-color: #" + hexColor + "\"/>" + DATA_SECTION_END);
                legendHtml.append(seriesLabel);
                legendHtml.append("</tr>");
                
                
            }
            legendHtml.append("</tbody></table>");

			legendHtml.append("<script>$('#" + legendId + 
					"').tablesorter" +
					"({widthFixed:true, widgets:['zebra'], textExtraction: legendTextExtraction})" +
					".tablesorterPager({positionFixed:false,container:$('#" + legendId + "Pager')});</script>");
			
            response.setContentType("text/html");
        	Writer writer = response.getWriter();
        	writer.write(legendHtml.toString());
        }
        
        incrementRequestCount(mbeanManager);
        updateRequestTotalMs(mbeanManager,System.currentTimeMillis() - startMs);
    }
    
    private void incrementRequestCount(ContextMbeanManager mbeanManager) {
        mbeanManager.incrementLegendRequestCount();
    }
    
    private void updateRequestTotalMs(ContextMbeanManager mbeanManager,long updateMs) {
    	mbeanManager.updateLegendRequestTotalMs(updateMs);
    }
}
