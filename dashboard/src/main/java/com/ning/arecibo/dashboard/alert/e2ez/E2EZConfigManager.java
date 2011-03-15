package com.ning.arecibo.dashboard.alert.e2ez;

import java.io.Reader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;

import com.ning.arecibo.util.Logger;

public class E2EZConfigManager implements Runnable {
    private final static Logger log = Logger.getLogger(E2EZConfigManager.class);

    private final static String CONFIG_FILE_NAME = "e2ez_alerts_dashboard.json";

    // constants from the config file
    private final static String METRIC_CATEGORIES = "metricCategories";
    private final static String CATEGORY_NAME = "categoryName";
    private final static String CHILDREN = "children";
    private final static String METRIC_GROUPS = "metricGroups";
    private final static String GROUP_NAME = "groupName";
    private final static String WARN_THRESHOLD = "warnThreshold";
    private final static String CRITICAL_THRESHOLD = "criticalThreshold";
    private final static String METRICS = "metrics";
    private final static String METRIC_NAME = "metricName";
    private final static String SUB_HEADING = "subHeading";
    private final static String DISPLAY_NAME = "displayName";
    private final static String EVENT_TYPE = "eventType";
    private final static String ATTRIBUTE_TYPE = "attributeType";
    private final static String DESCRIPTION = "description";
    private final static String QUALIFYING_ATTRIBUTES = "qualifyingAttributes";



    // thresholds for the minimum number of alerts within a hierarchy to trigger the top-level
    private final static int DEFAULT_WARN_THRESHOLD = 1;
    private final static int DEFAULT_CRITICAL_THRESHOLD = 1;

    //TODO: Inject this
    // poll every 5 mins (will want to inject this later, or change to happen on demand, with minimum refresh interval)
    private final static long CONFIG_UPDATE_INTERVAL = TimeUnit.MINUTES.toMillis(5);

    private volatile List<E2EZMetricGroupCategory> currMetricGroupCategoryList = null;
    private volatile Map<String,E2EZMetricGroup> currMetricGroupMap = null;
    private volatile Map<String,E2EZMetric> currMetricMap = null;

    private volatile ScheduledThreadPoolExecutor executor;

