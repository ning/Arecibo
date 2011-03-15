package com.ning.arecibo.dashboard.data.v1;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;


import org.joda.time.DateTime;
import org.joda.time.Duration;
import com.ning.arecibo.dashboard.client.datahierarchy.results.DataHierarchyDataSpecResult;
import com.ning.arecibo.dashboard.client.datahierarchy.results.DataHierarchyPathComponentsResult;
import com.ning.arecibo.dashboard.client.datahierarchy.results.DataHierarchyResult;
import com.ning.arecibo.dashboard.client.datahierarchy.results.DataHierarchyResultComposite;
import com.ning.arecibo.dashboard.client.datahierarchy.serializers.DataHierarchyResultSerializer;
import com.ning.arecibo.dashboard.client.datahierarchy.types.DataHierarchyResultType;
import com.ning.arecibo.dashboard.client.datahierarchy.types.DataHierarchyTokenType;
import com.ning.arecibo.dashboard.client.datahierarchy.types.DataUrlDataType;
import com.ning.arecibo.dashboard.client.datahierarchy.types.DataUrlParamSpec;
import com.ning.arecibo.dashboard.client.datahierarchy.types.DataUrlSpec;
import com.ning.arecibo.dashboard.context.DashboardContextManager;
import com.ning.arecibo.dashboard.context.DashboardContextUtils;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAO;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAOException;
import com.ning.arecibo.dashboard.dao.ResolutionRequestType;
import com.ning.arecibo.dashboard.data.DataHierarchyHandler;
import com.ning.arecibo.dashboard.format.DashboardTableFormatter;
import com.ning.arecibo.dashboard.graph.DashboardGraphUtils;

public class V1DataHierarchyHandler implements DataHierarchyHandler {

    // assumed to be accessed in a single threaded context only
    DashboardContextManager contextManager = null;
    DashboardCollectorDAO collectorDAO = null;
    DashboardTableFormatter formatter = null;

    @Override
    public String getDataHierarchyResponse(HttpServletRequest request,List<String> tokens) {

        try {
            int tokenPos = 0;
            String rootToken = nextToken(tokenPos++,tokens);
            DataHierarchyResult result;

            // see if this is a root level request
            if(rootToken == null) {
                result = getRootNodeTokens();
            }
            else if(rootToken.equals(DataHierarchyTokenType.host.getSpecifier())) {
                result = getHostHierarchy(request,tokenPos,tokens,null,null);
            }
            else if(rootToken.equals(DataHierarchyTokenType.type.getSpecifier())) {
                result = getTypeHierarchy(request,tokenPos,tokens);
            }
            else {
                throw new IllegalStateException("Illegal token '" + rootToken  + "'");
            }

            return DataHierarchyResultSerializer.serializeHierarchyResultToJSON(result);
        }
        catch(DashboardCollectorDAOException dcdEx) {
            throw new RuntimeException(dcdEx);
        }
        catch(IOException ioEx) {
            throw new RuntimeException(ioEx);
        }
    }

    private void appendLine(StringBuilder sb,String line) {
        sb.append(line);
        sb.append("\n");
    }

    private String nextToken(int pos,List<String> list) {
        if(list.size() > pos)
            return list.get(pos);
        else
            return null;
    }

    private boolean hasMoreTokens(int pos,List<String> list) {
        if(list.size() <= pos)
            return false;
        else
            return true;
    }

    private void assertTokenReceived(String tokenString,DataHierarchyTokenType expectedTokenType) {
        if(!tokenString.equals(expectedTokenType.getSpecifier())) {
            throw new IllegalStateException("Illegal token '" + tokenString + "', expected '" + expectedTokenType.getSpecifier() + "'");
        }
    }

    private DashboardContextManager getContextManager(HttpServletRequest request) {
        if(contextManager == null)
           contextManager = new DashboardContextManager(request);
        
        return contextManager;
    }

    private DashboardCollectorDAO getCollectorDAO(HttpServletRequest request) {
        if(collectorDAO == null) {
            getContextManager(request);
            collectorDAO = contextManager.getCollectorDAO();
        }

        return collectorDAO;
    }

    private DashboardTableFormatter getFormatter(HttpServletRequest request) {
        if(formatter == null) {
            getContextManager(request);
            formatter = contextManager.getTableFormatter();
        }

        return formatter;
    }

