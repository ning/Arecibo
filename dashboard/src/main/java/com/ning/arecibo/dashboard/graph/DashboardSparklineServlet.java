package com.ning.arecibo.dashboard.graph;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Timestamp;
import java.awt.Color;
import java.awt.geom.Ellipse2D;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.ui.RectangleInsets;
import com.ning.arecibo.dashboard.context.ContextMbeanManager;
import com.ning.arecibo.dashboard.context.DashboardContextManager;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAO;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAOException;
import com.ning.arecibo.dashboard.dao.ResolutionRequest;
import com.ning.arecibo.dashboard.dao.ResolutionRequestType;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.dashboard.context.DashboardContextUtils.GLOBAL_ZONE_PATH;
import static com.ning.arecibo.dashboard.context.DashboardContextUtils.GLOBAL_ZONE_TYPE;
import static com.ning.arecibo.dashboard.graph.DashboardGraphUtils.*;

public class DashboardSparklineServlet extends HttpServlet
{
    private final static Logger log = Logger.getLogger(DashboardSparklineServlet.class);
 
    //TODO: These could be injected
    private final static Color DEFAULT_SPARKLINE_COLOR = new Color(0.8F,0.0F,0.0F);
    private final static Color DEFAULT_BACKGROUND_COLOR = new Color(1.0F,1.0F,1.0F);
    private final static Color ALERT_SPARKLINE_COLOR = new Color(0.8F,0.0F,0.0F);
    private final static Color ALERT_BACKGROUND_COLOR = new Color(0.9F,0.8F,0.8F);
    private final static Color ALERT_BORDER_COLOR = Color.RED;

    private final static double END_CIRCLE_SIZE = 4.0;
    private final static double HALF_END_CIRCLE_SIZE = END_CIRCLE_SIZE /2D;
    //private final static RectangleInsets INSETS = new RectangleInsets(2,2,2,2);
    private final static RectangleInsets INSETS = new RectangleInsets(1,1,1,1);

    
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
        
        long startMs = System.currentTimeMillis();
        
        DashboardContextManager contextManager = new DashboardContextManager(request);

        DashboardCollectorDAO collectorDAO = contextManager.getCollectorDAO();
        ContextMbeanManager mbeanManager = contextManager.getMbeanManager();
        
        String graphType = contextManager.getPrimaryParamValue("graphType");
        String eventType = contextManager.getPrimaryParamValue("eventType");
        String attributeType = contextManager.getPrimaryParamValue("attributeType");
        String timeWindowString = contextManager.getPrimaryParamValue("timeWindow");
        String timeFromString = contextManager.getPrimaryParamValue("timeFrom");
        String alert = contextManager.getPrimaryParamValue("alert");
        String warn = contextManager.getPrimaryParamValue("warn");
        String reductionOverride = contextManager.getPrimaryParamValue("fixedReduction");
        String widthString = contextManager.getPrimaryParamValue("width");
        String heightString = contextManager.getPrimaryParamValue("height");
        String bgColorString = contextManager.getPrimaryParamValue("bgColor");
        String borderColorString = contextManager.getPrimaryParamValue("borderColor");
        String sparklineColorString = contextManager.getPrimaryParamValue("sparklineColor");

        Long timeWindow = DashboardGraphUtils.parseDurationMillis(timeWindowString,TimeWindow.getDefaultTimeWindow().getMillis());
        Long timeFrom = DashboardGraphUtils.parseDateTimeMillis(timeFromString,null);

        if (attributeType == null) {
            // backwards compatibility
            attributeType = contextManager.getPrimaryParamValue("key");
        }

        Color sparklineColor;
        Color backgroundColor;
        Color borderColor;

        if (bgColorString != null)
            backgroundColor = new Color(Integer.parseInt(bgColorString, 16));
        else if (alert != null && alert.length() > 0)
            backgroundColor = ALERT_BACKGROUND_COLOR;
        else
            backgroundColor = DEFAULT_BACKGROUND_COLOR;

        if (borderColorString != null)
            borderColor = new Color(Integer.parseInt(borderColorString, 16));
        else if (alert != null && alert.length() > 0)
            borderColor = ALERT_BORDER_COLOR;
        else
            borderColor = null;

