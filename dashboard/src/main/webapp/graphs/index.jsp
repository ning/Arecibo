<%@include file="includes/graph_setup.jsp" %>
<%@include file="../global_includes/header.jsp" %>

<link rel="stylesheet" type="text/css" href="../styles/anytime.css" />
<script type="text/javascript" src="../js/wz_tooltip.js"></script>
<script type="text/javascript" src="../js/jquery-1.3.2.min.js"></script>
<script type="text/javascript" src="../js/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="../js/jquery.tablesorter.pager.js"></script>
<script type="text/javascript" src="../js/anytime.js"></script> 
<script>

function legendToggle(count,url) {
    $("#legend" + count).toggle();

    if(typeof(window["legendToggleCount" + count]) == "undefined")
        $("#legend" + count).load(url);
    
	window["legendToggleCount" + count] = 1;
}   

var legendTextExtraction = function(node) {
	return node.childNodes[0].innerHTML;
}

</script>

<%
        String dashboardUrl = DashboardContextUtils.getRelativeDashboardPageUrl(graphContextManager);
%>
<a href="<%=dashboardUrl %>">Return to Dashboard View</a>
<br>
<br>

<form method="GET" action="<%=contextPath%>/graphs">

<%
        Long timeWindow = DashboardContextUtils.getGraphTimeWindowParameter(multipleGraphParams);
        if(timeWindow == null) {
            timeWindow = DashboardGraphUtils.TimeWindow.getDefaultTimeWindow().getMillis();
        }
        
        Long timeFrom = DashboardContextUtils.getGraphTimeFromParameter(multipleGraphParams);
        Long timeFromDerived = DashboardContextUtils.getGraphTimeFromDerived(multipleGraphParams,timeWindow);
        Long timeTo = DashboardContextUtils.getGraphTimeToDerived(multipleGraphParams);
        String timeFromString = DashboardContextUtils.getFormattedDateTimeFromMillis(timeFromDerived);
        String timeToString = DashboardContextUtils.getFormattedDateTimeFromMillis(timeTo);

        List<String> refreshParamList = DashboardContextUtils.getGraphPageTimeWindowRefreshParams(graphContextManager);
        for(String refreshParam:refreshParamList) {
%>
        <%=refreshParam%>
<%
        }
        
        
%>
<table border="4" cellspacing="4" cellpadding="4" bgcolor="DarkSlateGray">
    <tr><td colspan="<%=numCols%>" style="background:#006644">
    <font color="FFFFFF"><b><%=pageTitle%></b>
    	<br>
    	<br>
    	<div align="center">
    	<table>
    	<tr>
        <td style="background:#CCFFCC">
    	<b>Metric</b>
    	<br>
        <select name="subTitle" onchange="this.form.submit();">
<%
        for(String subTitle:subTitles) {
            String selected = "";
            String disabled = "";
            if(currSubTitle.equals(subTitle)) {
                selected = "SELECTED";
            }
            else if(subTitle.equals(DashboardContextUtils.RELATED_SUBTITLE_SEPARATOR)) {
            	disabled = "DISABLED";
            }
%>
            <option value="<%=subTitle%>" <%=selected%> <%=disabled %>>
                <%=subTitle%>
            </option>
<%
        }
%>
        </select>
        </td>

        <td style="background:#CCFFCC">
        <b>Resolution</b>
    	<br>
     	<select name="resolutionRequest" onchange="this.form.submit();">   
<%
		for(ResolutionRequestType reqType:ResolutionRequestType.values()) {
			
			if(reqType == ResolutionRequestType.FIXED)
				continue;
			
			String selected;
            if(resolutionRequest.equals(reqType.toString())) {
            	selected = "SELECTED";
            }
            else {
            	selected = "";
            }
%>
            <option value="<%=reqType%>" <%=selected%>>
                <%=reqType%>
            </option>
<%
		}
%>
            <option value="<%=DashboardContextUtils.DEFAULT_SUBTITLE_SEPARATOR%>" DISABLED>
                <%=DashboardContextUtils.DEFAULT_SUBTITLE_SEPARATOR%>
            </option>
