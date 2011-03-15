<%@include file="includes/alerts_setup.jsp" %>
<%@include file="../global_includes/header.jsp" %>


<script src="../../js/jquery-1.2.6.min.js"></script>
<script src="../../js/jquery.dataTables.min.js"></script>
<script src="../../js/wz_tooltip.js"></script>


<script>
	$(document).ready(function() {
		$('#alertDetailsTable').dataTable();
	} );	
</script>

<table border="1" cellspacing="2" cellpadding="4" bgcolor="DarkSlateGray">
<tr><td colspan="11" style="background:DarkGreen;"><font color="FFFFFF"><b>Alert Summaries</b></font></td></tr>
<tr><td style="background:#CCFFCC;text-align:left"><%=formattedAlertSummaryList%> </td></tr>
</table>

<br>
<br>

<table border="1" cellspacing="2" cellpadding="4" bgcolor="DarkSlateGray" id="alertDetailsTable">

	<thead>
    <tr>
        <th style="background:LightGreen"><b>Alert ID</b></th>
        <th style="background:LightGreen"><b>Description</b></th>
        <th style="background:LightGreen"><b>Current Value</b></th>
        <th style="background:LightGreen"><b>Activation Status</b></th>
        <th style="background:LightGreen"><b>Time in Alert</b></th>
        <th style="background:LightGreen"><b>Event Type</b></th>
        <th style="background:LightGreen"><b>Attribute Type</b></th>
        <th style="background:LightGreen"><b>Host Name</b></th>
        <th style="background:LightGreen"><b>Deployed Type</b></th>
        <th style="background:LightGreen"><b>Deployed Scope</b></th>
    </tr>
	</thead>
<tbody>	
<%
for(DashboardAlertStatus alertStatus:alertStatii) {
    String alertId = alertStatus.getThresholdConfigId();
    String description = alertStatus.getShortDescription();
    String activationStatus = alertStatus.getActivationStatus();
    String timeInAlert = alertStatus.getFormattedTimeInAlert();
    String eventType = alertStatus.getEventType();
    String attributeType = alertStatus.getAttributeType();
    String hostName = alertStatus.getAttribute(AlertStatusManager.HOST_ATTR);
    String deployedType = alertStatus.getAttribute(AlertStatusManager.TYPE_ATTR);
    String deployedScope = alertStatus.getAttribute(AlertStatusManager.PATH_ATTR);
    String currentValue = alertStatus.getAttribute(attributeType);
    
    if(alertId == null) alertId = "";
    if(description == null) description = "";
    if(activationStatus == null) activationStatus = "";
    if(timeInAlert == null) timeInAlert = "";
    if(eventType == null) eventType = "";
    if(attributeType == null) attributeType = "";

    if(deployedType == null) deployedType = "";
    if(deployedScope == null) deployedScope = "";
    if(currentValue == null) currentValue = "";
%>
    <tr>
        <td style="background:#CCFFCC"><%=alertId %></td>
        <td style="background:#CCFFCC"><%=description %></td>
        <td style="background:#CCFFCC"><%=currentValue %></td>
        <td style="background:#CCFFCC"><%=activationStatus %></td>
        <td style="background:#CCFFCC"><%=timeInAlert %></td>
        <td style="background:#CCFFCC"><%=eventType %></td>
        <td style="background:#CCFFCC"><%=attributeType %></td>
        <td style="background:#CCFFCC">
<% if (hostName != null) { 
	HashMap<String,String> graphParams = new HashMap<String,String>();
	graphParams.put("host", hostName);
	graphParams.put("graphType", "BY_HOST");
	graphParams.put("eventType", eventType);
	graphParams.put("key", attributeType);	
	graphParams.put("type", deployedType);
	graphParams.put("alert", "true");	
	String graphUrl = DashboardContextUtils.getGraphServletUrl(alertContextManager, graphParams);
	String mouseOver = DashboardContextUtils.getPopupString(
		"<img src=&quot;" + graphUrl + "&quot; width=&quot;" +
		 	DashboardGraphUtils.DEFAULT_GRAPH_WIDTH + "&quot; height=&quot;" + DashboardGraphUtils.DEFAULT_GRAPH_HEIGHT + "&quot;/>",
		DashboardGraphUtils.DEFAULT_GRAPH_WIDTH,
		DashboardGraphUtils.DEFAULT_GRAPH_HEIGHT);	
	
%>
			<a <%=mouseOver%> href="<%=DashboardContextUtils.getRelativeDashboardPageByHostUrl(alertContextManager,hostName.split("\\.")[0])%>"><%=hostName%></a>
<% } %>
		</td>
        <td style="background:#CCFFCC"><%=deployedType %></td>
        <td style="background:#CCFFCC"><%=deployedScope %></td>
    </tr>

<%
}
%>
</tbody>

</table>


    <br>
    <br>
    <a href="<%=dashboardUrl %>">Return to Dashboard View</a>

<%@include file="../global_includes/footer.jsp" %>