    private List<String> formatEventTypeListAndSortUnique(HttpServletRequest request,List<String> list) {

        List<String> outList = new ArrayList<String>();
        for(String eventType:list) {
            String formattedEventType = getFormatter(request).getFormattedEventType(eventType);

            // (potentially non efficient way to detect uniques)
            if(!outList.contains(formattedEventType))
                outList.add(formattedEventType);
        }

        Collections.sort(outList);

        return outList;
    }

    private DataHierarchyResult getEventTypesResponse(HttpServletRequest request,DataHierarchyTokenType hierarchyType,
                                                String host,String type,String path)
            throws DashboardCollectorDAOException {

        // return list of event types
        List<String> eventTypes = null;

        if(hierarchyType.equals(DataHierarchyTokenType.host))
            eventTypes = getCollectorDAO(request).getLastEventTypesForHost(host);
        else if(hierarchyType.equals(DataHierarchyTokenType.type))
            eventTypes = getCollectorDAO(request).getLastEventTypesForType(type);
        else if(hierarchyType.equals(DataHierarchyTokenType.path))
            eventTypes = getCollectorDAO(request).getLastEventTypesForPathWithType(path,type);

        if(eventTypes == null)
            return null;

        eventTypes = formatEventTypeListAndSortUnique(request,eventTypes);

        DataHierarchyResult result = new DataHierarchyPathComponentsResult(DataHierarchyResultType.HIERARCHY_PATH_COMPONENTS, eventTypes);
        return result;
    }

    private DataHierarchyResult getAttributeTypesResponse(HttpServletRequest request,DataHierarchyTokenType hierarchyType,String eventType,
                                                String host,String type,String path)
            throws DashboardCollectorDAOException {

        // return list of attribute types
        String fullEventType = eventType + DataHierarchyEventSuffixByToken.getSuffixForTokenType(hierarchyType);
        Map<String, Map<String,Object>> events;

        if(hierarchyType.equals(DataHierarchyTokenType.host))
            events = getCollectorDAO(request).getLastValuesForHost(host,fullEventType);
        else if(hierarchyType.equals(DataHierarchyTokenType.type))
            events = getCollectorDAO(request).getLastValuesForType(type,fullEventType);
        else if(hierarchyType.equals(DataHierarchyTokenType.path))
            events = getCollectorDAO(request).getLastValuesForPathWithType(path,type,fullEventType);
        else
            events = new HashMap<String, Map<String,Object>>();

        List<String> attributeTypes = new ArrayList<String>();
        for(Map<String,Object> event:events.values()) {
            String attr = (String)event.get("attr");

            // make sure unique
            // (potentially non efficient way to detect uniques)
            if(!attributeTypes.contains(attr))
                attributeTypes.add(attr);
        }

        // sort and return
        Collections.sort(attributeTypes);

        DataHierarchyResult result = new DataHierarchyPathComponentsResult(DataHierarchyResultType.HIERARCHY_PATH_COMPONENTS, attributeTypes);
        return result;
    }

    private DataHierarchyResult getHostNamesResponse(HttpServletRequest request,String type,String path)
            throws DashboardCollectorDAOException {

        // return list of hosts
        List<String> hosts;
        if(type == null) {
            hosts = getCollectorDAO(request).getHostsOverall();
        }
        else if(path == null) {
            hosts = getCollectorDAO(request).getHostsForType(type);
        }
        else {
            hosts = getCollectorDAO(request).getHostsForPathWithType(path,type);
        }

        Collections.sort(hosts);

        DataHierarchyResult result = new DataHierarchyPathComponentsResult(DataHierarchyResultType.HIERARCHY_PATH_COMPONENTS, hosts);
        return result;
    }

    private DataHierarchyResult getTypesResponse(HttpServletRequest request)
            throws DashboardCollectorDAOException {

        // return list of types
        List<String> types = getCollectorDAO(request).getTypesOverall();
        Collections.sort(types);

        DataHierarchyResult result = new DataHierarchyPathComponentsResult(DataHierarchyResultType.HIERARCHY_PATH_COMPONENTS, types);
        return result;
    }

    private DataHierarchyResult getPathsResponse(HttpServletRequest request,String type)
            throws DashboardCollectorDAOException {

        // return list of paths
        List<String> paths = getCollectorDAO(request).getPathsForType(type);
        Collections.sort(paths);

        DataHierarchyResult result = new DataHierarchyPathComponentsResult(DataHierarchyResultType.HIERARCHY_PATH_COMPONENTS, paths);
        return result;
    }

