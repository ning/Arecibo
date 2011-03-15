<%@page import="java.util.*"%>
<%@page import="java.net.*"%>
<%@page import="com.ning.arecibo.dashboard.*"%>
<%@page import="com.ning.arecibo.dashboard.graph.*"%>
<%@page import="com.ning.arecibo.dashboard.context.*"%>
<%@page import="com.ning.arecibo.dashboard.dao.*"%>
<%
    DashboardTableContextManager graphContextManager = new DashboardTableContextManager(request);

    String contextPath = graphContextManager.getContextPath();
    int[] reductionFactors = graphContextManager.getDataReductionFactors();

    Map<String,String> multipleGraphParams = DashboardContextUtils.getMultipleGraphParams(graphContextManager);
    String pageTitle = DashboardContextUtils.getMultipleGraphPageTitle(multipleGraphParams);
    String currSubTitle = DashboardContextUtils.getMultipleGraphPageSubTitle(multipleGraphParams);
    List<String> subTitles = DashboardContextUtils.getSubTitles(graphContextManager,multipleGraphParams);
    List<String> graphUrls = DashboardContextUtils.getGraphServletUrls(graphContextManager,multipleGraphParams);
    List<String> legendUrls = DashboardContextUtils.getLegendServletUrlsFromGraphUrls(graphContextManager,multipleGraphParams,graphUrls);
    List<String> dataUrls = DashboardContextUtils.getDataServletUrlsFromGraphUrls(graphContextManager,multipleGraphParams,graphUrls);
    List<String> alertToolTipStrings = DashboardContextUtils.getPopupAlertStatusStrings(graphContextManager,multipleGraphParams); 
    boolean hasAlerts = DashboardContextUtils.hasNonEmptyStringsInList(alertToolTipStrings);
    int maxTSPerGraph = DashboardContextUtils.getMaxTSPerGraph(multipleGraphParams);
    String resolutionRequest = DashboardContextUtils.getResolutionRequest(multipleGraphParams);
    
    int numCols = 2;
%>
