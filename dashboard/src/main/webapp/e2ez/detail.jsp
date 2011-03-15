<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.joda.time.format.DateTimeFormat" %>
<%@ page import="org.joda.time.format.DateTimeFormatter" %>
<%@ page import="org.joda.time.DateTime" %>
<%@ page import="org.joda.time.Duration" %>
<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZConfigManager" %>
<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZStatusManager" %>
<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZMetric" %>
<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZMetricGroup" %>
<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZMetricStatus" %>
<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZNode" %>
<%@ page import="com.ning.arecibo.dashboard.context.DashboardContextManager" %>
<%@ page import="com.ning.arecibo.dashboard.context.DashboardContextUtils" %>
<%@ page import="com.ning.arecibo.dashboard.graph.DashboardGraphUtils" %>
<%
    DashboardContextManager contextManager = new DashboardContextManager(request);
    E2EZConfigManager configManager = contextManager.getE2EZConfigManager();
    E2EZStatusManager statusManager = contextManager.getE2EZStatusManager();

    String group = request.getParameter("group");
    E2EZMetricGroup metricGroup = configManager.getMetricGroup(group);
    List<E2EZNode> children;

    if(metricGroup != null)
        children = metricGroup.getChildren();
    else
        children = new ArrayList<E2EZNode>();

    DateTimeFormatter fmt = DateTimeFormat.forPattern("MMMM dd, YYYY");

    DateTime today = new DateTime().withMillisOfDay(0);
    boolean defaultToday;
    DateTime currDate;
    String currDateString = request.getParameter("date");

    DateTime nextDate = null;

    if(currDateString == null) {
        currDate = today;
        defaultToday = true;
    }
    else {
        currDate = new DateTime(currDateString).withMillisOfDay(0);
        if(currDate.getDayOfYear() >= today.getDayOfYear() &&
                currDate.getYear() >= today.getYear()) {

            defaultToday = true;
        }
        else {
            defaultToday = false;
            nextDate = currDate.plusDays(1);
        }
    }

    DateTime prevDate = currDate.minusDays(1);

    DateTime timeFrom = currDate.withTime(0,0,0,0);
    Duration timeWindow = Duration.standardDays(1L);


%>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
<title>End To End Zen Detailed Status</title>
<style type="text/css">
@import "/styles/ae.css";
</style>
<script src="/js/ae_detail.js" type="text/javascript"></script>    
</head>
<body>
<div class="g-doc-1024">
<div id="hd" class="g-section">
<div class="g-section g-tpl-50-50 g-split">
<div class="g-unit g-first">
</div>
<div class="g-unit" id="ae-userinfo">
</div>
</div>
<div id="ae-appbar-lrg">
<h1 class="g-section">
<div class="ae-page-title">
<%=metricGroup.getGroupName()%> Status
</div>
<div class="ae-return-link">
<a href="/e2ez/status.jsp">&lt;&lt; System Status</a>
</div>
</h1>
</div>
</div>
<div id="bd" class="g-section">
<div id="ae-trust-detail-nav-day">
<a id="ae-trust-detail-older" href="/e2ez/detail.jsp?date=<%=prevDate%>&group=<%=group%>" class="goog-menu-button goog-menu-button-prod goog-inline-block" title="Older"><img src="/images/ae_left_arrow.gif" width="12" height="9" alt="Older"></a>
<%
    if(defaultToday) {
%>
<a id="ae-next-disabledr" class="goog-menu-button goog-menu-button-prod goog-inline-block ae-disabled" title="Newer"><img src="/images/ae_right_arrow.gif" width="12" height="9" alt="Newer"></a>
<a id="ae-today-disabled" class="goog-menu-button goog-menu-button-prod goog-inline-block ae-disabled" title="Today">Today</a>
<%
    }
    else {
%>
<a id="ae-trust-detail-newer" href="/e2ez/detail.jsp?date=<%=nextDate%>&group=<%=group%>" class="goog-menu-button goog-menu-button-prod goog-inline-block" title="Newer"><img src="/images/ae_right_arrow.gif" width="12" height="9" alt="Newer"></a>
<a id="ae-trust-detail-today" href="/e2ez/detail.jsp?date=<%=today%>&group=<%=group%>" class="goog-menu-button goog-menu-button-prod goog-inline-block" title="Today">Today</a>
<%
    }
