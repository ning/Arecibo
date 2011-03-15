<%@page import="java.util.*"%>
<%@page import="com.ning.arecibo.dashboard.table.*"%>
<%@page import="com.ning.arecibo.dashboard.relatedviews.*"%>
<%@page import="com.ning.arecibo.dashboard.graph.*"%>
<%@page import="com.ning.arecibo.dashboard.context.*"%>
<%
    DashboardTableContextManager tableContextManager = new DashboardTableContextManager(request);
    tableContextManager.initTableViewParams();

    List<String> hostsInContextList = tableContextManager.getHosts();
    List<String> pathsInContextList = tableContextManager.getPaths();
    List<String> groupingsInContextList = tableContextManager.getGroupings();
    
    boolean showHostsWithAggregates = tableContextManager.getShowHostsWithAggregates();
    boolean showAllDataValues = tableContextManager.getShowAllDataValues();
    boolean showGlobalZoneValues = tableContextManager.getShowGlobalZoneValues();
    String pageTitle = tableContextManager.getPageTitle();
%>
