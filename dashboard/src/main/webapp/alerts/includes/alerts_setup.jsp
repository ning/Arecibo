<%@page import="java.util.*"%>
<%@page import="com.ning.arecibo.dashboard.context.*"%>
<%@page import="com.ning.arecibo.dashboard.alert.*"%>
<%@page import="com.ning.arecibo.dashboard.graph.*"%>

<%
    DashboardContextManager alertContextManager = new DashboardContextManager(request);

    String dashboardUrl = DashboardContextUtils.getDashboardPageUri(alertContextManager);
    String contextPath = alertContextManager.getContextPath();
    String formattedAlertSummaryList = DashboardContextUtils.getHtmlAlertStatusSummaryString(alertContextManager);
    List<DashboardAlertStatus> alertStatii = DashboardContextUtils.getActiveAlertList(alertContextManager);

    String pageTitle = "Currently Active Alerts";
%>