%>
<%=fmt.print(currDate)%>
</div>
<div id="ae-trust-detail-c">
<div id="ae-trust-detail" class="g-section ae-trust-detail-prod">
<div class="g-unit g-first">
<div id="ae-trust-detail-nav">
<ul>
<%

    if(children.size() == 0) {
%>
        No Metrics Configured
<%
    }
    else {

        String currSubHeading = "";
        for(E2EZNode child:children) {
            if(!(child instanceof E2EZMetric))
                continue;

            E2EZMetric metric = (E2EZMetric)child;
            String newSubHeading = metric.getSubHeading();
            if(newSubHeading != null && !newSubHeading.equals(currSubHeading)) {
                currSubHeading = newSubHeading;
%>
    <li>
        <strong><%=currSubHeading%></strong>
    </li>
<%
            }

            E2EZMetricStatus status;
            if(defaultToday)
                status = statusManager.getHistoricalMetricStatus(metric,currDate.getMillis(),System.currentTimeMillis());
            else
                status = statusManager.getHistoricalMetricStatus(metric,currDate.getMillis(),currDate.plusDays(1).getMillis());

            String anchorName = metric.getMetricName();
            String linkName = anchorName + "-link";

            HashMap<String,String> graphParams = new HashMap<String,String>();
            graphParams.put("eventType", metric.getEventType());
            graphParams.put("attributeType", metric.getAttributeType());
            graphParams.put("includeIndividualHosts", "false");
            graphParams.put("timeFrom", timeFrom.toString());
            graphParams.put("timeWindow", timeWindow.toString());
            graphParams.put("width", "700");
            graphParams.put("height", "290");

            if(metric.getQualifyingAttribute("deployedType") != null) {
                if(metric.getQualifyingAttribute("deployedConfigSubPath") != null) {
                    graphParams.put("path", metric.getQualifyingAttribute("deployedConfigSubPath"));
                    graphParams.put("graphType", DashboardGraphUtils.GraphType.BY_PATH_WITH_TYPE.toString());
                }
                else {
                    graphParams.put("graphType", DashboardGraphUtils.GraphType.BY_TYPE.toString());
                }
                graphParams.put("type", metric.getQualifyingAttribute("deployedType"));
            }

            if(status.equals(E2EZMetricStatus.CRITICAL)) {
                graphParams.put("alert","true");
            }
            else if(status.equals(E2EZMetricStatus.WARN)) {
                graphParams.put("bgColor","FFFFBB");
                graphParams.put("borderColor","FF8800");
            }

            String graphUrl = DashboardContextUtils.getGraphServletUrl(contextManager, graphParams);

            HashMap<String,String> sparklineParams = new HashMap<String,String>();
            sparklineParams.put("graphType", DashboardGraphUtils.GraphType.BY_TYPE.toString());
            sparklineParams.put("eventType", metric.getEventType());
            sparklineParams.put("attributeType", metric.getAttributeType());
            //sparklineParams.put("timeFrom", timeFrom.toString());
            //sparklineParams.put("timeWindow", timeWindow.toString());
            sparklineParams.put("width", "40");
            sparklineParams.put("height", "15");

            if(metric.getQualifyingAttribute("deployedType") != null) {
                if(metric.getQualifyingAttribute("deployedConfigSubPath") != null) {
                    sparklineParams.put("path", metric.getQualifyingAttribute("deployedConfigSubPath"));
                    sparklineParams.put("graphType", DashboardGraphUtils.GraphType.BY_PATH_WITH_TYPE.toString());
                }
                else {
                    sparklineParams.put("graphType", DashboardGraphUtils.GraphType.BY_TYPE.toString());
                }
                sparklineParams.put("type", metric.getQualifyingAttribute("deployedType"));
            }

            if(status.equals(E2EZMetricStatus.CRITICAL)) {
                sparklineParams.put("alert","true");
            }
            else if(status.equals(E2EZMetricStatus.WARN)) {
                sparklineParams.put("bgColor","FFFFBB");
                sparklineParams.put("borderColor","FF8800");
            }

            String sparklineUrl = DashboardContextUtils.getSparklineServletUrl(contextManager, sparklineParams);
%>
    <li>
        <input type="hidden" value="<%=graphUrl%>">
        <input type="hidden" value="">
        <input type="hidden" value="<%=metric.getDescription()%>">

        <a id="<%=linkName%>" href="#<%=anchorName%>"  title="<%=metric.getSubHeading() + ": " + metric.getDisplayName()%>">
            <img src="<%=sparklineUrl%>" alt="">
            <%=metric.getDisplayName()%>
        </a>
    </li>
<%
        }
    }
%>
</ul>
</div>
</div>
<div class="g-unit">
<div id="ae-trust-detail-chart-graph">
<h3 id="ae-trust-detail-chart-title"></h3>
<div id="ae-trust-detail-chart-description"></div>
<div id="ae-trust-detail-chart-img-c">
<img id="ae-trust-detail-chart-img" src="" width="700" height="290" alt="">
</div>
</div>
<div id="ae-trust-detail-table-c">
<table id="ae-trust-detail-table" class="ae-table" cellpadding="5" cellspacing="0">
<colgroup>
<col id="ae-trust-detail-time-col">
<col id="ae-trust-detail-probe-col">
<col>
</colgroup>
<thead>
<tr>
<th scope="col">
Time (US/Pacific)
</th>
<th scope="col">
Scope
</th>
<th scope="col">
Description
</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="3">No alerts for this day.</td>
</tr>
</tbody>
</table>
</div>
</div>
</div>
</div>
</div>
<script type="text/javascript"><!--
          ae.Trust.Detail.init();
        --></script>
<div id="ft">
</div>
<script type="text/javascript"><!--
  ae.init();
--></script>
</div>
</body>
</html>