<%
		for(int reduction:reductionFactors) {
			String selected;
            if(resolutionRequest.equals(Integer.toString(reduction))) {
            	selected = "SELECTED";
            }
            else {
            	selected = "";
            }
%>
            <option value="<%=reduction%>" <%=selected%>>
                <%=reduction%> minute
            </option>
<%
		}
%>
     	</select>
     	</td>
     	
        <td style="background:#CCFFCC">
        <b>Max Time Series Per Graph</b>
    	<br>
        <input type="text" name="maxTSPerGraph" value="<%=maxTSPerGraph %>" size="4" onchange="this.form.submit();">
        </td>
        
        <td style="background:#CCFFCC">
        <b>Update Time Window</b>
    	<br>
<%
        
        if(timeFrom != null) {
%>
        <input type="hidden" name="timeFrom" value="<%=timeFrom%>">
<%
        }
        
%>
        <input type="submit" name="timeWindowShiftHalfBack" value="<-">
        <select name="timeWindow" onchange="this.form.submit();">
<%
        for(DashboardGraphUtils.TimeWindow optionWindow:DashboardGraphUtils.TimeWindow.values()) {
            String selected = "";
            if(timeWindow != null && optionWindow.getMillis() == timeWindow) {
                selected = "SELECTED";
            }
%>
            <option value="<%=optionWindow.getMillis() %>" <%=selected%>>
                <%=optionWindow.getDisplayName() %>
            </option>
<%
        }
%>
        </select>
<%
        if(timeFrom != null) {
%>
        <input type="submit" name="timeWindowShiftHalfForward" value="->">
<%
        }
%>
        <br>
        <i><div style="font-size:75%">
        (Use arrows to shift time range<br>by half the selected time window)
        </div></i>
        </td>
        
        <td style="background:#CCFFCC">
        <b>Start Time (GMT):</b><br>
        <input type="text" id="timeFromUpdate" name="timeFromUpdate" value="<%=timeFromString%>"/>
        <br>
        <input type="submit" name="timeFromUpdateSubmit" value="Update"/>
        </td>

        </tr>
        </table>
        </div>




    </font>
    </td>
    </tr>

<script type="text/javascript">
    AnyTime.widget( "timeFromUpdate", {format: "%b %e, %Z %H:%i"});
</script>
    
<%
        for(int count=0;count < graphUrls.size(); count++) {
            String graphUrl = graphUrls.get(count);
            String legendUrl = legendUrls.get(count);
            String dataUrl = dataUrls.get(count);
            String graphToolTipString = alertToolTipStrings.get(count);
                        
            boolean newRow = (count % numCols) == 0;
            boolean endRow = (count % numCols) == numCols-1;
            
            if(newRow) {
%>

<tr>
<%
            }
%>

<td>
    <div>
    <img src="<%=graphUrl %>" 
         width="<%=DashboardGraphUtils.DEFAULT_GRAPH_WIDTH %>" 
         height="<%=DashboardGraphUtils.DEFAULT_GRAPH_HEIGHT %>" 
         <%=graphToolTipString %>/>
         
    </div>
    <div align="CENTER">
    <table>
    	<tr>
    		<td style="background:SkyBlue">
    			<input type="button" value="legend" onclick="legendToggle(<%=count%>,'<%=legendUrl%>')"/>
    		</td>
    		<td style="background:SkyBlue">
    			<a rel="bookmark" href="<%=graphUrl %>" target="_blank">graph permalink</a> 
    		</td>
    		<td style="background:SkyBlue">
    			<a rel="bookmark" href="<%=dataUrl %>" target="_blank">data permalink</a>
    		</td>
    	</tr>
    </table>
    </div>
    <br>
    <div id="legend<%=count%>" style="display:none" align="CENTER">
    </div>
    <br>
    
</td>
<%
            if(endRow) {
%>
</tr>
<%
            }
        }
%>
</table>


    
<%
        if(hasAlerts) {
            String alertUrl = DashboardContextUtils.getAlertsPageUri(graphContextManager);
%>
    <br>
    <br>
    <a href="<%=alertUrl %>">View All Currently Active Alerts</a>
    <br>
<%
        }

%>
    
    <br>
    <a href="<%=dashboardUrl %>">Return to Dashboard View</a>
    
    </form>
    
<%@include file="../global_includes/footer.jsp" %>