    private DataHierarchyResult getRootNodeTokens() {

        ArrayList<String> list = new ArrayList<String>();
        list.add(DataHierarchyTokenType.type.getSpecifier());
        list.add(DataHierarchyTokenType.host.getSpecifier());

        DataHierarchyResult result = new DataHierarchyPathComponentsResult(DataHierarchyResultType.HIERARCHY_PATH_COMPONENTS, list);
        return result;
    }

    private DataHierarchyResult getPathNodeTokens() {

        ArrayList<String> list = new ArrayList<String>();
        list.add(DataHierarchyTokenType.host.getSpecifier());
        list.add(DataHierarchyTokenType.event.getSpecifier());

        DataHierarchyResult result = new DataHierarchyPathComponentsResult(DataHierarchyResultType.HIERARCHY_PATH_COMPONENTS, list);
        return result;
    }

    private DataHierarchyResult getTypeNodeTokens() {

        ArrayList<String> list = new ArrayList<String>();
        list.add(DataHierarchyTokenType.path.getSpecifier());
        list.add(DataHierarchyTokenType.host.getSpecifier());
        list.add(DataHierarchyTokenType.event.getSpecifier());

        DataHierarchyResult result = new DataHierarchyPathComponentsResult(DataHierarchyResultType.HIERARCHY_PATH_COMPONENTS, list);
        return result;
    }

    /*
    *
    * Host Hierarchy (possibly a sub hierarchy)
    *
    * allowed path (or sub-path):
    *
    * host : <hostName> : event : <eventType> : attribute : <attributeType> : end
    *
    */
    private DataHierarchyResult getHostHierarchy(HttpServletRequest request, int tokenPos, List<String> tokens,String type,String path)
            throws DashboardCollectorDAOException {

        // the 'host' will have already been parsed by the time we get here

        // if this is the last token, then the request is for the list of hostNames
        if(!hasMoreTokens(tokenPos,tokens)) {
            // return list of hostNames
            return getHostNamesResponse(request,type,path);
        }

        // treat next token as a hostName
        String host = nextToken(tokenPos++,tokens);

        if(!hasMoreTokens(tokenPos,tokens)) {
            // return next allowed token ('event')
            List<String> list = new ArrayList<String>();
            list.add(DataHierarchyTokenType.event.getSpecifier());
            DataHierarchyResult result = new DataHierarchyPathComponentsResult(DataHierarchyResultType.HIERARCHY_PATH_COMPONENTS, list);
            return result;
        }
        else {
            // make sure next token is 'event'
            assertTokenReceived(nextToken(tokenPos++,tokens),DataHierarchyTokenType.event);
        }
        return getEventSubHierarchy(request,tokenPos,tokens,host,type,path);
    }

    /*
    *
    * Type Hierarchy
    *
    * allowed paths:
    *
    * type : <typeName> : event : <eventName> : attribute : <attributeName> : end
    * type : <typeName> : path : <pathName> : event : <eventName> : attribute : <attributeName> : end
    * type : <typeName> : path : <pathName> : host : <hostName> : event : <eventName> : attribute : <attributeName> : end
    * type : <typeName> : host : <hostName> : event : <eventName> : attribute : <attributeName> : end
    *
    *
    */
    private DataHierarchyResult getTypeHierarchy(HttpServletRequest request, int tokenPos, List<String> tokens)
            throws DashboardCollectorDAOException {

        // the 'type' will have already been parsed by the time we get here

        // if this is the last token, then the request is for the list of typeNames
        if(!hasMoreTokens(tokenPos,tokens)) {
            // return list of typeNames
            return getTypesResponse(request);
        }

        // treat next token as a type
        String type = nextToken(tokenPos++,tokens);

        if(!hasMoreTokens(tokenPos,tokens)) {
            return getTypeNodeTokens();
        }
        else {
            String nextToken = nextToken(tokenPos++,tokens);
            if (nextToken.equals(DataHierarchyTokenType.host.getSpecifier())) {
                return getHostHierarchy(request, tokenPos, tokens,type,null);
            }
            else if (nextToken.equals(DataHierarchyTokenType.path.getSpecifier())) {
                return getPathSubHierarchy(request, tokenPos, tokens,type);
            }
            else if (nextToken.equals(DataHierarchyTokenType.event.getSpecifier())) {
                return getEventSubHierarchy(request, tokenPos, tokens,null,type,null);
            }
            else {
                throw new IllegalStateException("Illegal token '" + nextToken + "'");
            }
        }
    }

