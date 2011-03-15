<%@ page import="java.util.List" %>
<%@ page import="org.joda.time.DateTime" %>
<%@ page import="org.joda.time.format.DateTimeFormatter" %>
<%@ page import="org.joda.time.format.DateTimeFormat" %>
<%@ page import="com.ning.arecibo.dashboard.context.DashboardContextManager" %>
<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZConfigManager" %>
<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZStatusManager" %>
<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZMetricGroupCategory" %>
<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZMetricGroup" %>
<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZMetricStatus" %>
<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZNode" %>
<%
    DashboardContextManager contextManager = new DashboardContextManager(request);
    E2EZConfigManager configManager = contextManager.getE2EZConfigManager();
    E2EZStatusManager statusManager = contextManager.getE2EZStatusManager();
    List<E2EZMetricGroupCategory> categories = configManager.getMetricGroupCategories();

    DateTimeFormatter fmt = DateTimeFormat.forPattern("MM/dd/YY");

    boolean defaultToday;
    DateTime today = new DateTime().withMillisOfDay(0);
    DateTime endDate;
    String endDateString = request.getParameter("endDate");

    DateTime nextEndDate = null;

    if(endDateString == null) {
        endDate = today;
        defaultToday = true;
    }
    else {
        endDate = new DateTime(endDateString).withMillisOfDay(0);
        if(endDate.getDayOfYear() >= today.getDayOfYear() &&
                endDate.getYear() >= today.getYear()) {

            defaultToday = true;
        }
        else {
            defaultToday = false;
            nextEndDate = endDate.plusDays(7);
        }
    }

    DateTime prevEndDate = endDate.minusDays(7);

%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
<title>End To End Zen Status</title>
<style type="text/css">
@import "/styles/ae.css";
</style>
</head>
<body>
<div id="ae-trust" class="g-doc-800">
<div id="hd" class="g-section">
<div class="g-section g-tpl-50-50 g-split">
<div class="g-unit g-first">
</div>
<div class="g-unit" id="ae-userinfo">
</div>
</div>
<div id="ae-appbar-lrg">
<h1>System Status</h1>
</div<%@ page import="com.ning.arecibo.dashboard.alert.e2ez.E2EZStatusManager" %>>
</div>
<div id="bd" class="g-section">
<div id="alerts">
<div class="ae-trust-alert ae-trust-daily-offline-prod">
<strong>
Increased Latency and Error Rate
&ndash;
</strong>
<p class="ae-trust-alert-info">
Dec 5 2009, 07:20 AM
- Dec 5 2009, 07:45 AM
<br>
posted by Joe Ops
</p>
<p>
User Sign-ups saw increased latency.  This issue has now been resolved.
</p>
</div>
</div>
<table id="ae-trust-status-table" cellpadding="0" cellspacing="0" class="ae-table ae-table-striped">
<thead>
<tr>
<th class="ae-button-th">
<a href="status.jsp?endDate=<%=prevEndDate.toString()%>" class="goog-menu-button goog-menu-button-prod goog-inline-block" title="Older"><img src="/images/ae_left_arrow.gif" width="12" height="9" alt="Older"></a>
<%
    if(defaultToday) {
%>
<a id="ae-next-disabled" class="goog-menu-button goog-menu-button-prod goog-inline-block ae-disabled" title="Newer"><img src="/images/ae_right_arrow.gif" width="12" height="9" alt="Newer"></a>
<%
    }
    else {
%>
    <a href="status.jsp?endDate=<%=nextEndDate.toString()%>" class="goog-menu-button goog-menu-button-prod goog-inline-block" title="Newer"><img src="/images/ae_right_arrow.gif" width="12" height="9" alt="Newer"></a>
<%
    }
%>
</th>
<%
    for(int i=7;i>=0;i--) {
%>
<th scope="col" class="ae-nowrap">
<%
        if(i == 1 && defaultToday) {
%>
    Yesterday
<%
        }
        else if(i == 0 && defaultToday) {
%>
    Today
<%
        }
        else {
%>
<%=fmt.print(endDate.minusDays(i))%>
<%
        }
    }