        if(sparklineColorString != null)
            sparklineColor = new Color(Integer.parseInt(sparklineColorString, 16));
        else if(alert != null && alert.length() > 0)
            sparklineColor = ALERT_SPARKLINE_COLOR;
        else
            sparklineColor = DEFAULT_SPARKLINE_COLOR;

        int width;
        if (widthString == null) {
            width = DEFAULT_SPARKLINE_WIDTH;
        }
        else {
            width = Integer.parseInt(widthString);
        }

        int height;
        if (heightString == null) {
            height = DEFAULT_SPARKLINE_HEIGHT;
        }
        else {
            height = Integer.parseInt(heightString);
        }

        ResolutionRequest resolutionRequest;
        if(reductionOverride != null) {
        	int reduction = Integer.parseInt(reductionOverride);
        	resolutionRequest = new ResolutionRequest(ResolutionRequestType.FIXED,reduction);
        }
        else
        	resolutionRequest = new ResolutionRequest(ResolutionRequestType.BEST_FIT,DashboardGraphUtils.DEFAULT_SPARKLINE_REFERENCE_RESOLUTION_WIDTH);
        
        List<Map<String,Object>> valueMaps = null;
        
        try {
            if(graphType == null || graphType.length() == 0)
                throw new IllegalStateException("graphType parameter must be provided");
            
            if(graphType.equals(DashboardGraphUtils.GraphType.BY_HOST.toString())) {
            
                String host = contextManager.getPrimaryParamValue("host");
                if(host == null || host.length() == 0)
                    throw new IllegalStateException("missing host param with graphType = " + graphType);
                
                valueMaps = collectorDAO.getValuesForHostEvent(host,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
            }
            else if(graphType.equals(DashboardGraphUtils.GraphType.BY_PATH_WITH_TYPE.toString())) {
            
                String path = contextManager.getPrimaryParamValue("path");
                if(path == null || path.length() == 0)
                    throw new IllegalStateException("missing path param with graphType = " + graphType);
                
                String type = contextManager.getPrimaryParamValue("type");
                if(type == null || type.length() == 0)
                    throw new IllegalStateException("missing type param with graphType = " + graphType);
                
                valueMaps = collectorDAO.getValuesForPathWithTypeEvent(path,type,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
            }
            else if(graphType.equals(DashboardGraphUtils.GraphType.BY_TYPE.toString())) {
            
                String type = contextManager.getPrimaryParamValue("type");
                if(type == null || type.length() == 0)
                    throw new IllegalStateException("missing type param with graphType = " + graphType);
                
                valueMaps = collectorDAO.getValuesForTypeEvent(type,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
            }
            else if(graphType.equals(DashboardGraphUtils.GraphType.BY_GLOBAL_ZONE_LIST_FOR_PATH_WITH_TYPE.toString())) {
            	// for now just treat it as a global zone byPathWithType
                String path = GLOBAL_ZONE_PATH;
                String type = GLOBAL_ZONE_TYPE;
                
                valueMaps = collectorDAO.getValuesForPathWithTypeEvent(path,type,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
            }
            else if(graphType.equals(DashboardGraphUtils.GraphType.BY_GLOBAL_ZONE_LIST_FOR_TYPE.toString())) {
            	// for now just treat it as a global zone byType
                String type = GLOBAL_ZONE_TYPE;
            	
                valueMaps = collectorDAO.getValuesForTypeEvent(type,eventType, attributeType,resolutionRequest,timeWindow,timeFrom);
            }
            else {
                throw new IllegalStateException("Unrecognized graphType = '" + graphType + "'");
            }
        }
        catch(DashboardCollectorDAOException ddEx) {
            log.warn(ddEx);
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx);
        }
        
        TimeSeriesCollection dataset = null;
        dataset = new TimeSeriesCollection();
        
        Double minNum = null;
        Double maxNum = null;
        if(valueMaps != null && valueMaps.size() > 0) {
            // Create a time series chart
            TimeSeries values = new TimeSeries("", Millisecond.class);
            
            Timestamp maxTs = null;
            Number lastNum = null;
    
            for(Map<String,Object> valueMap:valueMaps) {
                Timestamp ts = (Timestamp)valueMap.get("ts");
                Number num = (Number)valueMap.get("value");
            
                values.addOrUpdate(new Millisecond(ts),num);
                
                if(maxTs == null || maxTs.getTime() < ts.getTime()) {
                    maxTs = ts;
                    lastNum = num;
                }
                
                double numDouble = num.doubleValue();
                if(minNum == null || minNum > numDouble)
                	minNum = numDouble;
                if(maxNum == null || maxNum < numDouble)
                	maxNum = numDouble;
            }
    
            // add null at beginning, to establish start time range
            Timestamp timeFromTs;
            if(timeFrom != null)
                timeFromTs = new Timestamp(timeFrom);
            else
                timeFromTs = new Timestamp(System.currentTimeMillis() - timeWindow);
            values.addOrUpdate(new Millisecond(timeFromTs), null);
            
            // add null at end to establish end time range
            Timestamp timeToTs;
            if(timeFrom != null)
                timeToTs = new Timestamp(timeFrom + timeWindow);
            else
                timeToTs = new Timestamp(System.currentTimeMillis());
            values.addOrUpdate(new Millisecond(timeToTs), null); 
            
            dataset.addSeries(values);
            
            // Add another series with just the max value, so can render a dot
            TimeSeries lastValueSeries = new TimeSeries("",Millisecond.class);
            if(maxTs != null)
                lastValueSeries.add(new Millisecond(maxTs),lastNum);
            
            dataset.addSeries(lastValueSeries);
        } 
        
        DateAxis x = new DateAxis();
        //x.setTickUnit(new DateTickUnit(DateTickUnit.MONTH, 1));
        x.setTickLabelsVisible(false);
        x.setTickMarksVisible(false);
        x.setAxisLineVisible(false);
        x.setNegativeArrowVisible(false);
        x.setPositiveArrowVisible(false);
        x.setVisible(false);

        NumberAxis y = new NumberAxis();
        y.setAutoRangeIncludesZero(false);
        y.setAutoRangeStickyZero(false);
        y.setTickLabelsVisible(false);
        y.setTickMarksVisible(false);
        y.setAxisLineVisible(false);
        y.setNegativeArrowVisible(false);
        y.setPositiveArrowVisible(false);
        y.setVisible(false);
        
        // fix weird bug where some memory sparklines aren't centered
        // only do this for flat line constant graphs, others get screwed up
        if(minNum != null && maxNum != null && minNum.equals(maxNum)) {
        	y.centerRange(minNum);
        }
        	
        	
        
        XYPlot plot = new XYPlot();
        plot.setInsets(INSETS);
        plot.setDataset(dataset);
        plot.setDomainAxis(x);
        plot.setDomainGridlinesVisible(false);
        plot.setDomainCrosshairVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setRangeCrosshairVisible(false);
        plot.setRangeAxis(y);
        
        plot.setBackgroundPaint(backgroundColor);
        
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0,true);
        renderer.setSeriesShapesVisible(0,false);
        renderer.setSeriesPaint(0,sparklineColor);
        
        renderer.setSeriesShapesVisible(1,true);
        renderer.setSeriesOutlinePaint(1,sparklineColor);
        renderer.setSeriesFillPaint(1,Color.black);
        renderer.setSeriesShapesFilled(1,true);
        renderer.setSeriesShape(1, new Ellipse2D.Double(-HALF_END_CIRCLE_SIZE,
                                                        -HALF_END_CIRCLE_SIZE,
                                                        END_CIRCLE_SIZE, 
                                                        END_CIRCLE_SIZE)); 
        
        renderer.setUseOutlinePaint(true);
        renderer.setUseFillPaint(true);
        
        plot.setRenderer(renderer);

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT,
                plot, false);
        
        if(borderColor != null) {
            chart.setBorderVisible(true);
        	chart.setBorderPaint(borderColor);
        }
        else {
            chart.setBorderVisible(false);
        }

        if(chart != null) {
            response.setContentType("image/png");
            OutputStream os = response.getOutputStream();
            
            ChartUtilities.writeChartAsPNG(os, chart, width, height);
        } 
        else {
            response.setContentType("text/html");
        	Writer writer = response.getWriter();
        	writer.write("No chart generated");
        }
        
        incrementRequestCount(mbeanManager);
        updateRequestTotalMs(mbeanManager,System.currentTimeMillis() - startMs);
    }
    
    private void incrementRequestCount(ContextMbeanManager mbeanManager) {
        mbeanManager.incrementSparklineRequestCount();
    }
    
    private void updateRequestTotalMs(ContextMbeanManager mbeanManager,long updateMs) {
    	mbeanManager.updateSparklineRequestTotalMs(updateMs);
    }
}