    /*
    * Path Sub Hierarchy
    *
    * allowed sub-paths:
    *
    * path : <pathName> : event : <eventType> : attribute : <attributeType> : end
    * path : <pathName> : host : <hostName> : event : <eventType> : attribute : <attributeType> : end
    */
    private DataHierarchyResult getPathSubHierarchy(HttpServletRequest request, int tokenPos, List<String> tokens, String type)
            throws DashboardCollectorDAOException {

        // the 'path' will have already been parsed by the time we get here

        // if this is the last token, then the request is for the list of paths
        if(!hasMoreTokens(tokenPos,tokens)) {
            // return list of paths
            return getPathsResponse(request,type);
        }

        // treat next token as a path
        String path = nextToken(tokenPos++,tokens);

        if(!hasMoreTokens(tokenPos,tokens)) {
            return getPathNodeTokens();
        }
        else {
            String nextToken = nextToken(tokenPos++,tokens);
            if (nextToken.equals(DataHierarchyTokenType.host.getSpecifier())) {
                return getHostHierarchy(request, tokenPos, tokens,type,path);
            }
            else if (nextToken.equals(DataHierarchyTokenType.event.getSpecifier())) {
                return getEventSubHierarchy(request, tokenPos, tokens,null,type,path);
            }
            else {
                throw new IllegalStateException("Illegal token '" + nextToken + "'");
            }
        }
    }

    /*
    * Event Sub Hierarchy
    *
    * allowed sub-path:
    *
    * event : <eventType> : attribute : <attributeType> : end
    */
    private DataHierarchyResult getEventSubHierarchy(HttpServletRequest request, int tokenPos,List<String> tokens, String host, String type, String path)
                throws DashboardCollectorDAOException {

        // the 'event' will have already been parsed by the time we get here

        // figure out root token type
        DataHierarchyTokenType rootToken;
        if(host != null) {
            rootToken = DataHierarchyTokenType.host;
        }
        else if(path != null && type != null) {
            rootToken = DataHierarchyTokenType.path;
        }
        else if(type != null) {
            rootToken = DataHierarchyTokenType.type;
        }
        else {
            throw new IllegalStateException("Need at least a host, path and/or type");
        }

        // if this is the last token, then the request is for the list of eventTypes
        if(!hasMoreTokens(tokenPos,tokens)) {
            // return list of event types
            return getEventTypesResponse(request,rootToken,host,type,path);
        }

        // treat next token as an event type
        String eventType = nextToken(tokenPos++,tokens);

        if(!hasMoreTokens(tokenPos,tokens)) {
            // return next allowed token ('attribute')
            ArrayList<String> list = new ArrayList<String>();
            list.add(DataHierarchyTokenType.attribute.getSpecifier());
            DataHierarchyResult result = new DataHierarchyPathComponentsResult(DataHierarchyResultType.HIERARCHY_PATH_COMPONENTS, list);
            return result;
        }
        else {
            // make sure next token is 'attribute'
            assertTokenReceived(nextToken(tokenPos++,tokens),DataHierarchyTokenType.attribute);
        }

        // if this is the last token, then the request is for the list of attributes
        if(!hasMoreTokens(tokenPos,tokens)) {
            // return list of attribute types
            return getAttributeTypesResponse(request,rootToken,eventType,host,type,path);
        }

        // treat next token as an event type
        String attributeType = nextToken(tokenPos++,tokens);

        if(!hasMoreTokens(tokenPos,tokens)) {
            // return next allowed token ('end')
            ArrayList<String> list = new ArrayList<String>();
            list.add(DataHierarchyTokenType.end.getSpecifier());
            DataHierarchyResult hierarchyResult = new DataHierarchyPathComponentsResult(DataHierarchyResultType.HIERARCHY_PATH_COMPONENTS, list);

            // also return the dataUrlSpec here, since we can
            DataUrlSpec urlSpec = getDataUrlSpec(request,rootToken,host,type,path,eventType,attributeType);
            DataHierarchyResult dataResult = new DataHierarchyDataSpecResult(urlSpec);

            // create a composite result
            DataHierarchyResultComposite compositeResult = new DataHierarchyResultComposite();
            compositeResult.addResult(hierarchyResult);
            compositeResult.addResult(dataResult);

            return compositeResult;
        }
        else {
            // make sure next token is 'end'
            assertTokenReceived(nextToken(tokenPos++,tokens),DataHierarchyTokenType.end);
        }

        // we are at the end:
        // return just the dataUrlSpec
        DataUrlSpec urlSpec = getDataUrlSpec(request,rootToken,host,type,path,eventType,attributeType);
        DataHierarchyResult result = new DataHierarchyDataSpecResult(urlSpec);
        return result;
    }

