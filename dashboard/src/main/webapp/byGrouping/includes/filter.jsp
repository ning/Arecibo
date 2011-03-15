<%

    String dashboardPageUri = DashboardContextUtils.getDashboardPageUri(tableContextManager);

	String relatedGroupingsTitle = "No Data Available";
	String relatedPathsTitle = "No Data Available";
	String relatedHostsTitle = "No Data Available";
%>
<table border="1" cellspacing="2" cellpadding="4" bgcolor="SlateGray">
<tr>
	<td colspan="2" style="background:DarkGreen" align="center"><font color="LightBlue"><b>Grouping By Scope</b></font></td>
	<td colspan="1" style="background:DarkGreen" align="center"><font color="LightBlue"><b>Ad Hoc Hosts to View</b></font></td>
	<td colspan="1" style="background:DarkGreen" align="center"><font color="LightBlue"><b>Options</b></font></td>
	<td colspan="1" style="background:DarkGreen" align="center"><font color="LightBlue"><b>Alerts</b></font></td>
</tr>
<%
        List<String> relatedGroupings = dashboardRelatedViewsBean.getRelatedGroupings();
        if(relatedGroupings != null && relatedGroupings.size() > 0) {
            relatedGroupingsTitle = dashboardRelatedViewsBean.getRelatedGroupingsTitle();
        }
        
        List<String> relatedPaths = dashboardRelatedViewsBean.getRelatedPaths();
        if(relatedPaths != null && relatedPaths.size() > 0) {
            relatedPathsTitle = dashboardRelatedViewsBean.getRelatedPathsTitle();
        }
        
        relatedHostsTitle = dashboardRelatedViewsBean.getRelatedHostsTitle();
        
        boolean alertStatusAvailable = DashboardContextUtils.isAlertStatusAvailable(tableContextManager);
        int numActiveAlerts = DashboardContextUtils.getActiveAlertCount(tableContextManager);
        
%>
    <tr>
           
    <form method="GET" action="<%=dashboardPageUri%>">
		<td style="background:LightGreen; text-align:left">
		
	<%
        if(tableContextManager.getShowDebugMode()) {
%>
    <input type="hidden" name="<%=DashboardContextUtils.DEBUG_MODE_PARAM %>" value="on">
<%
        }
        if(tableContextManager.getShowHostsWithAggregates()) {
%>
    <input type="hidden" name="<%=DashboardContextUtils.SHOW_HOSTS_WITH_AGGREGATES_ON_UPDATE_BUTTON %>" value="on">
<%
        }
%>	


<%
        boolean groupingRendered = false;
        if(groupingsInContextList != null && groupingsInContextList.size() > 0 && !groupingsInContextList.contains(DashboardContextUtils.UNDEFINED_GROUPING_NAME)) {                        
            
            int i=0;
            for(String grouping:groupingsInContextList) {
                
                if(grouping.equals(""))
                    continue;
                
                if(!groupingRendered) {
%>
				<b>Current Groupings</b>
				<br>
<%
                	groupingRendered = true;
                }
%>
        	    <input type="hidden" name="grouping" size="7" value="<%=grouping%>">
                <input type="checkbox" name="<%=DashboardContextUtils.GROUPING_CHECKBOX_VALID_PREFIX%><%=grouping%>" checked onchange="this.form.submit();">
                <%=grouping %>
                <br>
<%
            }
            if(groupingRendered) {
%>
                <br>
<%   
            }
        }
        else {
%>
                <br>
<%   
        }
%>
        </td>

		<td style="background:LightGreen; text-align:left">
<%
        boolean pathRendered = false;
        if(pathsInContextList != null && pathsInContextList.size() > 0 && !pathsInContextList.contains(DashboardContextUtils.UNDEFINED_PATH_NAME)) {                        
            
            int i=0;
            for(String path:pathsInContextList) {
                
                if(path.equals(""))
                    continue;
                
                if(!pathRendered) {
%>
                <b>Current Scopes</b>
                <br>
<%
                    pathRendered = true;
                }
%>
                <input type="hidden" name="path" size="7" value="<%=path%>">
                <input type="checkbox" name="<%=DashboardContextUtils.PATH_CHECKBOX_VALID_PREFIX%><%=path%>" checked onchange="this.form.submit();">
                <%=path %>
                <br>
<%
            }
            if(pathRendered) {
%>
                <br>
<%   
            }
        }
        else {
%>
                <br>
<%   
        }
%>
        </td>

		<td style="background:LightGreen; text-align:left">
<%

        boolean hostRendered = false;
        if(hostsInContextList != null && hostsInContextList.size() > 0 && !hostsInContextList.contains(DashboardContextUtils.UNDEFINED_HOST_NAME)) {
            int i=0;
            for(String host:hostsInContextList) {
                
                if(host.equals(""))
                    continue;
                
                if(!hostRendered) {
%>
				<b>Current Ad Hoc Hosts</b>
				<br>
<%
                	hostRendered = true;
                }
                
%>
        	    <input type="hidden" name="host" size="7" value="<%=host%>">
                <input type="checkbox" name="<%=DashboardContextUtils.HOST_CHECKBOX_VALID_PREFIX%><%=host%>" checked onchange="this.form.submit();">
                <%=host %>
                <br>
<%
            }
            if(hostRendered) {
%>
                <br>
<%   
            }
        }
        else {
%>
                <br>
<%   
        }