%>
</th>
<th scope="col" class="ae-nowrap">
Now
</th>
</tr>
</thead>
<tbody>
<%
    if(categories != null) {
        for(E2EZMetricGroupCategory category:categories) {
%>

<tr class="ae-trust-status-section">
<td colspan="10">
<%=category.getCategoryName()%>
</td>
</tr>
<%
            for(E2EZNode child:category.getChildren()) {
                E2EZMetricGroup group;
                if(child instanceof E2EZMetricGroup) {
                    group = (E2EZMetricGroup)child;
                }
                else
                    continue;

%>
<tr>
<th scope="row">
<h3>
<%=group.getGroupName()%>
</h3>
</th>
<%
                E2EZMetricStatus nowStatus = statusManager.getCurrentMetricGroupStatus(group);
                for(int i=7;i>=0;i--) {
                    E2EZMetricStatus status;
                    if(i==0 && defaultToday)
                        status = statusManager.getHistoricalMetricGroupStatus(group,endDate.getMillis(),System.currentTimeMillis());
                    else
                        status = statusManager.getHistoricalMetricGroupStatus(group,endDate.minusDays(i).getMillis(),endDate.minusDays(i-1).getMillis());

                    if(status.equals(E2EZMetricStatus.OK)) {
%>
<td>
<a
                        href="/e2ez/detail.jsp?group=<%=group.getGroupName()%>&date=<%=endDate.minusDays(i)%>"
                        title="Online"
                        class="ae-ir ae-trust-daily-icon ae-trust-daily-online-prod"></a>
</td>
<%
                    }
                    else if(status.equals(E2EZMetricStatus.WARN)) {
%>
<td>
<a
                        href="/e2ez/detail.jsp?group=<%=group.getGroupName()%>&date=<%=endDate.minusDays(i)%>"
                        title="Warn"
                        class="ae-ir ae-trust-daily-icon ae-trust-daily-warn-prod"></a>
</td>
<%
                    }
                    else if(status.equals(E2EZMetricStatus.CRITICAL)) {
%>
<td>
<a
                        href="/e2ez/detail.jsp?group=<%=group.getGroupName()%>&date=<%=endDate.minusDays(i)%>"
                        title="Critical"
                        class="ae-ir ae-trust-daily-icon ae-trust-daily-critical-prod"></a>
</td>
<%
                    }
                    else if(status.equals(E2EZMetricStatus.INVESTIGATING)) {
%>
<td>
<a
                        href="/e2ez/detail.jsp?group=<%=group.getGroupName()%>&date=<%=endDate.minusDays(i)%>"
                        title="Investigating"
                        class="ae-ir ae-trust-daily-icon ae-trust-daily-investigating-prod"></a>
</td>
<%
                    }
                    else /*if(status.equals(E2EZMetricStatus.UNKNOWN)) */{
%>
<td>
<a
                        href="/e2ez/detail.jsp?group=<%=group.getGroupName()%>&date=<%=endDate.minusDays(i)%>"
                        title="Unknown"
                        class="ae-ir ae-trust-daily-icon ae-trust-daily-unknown-prod"></a>
</td>
<%
                    }
                }
%>
<td class="ae-trust-col-now-prod">
<a
                      href="/e2ez/detail.jsp?group=<%=group.getGroupName()%>&date=<%=endDate%>"
                      id="<%=nowStatus.toString()%>"
                      class="ae-status-<%=nowStatus.toString().toLowerCase()%>"
                      ><%=nowStatus.toString()%>
</a>
</td>
</tr>
<%
            }
        }
    }
%>
</tbody>
</table>
<div class="ae-trust-legend">
<p>The following symbols signify the most severe issue (if any) encountered during that day. Click a symbol in the table above to view a day's performance graphs.</p>
<p>
<img src="/images/ae_status_good.gif" width="16" height="15" alt="no issues"> <span>No known issues</span>
<img src="/images/ae_status_warn.gif" width="16" height="15" alt="warning"> <span>Warning of possible issues</span>
<img src="/images/ae_status_investigating.gif" width="16" height="15" alt="investigating"> <span>Investigating</span>
<img src="/images/ae_status_alert.gif" width="16" height="15" alt="service disruption"> <span>Service disruption</span>
<img src="/images/ae_status_unknown.gif" width="16" height="15" alt="unknown"> <span>Unknown</span>
</p>
</div>
</div>
</div>
</div>
</div>
</body>
</html>
