package com.ning.arecibo.dashboard.graph;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Timestamp;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.RangeType;
import org.jfree.ui.HorizontalAlignment;
import com.ning.arecibo.dashboard.context.ContextMbeanManager;
import com.ning.arecibo.dashboard.context.DashboardContextManager;
import com.ning.arecibo.dashboard.context.DashboardContextUtils;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAO;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAOException;
import com.ning.arecibo.dashboard.dao.ResolutionRequest;
import com.ning.arecibo.dashboard.dao.ResolutionRequestType;
import com.ning.arecibo.dashboard.format.DashboardGraphFormatter;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.dashboard.graph.DashboardGraphUtils.*;

public class DashboardGraphServlet extends HttpServlet
{
    private final static Logger log = Logger.getLogger(DashboardGraphServlet.class);
    
    //TODO: These could be injected
    private final static Color DEFAULT_BACKGROUND_COLOR = new Color(0.79F,0.8F,0.79F);
    private final static Color ALERT_BACKGROUND_COLOR = new Color(0.9F,0.8F,0.8F);
    private final static Color ALERT_BORDER_COLOR = Color.RED;

    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
        
        long startMs = System.currentTimeMillis();
        
        DashboardContextManager contextManager = new DashboardContextManager(request);
        
        DashboardCollectorDAO collectorDAO = contextManager.getCollectorDAO();
        DashboardGraphFormatter formatter = contextManager.getGraphFormatter();
        ContextMbeanManager mbeanManager = contextManager.getMbeanManager();
        boolean showDebugMode = contextManager.getShowDebugMode();
        
        String graphType = contextManager.getPrimaryParamValue("graphType");
        String graphMultipleSeriesType = contextManager.getPrimaryParamValue("graphMultipleSeriesType");
        String eventType = contextManager.getPrimaryParamValue("eventType");
        String attributeType = contextManager.getPrimaryParamValue("attributeType");
        String includeIndividualHostsString = contextManager.getPrimaryParamValue("includeIndividualHosts");
        String timeWindowString = contextManager.getPrimaryParamValue("timeWindow");
        String timeFromString = contextManager.getPrimaryParamValue("timeFrom"); 
        String resolutionRequestString = contextManager.getPrimaryParamValue("resolutionRequest");
        String widthString = contextManager.getPrimaryParamValue("width");
        String heightString = contextManager.getPrimaryParamValue("height");
        String alert = contextManager.getPrimaryParamValue("alert");
        String bgColorString = contextManager.getPrimaryParamValue("bgColor");
        String borderColorString = contextManager.getPrimaryParamValue("borderColor");

        Long timeWindow = DashboardGraphUtils.parseDurationMillis(timeWindowString,TimeWindow.getDefaultTimeWindow().getMillis());
        Long timeFrom = DashboardGraphUtils.parseDateTimeMillis(timeFromString,null);