    private DataUrlSpec getDataUrlSpec(HttpServletRequest request,DataHierarchyTokenType rootToken,String host,String type,String path,String eventType,String attributeType) {

        // TODO: this is a bit of an inplace hack, should really place in DashboardContextUtils, etc.
        Map<String,String> params = new HashMap<String,String>();

        // add graphType param
        String graphType;
        if(rootToken.equals(DataHierarchyTokenType.type))
            graphType = DashboardGraphUtils.GraphType.BY_TYPE.toString();
        else if(rootToken.equals(DataHierarchyTokenType.path))
            graphType = DashboardGraphUtils.GraphType.BY_PATH_WITH_TYPE.toString();
        else if(rootToken.equals(DataHierarchyTokenType.host))
            graphType = DashboardGraphUtils.GraphType.BY_HOST.toString();
        else
            throw new IllegalStateException("Could not determine graphType");

        params.put("graphType",graphType);

        // add host
        if(host == null)
            params.put("host","undefined");
        else
            params.put("host",host);

        // add type
        if(type == null)
            params.put("type","undefined");
        else
            params.put("type",type);

        // add path
        if(path == null)
            params.put("path","undefined");
        else
            params.put("path",path);

        // add eventType
        params.put("eventType",eventType);

        // add attributeType
        params.put("attributeType",attributeType);

        String description = DashboardContextUtils.getGraphTitle(graphType,host,type,path,eventType,attributeType);


        // get the current host
        String dataHost = request.getServerName();

        // see if we're using a non-standard port
        int dataPort = request.getServerPort();
        if(dataPort != 80) {
            dataHost += ":" + dataPort;
        }

        // set the base data uri
        String dataPath = getContextManager(request).getContextPath() +  DashboardContextUtils.DASHBOARD_DATA_SERVLET_PATH;

        // create the base query params
        StringBuilder sb = new StringBuilder();
        DashboardContextUtils.appendParamsToUri(sb,params);
        String baseQueryParameterString = sb.toString();

        // create the optional param spec
        List<DataUrlParamSpec> optionalParams = new ArrayList<DataUrlParamSpec>();

        optionalParams.add(new DataUrlParamSpec("timeWindow",
                                                    DataUrlDataType.DURATION_OR_MILLIS,
                                                    new Duration(DashboardGraphUtils.TimeWindow.getDefaultTimeWindow().getMillis()),
                                                    false,
                                                    "time duration for data request (default is 1 hour)"));

        optionalParams.add(new DataUrlParamSpec("timeFrom",
                                                    DataUrlDataType.DATETIME_OR_MILLIS,
                                                    new DateTime(System.currentTimeMillis() - DashboardGraphUtils.TimeWindow.getDefaultTimeWindow().getMillis()),
                                                    false,
                                                    "start time for data request (default is 'now - timeWindow')"));

        optionalParams.add(new DataUrlParamSpec("resolutionRequest",
                                                    DataUrlDataType.RESOLUTION_REQUEST,
                                                    ResolutionRequestType.BEST_FIT.toString(),
                                                    false,
                                                    "resolution request type, can be one of: HIGHEST,HIGHESTAVAIL,LOWEST,LOWESTAVAIL,BEST_FIT"));

        optionalParams.add(new DataUrlParamSpec("maxTSPerGraph",
                                                    DataUrlDataType.INTEGER,
                                                    DashboardGraphUtils.DEFAULT_MAX_TIME_SERIES_PER_GRAPH,
                                                    false,
                                                    "maximum number of time series to include in returned data set"));

        DataUrlSpec dataUrlSpec = new DataUrlSpec(dataHost,dataPath,baseQueryParameterString,description,optionalParams);
        return dataUrlSpec;
    }
}