%>
        </td>
        
        <td style="background:LightGreen; text-align:left">
<%
        String showAllDataValuesChecked;
        if(showAllDataValues) {
            showAllDataValuesChecked = "checked";
        }
        else {
            showAllDataValuesChecked = "";
        }
        String showAllDataValuesString = Boolean.toString(showAllDataValues);
%>
        <input type="hidden" name="<%=DashboardContextUtils.PREV_SHOW_ALL_DATA_VALUES_FLAG %>" value="<%=showAllDataValuesString %>">
        <input type="checkbox" name="<%=DashboardContextUtils.SHOW_ALL_DATA_VALUES%>" <%=showAllDataValuesChecked%> onchange="this.form.submit()">
        Show All Available Data
        
        <br>
<%
        String showGlobalZoneValuesChecked;
        if(showGlobalZoneValues) {
            showGlobalZoneValuesChecked = "checked";
        }
        else {
            showGlobalZoneValuesChecked = "";
        }
        String showGlobalZoneValuesString = Boolean.toString(showGlobalZoneValues);
%>
        <input type="hidden" name="<%=DashboardContextUtils.PREV_SHOW_GLOBAL_ZONE_VALUES_FLAG %>" value="<%=showGlobalZoneValuesString %>">
        <input type="checkbox" name="<%=DashboardContextUtils.SHOW_GLOBAL_ZONE_VALUES%>" <%=showGlobalZoneValuesChecked%> onchange="this.form.submit()">
        Show Related System Data
         
        </td>
        
        <td style="background:LightGreen; text-align:center">
<%
        String alertsMessage;
        
        if(!alertStatusAvailable) {
            alertsMessage = "<b>Alert Service not Available<br>or Not Responding</b>";
%>
	    <font color="Red"><%=alertsMessage%></font></td>
<%
        }
        else if(numActiveAlerts > 0) {
            if(numActiveAlerts == 1) {
                alertsMessage = "There is currently<br><div style=\"font-size:150%\">" + numActiveAlerts + "</div><br>active alert";
            }
            else {
                alertsMessage = "There are currently<br><div style=\"font-size:150%\">" + numActiveAlerts + "<br></div>active alerts";
            }
%>
	    <font color="DarkRed"><%=alertsMessage%></font></td>
<%
        }
        else {
            alertsMessage = "There are no currently<br>active alerts";
%>
	    <font color="Black"><%=alertsMessage%></font></td>
<%
        }
%>
        </td>
        
     </tr>
     <tr>   
        <td style="background:#CCFFCC">
            <b>Add Grouping</b>
            <br>
            <select name="new_grouping" onchange="this.form.submit();">
                <option value="">--select--</option>
<%
        if(relatedGroupings != null && relatedGroupings.size() > 0) {
            String disabled;
            for(String relatedGrouping:relatedGroupings) {
                if(relatedGrouping == null)
                    continue;
                
                if(relatedGrouping.equals(DashboardRelatedViewsByGrouping.GROUPING_SEPARATOR))
                    disabled = "DISABLED";
                else
                    disabled = "";
                
%>
                <option <%=disabled%> %><%=relatedGrouping%></option>
<%
            }
        }
%>
            </select>
        </td>
        
        <td style="background:#CCFFCC">
            <b>Add Scope</b>
            <br>
            <select name="new_path" onchange="this.form.submit();">
                <option value="">--select--</option>
<%
        if(relatedPaths != null && relatedPaths.size() > 0) {
            for(String relatedPath:relatedPaths) {
                if(relatedPath == null)
                    continue;
%>
                <option><%=relatedPath%></option>
<%
            }
        }
%>
            </select>
        </td>        
        
		<td style="background:#CCFFCC">
                <b>Add Host (and domain)</b>
                <br>
        	    <input type="text" name="new_host" size="7" value="">
        	    .
        	    <input type="text" name="new_host_domain" size="11" value="<%=DashboardContextUtils.DEFAULT_HOST_DOMAIN%>">
        	    <br>
        </td>
        
		<td style="background:#CCFFCC">
		        <br>
    	        <input type="submit" name="<%=DashboardContextUtils.UPDATE_BUTTON %>" value="Update">
    	</td>
        </form>
 
		<td style="background:#CCFFCC">
<%
        if(numActiveAlerts > 0) {
%>
                <form method="GET" action="<%=DashboardContextUtils.getAlertsPageUri(tableContextManager)%>" >
		        <br>
    	        <input type="submit" name="<%=DashboardContextUtils.ALERTS_BUTTON %>" value="View Current Alerts">
                </form>
<%
        }
%>    	
    	</td>
	</tr>

</table>
</br>
</br>