        if (attributeType == null) {
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
        
        String lowerCaseEventType = eventType.toLowerCase();
        String lowerCaseKey = attributeType.toLowerCase();
        
        Color backgroundColor;
        Color borderColor;

        if(bgColorString != null)
            backgroundColor = new Color(Integer.parseInt(bgColorString,16));
        else if(alert != null && alert.length() > 0)
            backgroundColor = ALERT_BACKGROUND_COLOR;
        else
            backgroundColor = DEFAULT_BACKGROUND_COLOR;

        if(borderColorString != null)
            borderColor = new Color(Integer.parseInt(borderColorString,16));
        else if(alert != null && alert.length() > 0)
            borderColor = ALERT_BORDER_COLOR;
        else
            borderColor = null;

        int width;
        if(widthString == null) {
            width = DEFAULT_GRAPH_WIDTH;
        }
        else {
            width = Integer.parseInt(widthString);
        }

        int height;
        if(heightString == null) {
            height = DEFAULT_GRAPH_HEIGHT;
        }
        else {
            height = Integer.parseInt(heightString);
        }

        boolean includeIndividualHosts = true;
        if(includeIndividualHostsString != null) {
            includeIndividualHosts = Boolean.parseBoolean(includeIndividualHostsString);
        }
        
        ResolutionRequest resolutionRequest = null;;
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

        String host = contextManager.getPrimaryParamValue("host");
        String type = contextManager.getPrimaryParamValue("type");
        String path = contextManager.getPrimaryParamValue("path");

        String chartTitle;

        try {
            if(graphType == null || graphType.length() == 0)
                throw new IllegalStateException("graphType parameter must be provided");
            
            if(graphType.equals(GraphType.BY_HOST.toString())) {
            
                if(host == null || host.length() == 0)
                    throw new IllegalStateException("missing host param with graphType = " + graphType);
                
                List<Map<String,Object>> values = collectorDAO.getValuesForHostEvent(host,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                valueMaps.put(host, values);
            }
            else if(graphType.equals(GraphType.BY_PATH_WITH_TYPE.toString())) {
            
                if(path == null || path.length() == 0)
                    throw new IllegalStateException("missing path param with graphType = " + graphType);
                
                if(type == null || type.length() == 0)
                    throw new IllegalStateException("missing type param with graphType = " + graphType);

                int count = 0;
                if(includeIndividualHosts) {
                    List<String> hostsForPathWithType = collectorDAO.getHostsForPathWithType(path, type);
                    for(String hostForPathWithType:hostsForPathWithType) {
                        List<Map<String,Object>> values = collectorDAO.getValuesForHostEvent(hostForPathWithType,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                        if(values == null || values.size() == 0)
                            continue;

                        valueMaps.put(hostForPathWithType, values);

                        if(++count > maxTSPerGraph)
                            break;
                    }
                }

                if(count != 1 || showDebugMode) {
                    List<Map<String,Object>> values = collectorDAO.getValuesForPathWithTypeEvent(path,type,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                    valueMaps.put(path + "_" + type, values);
                }
            }
            else if(graphType.equals(GraphType.BY_TYPE.toString())) {
            
                if(type == null || type.length() == 0)
                    throw new IllegalStateException("missing type param with graphType = " + graphType);
                
                int count = 0;
                if(includeIndividualHosts) {
                    List<String> hostsForType = collectorDAO.getHostsForType(type);
                    for(String hostForType:hostsForType) {
                        List<Map<String,Object>> values = collectorDAO.getValuesForHostEvent(hostForType,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                        if(values == null || values.size() == 0)
                            continue;

                        valueMaps.put(hostForType, values);

                        if(++count > maxTSPerGraph)
                            break;
                    }
                }

                if(count != 1 || showDebugMode) {
                    List<Map<String,Object>> values = collectorDAO.getValuesForTypeEvent(type,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
                    valueMaps.put(type, values);
                }
            }
            else if(graphType.equals(GraphType.BY_GLOBAL_ZONE_LIST_FOR_PATH_WITH_TYPE.toString())) {
            	
                if(path == null || path.length() == 0)
                    throw new IllegalStateException("missing path param with graphType = " + graphType);
                
                if(type == null || type.length() == 0)
                    throw new IllegalStateException("missing type param with graphType = " + graphType);
                
                List<String> hostsInPathWithType = collectorDAO.getHostsForPathWithType(path,type);
                ArrayList<String> globalZoneHosts = new ArrayList<String>();
                
                GalaxyStatusManager galaxyStatusManager = contextManager.getGalaxyStatusManager();
        
                int count = 0;
                for(String hostInPathWithType:hostsInPathWithType) {
                	String globalZone = galaxyStatusManager.getGlobalZone(hostInPathWithType);
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
            	
                if(type == null || type.length() == 0)
                    throw new IllegalStateException("missing type param with graphType = " + graphType);
                
                List<String> hostsInType = collectorDAO.getHostsForType(type);
                ArrayList<String> globalZoneHosts = new ArrayList<String>();
                
                GalaxyStatusManager galaxyStatusManager = contextManager.getGalaxyStatusManager();
        
                int count = 0;
                for(String hostInType:hostsInType) {
                	String globalZone = galaxyStatusManager.getGlobalZone(hostInType);
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

            chartTitle = DashboardContextUtils.getGraphTitle(graphType,host,type,path,eventType,attributeType);
            if(resolutionRequest.getSelectedReduction() != null)
            	chartTitle += " (" + resolutionRequest.getSelectedReduction() + " min data)";
        }
        catch(DashboardCollectorDAOException ddEx) {
            log.warn(ddEx);
            chartTitle = "Got exception retrieving data:" + ddEx.getMessage();
            valueMaps.clear();
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx);
            chartTitle = "Got exception:" + ruEx.getMessage();
            valueMaps.clear();
        }
        
        TimeSeriesCollection dataset = new TimeSeriesCollection();

        Double minDataValue = null;
        Double maxDataValue = null;
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
                double total = 0.0;
                for(Map<String,Object> valueMap:values) {
                    Timestamp ts = (Timestamp)valueMap.get("ts");
                    Number num = (Number)valueMap.get("value");
                    double value = num.doubleValue();
        	            
                    tsValues.addOrUpdate(new Millisecond(ts),num);
                    
                    if(minDataValue == null || minDataValue > value)
                        minDataValue = value;
                    if(maxDataValue == null || maxDataValue < value)
                        maxDataValue = value;
                    total += value;
                }
                
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
                          chartTitle,
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
            
            if(borderColor != null) {
            	chart.setBorderVisible(true);
            	chart.setBorderPaint(borderColor);
        	}
        	else {
            	//chart.setBorderVisible(false);
        	}
            
            // reduce size of title
            TextTitle title = chart.getTitle();
            title.setMaximumLinesToDisplay(2);
            title.setTextAlignment(HorizontalAlignment.CENTER);
            
            Font titleFont = title.getFont();
            String fontName = titleFont.getName();
            int fontStyle = titleFont.getStyle();
            
            Font newFont = new Font(fontName,fontStyle,12);
            title.setFont(newFont);
            
            // set formatting for range axis
            XYPlot xyPlot = (XYPlot)plot;
            NumberAxis rangeAxis = (NumberAxis)xyPlot.getRangeAxis();
            NumberFormat numberFormat = formatter.getNumberFormat(lowerCaseEventType,lowerCaseKey);
            
            if(numberFormat != null)
            	rangeAxis.setNumberFormatOverride(numberFormat);

            RangeType rangeType = RangeType.FULL;
            if(minDataValue != null && maxDataValue != null) {
                if(minDataValue >= 0.0 && maxDataValue > 0.0)
                    rangeType = RangeType.POSITIVE;
                else if(maxDataValue <= 0.0 && minDataValue < 0.0)
                    rangeType = RangeType.NEGATIVE;
            }
            rangeAxis.setRangeType(rangeType);
            rangeAxis.setAutoRangeStickyZero(false);

            TickUnits formatTickUnits = formatter.getTickUnits(lowerCaseEventType,lowerCaseKey);
            if(formatTickUnits != null)
            	rangeAxis.setStandardTickUnits(formatTickUnits);
            
            Double autoRangeMinimumSize = formatter.getAutoRangeMinimumSize(lowerCaseEventType,lowerCaseKey,formatTickUnits,minDataValue,maxDataValue);
            if(autoRangeMinimumSize != null)
            	rangeAxis.setAutoRangeMinimumSize(autoRangeMinimumSize);

        	plot.setBackgroundPaint(backgroundColor);
 
            response.setContentType("image/png");
            OutputStream os = response.getOutputStream();
            
            ChartUtilities.writeChartAsPNG(os,
                                           chart,
                                           width,
                                           height);
            
        } 
        
        incrementRequestCount(mbeanManager);
        updateRequestTotalMs(mbeanManager,System.currentTimeMillis() - startMs);
    }
    
    private void incrementRequestCount(ContextMbeanManager mbeanManager) {
        mbeanManager.incrementGraphRequestCount();
    }
    
    private void updateRequestTotalMs(ContextMbeanManager mbeanManager,long updateMs) {
    	mbeanManager.updateGraphRequestTotalMs(updateMs);
    }
}