    public synchronized void start() {

        // one thread should be fine
        this.executor = new ScheduledThreadPoolExecutor(1);

        // start the config updater
        this.executor.scheduleAtFixedRate(this, 0, CONFIG_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (this.executor != null) {
            this.executor.shutdown();
            this.executor = null;
        }
    }

    public void run() {
        try {
            log.info("Updating E2EZ configuration");
            ClassLoader cLoader = Thread.currentThread().getContextClassLoader();

            InputStream configStream = cLoader.getResourceAsStream(CONFIG_FILE_NAME);
            InputStreamReader configReader = new InputStreamReader(configStream);

            parseConfigsFromJSON(configReader);
            log.info("Loaded E2EZ configuration");
        }
        catch(Exception ex) {
            log.warn(ex);
            // don't throw an exception out of Runnable
        }
    }

    private void parseConfigsFromJSON(Reader inReader) throws E2EZConfigException
    {

        JsonParser parser = null;
        try {
            List<E2EZMetricGroupCategory> metricGroupCategoryList = new ArrayList<E2EZMetricGroupCategory>();
            Map<String,E2EZMetricGroupCategory> metricGroupCategoryMap = new HashMap<String, E2EZMetricGroupCategory>();
            Map<String,E2EZMetricGroup> metricGroupMap = new HashMap<String,E2EZMetricGroup>();
            Map<String,E2EZMetric> metricMap = new HashMap<String,E2EZMetric>();

            parser = new MappingJsonFactory().createJsonParser(inReader);
            parser.enable(JsonParser.Feature.ALLOW_COMMENTS);
            parser.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
            parser.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

            JsonNode rootNode = parser.readValueAsTree();

            // read in metrics
            JsonNode metricsNode = rootNode.path(METRICS);
            if(metricsNode != null && !metricsNode.isMissingNode() && metricsNode.size() > 0) {

                Iterator<JsonNode> metricNodes = metricsNode.getElements();
                while(metricNodes.hasNext()) {

                    JsonNode metricNode = metricNodes.next();

                    String metricName = null;
                    String subHeading = null;
                    String displayName = null;
                    String eventType = null;
                    String attributeType = null;
                    String description = null;

                    JsonNode fieldNode = metricNode.get(METRIC_NAME);
                    if(fieldNode != null && !fieldNode.isMissingNode()) {
                        metricName = fieldNode.getValueAsText();
                    }

                    fieldNode = metricNode.get(SUB_HEADING);
                    if(fieldNode != null && !fieldNode.isMissingNode()) {
                        subHeading = fieldNode.getValueAsText();
                    }

                    fieldNode = metricNode.get(DISPLAY_NAME);
                    if(fieldNode != null && !fieldNode.isMissingNode()) {
                        displayName = fieldNode.getValueAsText();
                    }

                    fieldNode = metricNode.get(EVENT_TYPE);
                    if(fieldNode != null && !fieldNode.isMissingNode()) {
                        eventType = fieldNode.getValueAsText();
                    }

                    fieldNode = metricNode.get(ATTRIBUTE_TYPE);
                    if(fieldNode != null && !fieldNode.isMissingNode()) {
                        attributeType = fieldNode.getValueAsText();
                    }

                    fieldNode = metricNode.get(DESCRIPTION);
                    if(fieldNode != null && !fieldNode.isMissingNode()) {
                        description = fieldNode.getValueAsText();
                    }

                    E2EZMetric metric = new E2EZMetric(metricName,subHeading,displayName,eventType,attributeType,description);

                    JsonNode qAttsNode = metricNode.get(QUALIFYING_ATTRIBUTES);
                    if(qAttsNode != null && !qAttsNode.isMissingNode()) {

                        Iterator<String> qAtts = qAttsNode.getFieldNames();
                        while(qAtts.hasNext()) {

                            String qAtt = qAtts.next();
                            JsonNode qAttNode = qAttsNode.get(qAtt);
                            String qAttVal = qAttNode.getValueAsText();

                            metric.addQualifyingAttribute(qAtt,qAttVal);
                        }
                    }

                    metricMap.put(metricName,metric);
                }
            }


            // read in metricGroups (dereferencing previously parsed metrics)
            JsonNode groupsNode = rootNode.path(METRIC_GROUPS);
            if(groupsNode != null && !groupsNode.isMissingNode()) {
                Iterator<JsonNode> groupsNodes = groupsNode.getElements();

                while(groupsNodes.hasNext()) {
                    JsonNode groupNode = groupsNodes.next();

                    String groupName = null;
                    int warnThreshold = DEFAULT_WARN_THRESHOLD;
                    int criticalThreshold = DEFAULT_CRITICAL_THRESHOLD;

                    JsonNode fieldNode = groupNode.get(GROUP_NAME);
                    if(fieldNode != null && !fieldNode.isMissingNode()) {
                        groupName = fieldNode.getValueAsText();
                    }

                    fieldNode = groupNode.get(WARN_THRESHOLD);
                    if(fieldNode != null && !fieldNode.isMissingNode()) {
                        warnThreshold = fieldNode.getIntValue();
                    }

                    fieldNode = groupNode.get(CRITICAL_THRESHOLD);
                    if(fieldNode != null && !fieldNode.isMissingNode()) {
                        criticalThreshold = fieldNode.getIntValue();
                    }

                    E2EZMetricGroup metricGroup = new E2EZMetricGroup(groupName,warnThreshold,criticalThreshold);

                    JsonNode childrenNode = groupNode.get(CHILDREN);
                    if(childrenNode != null && !childrenNode.isMissingNode()) {

                        Iterator<JsonNode> childrenNodes = childrenNode.getElements();
                        while(childrenNodes.hasNext()) {
                            JsonNode child = childrenNodes.next();
                            String childName = child.getValueAsText();

                            // see if this is a valid metric name
                            E2EZNode e2ezNode = metricMap.get(childName);
                            if(e2ezNode == null) {
                                // see if this is a valid metricGroup (note order dependence in loading)
                                e2ezNode = metricGroupMap.get(childName);
                            }

                            if(e2ezNode == null) {
                                throw new E2EZConfigException(String.format("Found unresolved child reference '%s' for metricGroup '%s'",childName,groupName));
                            }
                            metricGroup.addChildNode(e2ezNode);
                        }
                    }

                    metricGroupMap.put(groupName,metricGroup);
                }
            }


            // read in metricCategories (dereferencing previously parsed metricGroups)
            JsonNode categoriesNode = rootNode.path(METRIC_CATEGORIES);
            if(categoriesNode != null && !categoriesNode.isMissingNode()) {
                Iterator<JsonNode> categoriesNodes = categoriesNode.getElements();

                while(categoriesNodes.hasNext()) {
                    JsonNode categoryNode = categoriesNodes.next();

                    String categoryName = null;

                    JsonNode fieldNode = categoryNode.get(CATEGORY_NAME);
                    if(fieldNode != null && !fieldNode.isMissingNode()) {
                        categoryName = fieldNode.getValueAsText();
                    }

                    E2EZMetricGroupCategory metricGroupCategory = new E2EZMetricGroupCategory(categoryName);

                    JsonNode childrenNode = categoryNode.get(CHILDREN);
                    if(childrenNode != null && !childrenNode.isMissingNode()) {

                        Iterator<JsonNode> childrenNodes = childrenNode.getElements();
                        while(childrenNodes.hasNext()) {
                            JsonNode child = childrenNodes.next();
                            String childName = child.getValueAsText();

                            // see if this is a valid metric group name
                            E2EZNode e2ezNode = metricGroupMap.get(childName);

                            if(e2ezNode == null) {
                                throw new E2EZConfigException(String.format("Found unresolved child reference '%s' for metricGroupCategory '%s'",childName,categoryName));
                            }
                            metricGroupCategory.addChildNode(e2ezNode);
                        }
                    }

                    metricGroupCategoryMap.put(categoryName, metricGroupCategory);
                    metricGroupCategoryList.add(metricGroupCategory);
                }
            }

            // update current config data with latest parse result
            synchronized(this) {
                currMetricGroupCategoryList = metricGroupCategoryList;
                currMetricGroupMap = metricGroupMap;
                currMetricMap = metricMap;
            }
        }
        catch(IOException ioEx) {
            log.warn(ioEx);
            throw new E2EZConfigException("problem creating json parser from inputStream",ioEx);
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx);
            throw new E2EZConfigException("problem parsing json config from inputStream",ruEx);
        }
        finally {
            try {
                if(parser != null)
                    parser.close();
                if(inReader != null)
                    inReader.close();
            }
            catch(IOException ioEx) {
                log.info(ioEx,"IOException:");
                // return ok here
            }
        }
	}

    public synchronized List<E2EZMetricGroupCategory> getMetricGroupCategories() {
        return currMetricGroupCategoryList;
    }

    public synchronized E2EZMetricGroup getMetricGroup(String groupName) {
        if(currMetricGroupMap == null)
            return null;
        
        return currMetricGroupMap.get(groupName);
    }

    public synchronized E2EZMetric getMetric(String metricName) {
        if(currMetricMap == null)
            return null;

        return currMetricMap.get(metricName);
    }
}
