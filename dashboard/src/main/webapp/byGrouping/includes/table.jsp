<%
       String title = dashboardTableBean.getTableTitle();
       String tableHost = dashboardTableBean.getTableHost();
       String tablePath = dashboardTableBean.getTableDepPath();
       String tableType = dashboardTableBean.getTableDepType();
       String tableGraphType = dashboardTableBean.getTableGraphType().toString();
       String tableBeanSignature = dashboardTableBean.getTableBeanSignature();
       Set<String> subTitles = dashboardTableBean.getSubTitles();
       if(subTitles != null && subTitles.size() > 0) {
           tableCount++;
%>
<table border="1" cellspacing="2" cellpadding="4" bgcolor="DarkSlateGray">
<tr><td colspan="11" style="background:#009;text-align:left;color:white"><b><%=title %></b>
</td></tr>
<%
			int totalAlertCount = 0;
            for(String subTitle:subTitles) {	
				int id = (int)(Math.random() * 99999);	
				
				if(DashboardContextUtils.isSubTitlePerEventSpecific(subTitle)) {
%>
<tr><td colspan="11" style="background:LightBlue;text-align:left;"><span style="font-size:80%; cursor:pointer; font-weight:bold; text-decoration:underline;" onclick='$("#metric-group-<%=id%>").toggle()'><%=subTitle %></span>
	<b><span style="background:red;color:white" id='metric-group-<%=id%>-alert-count'></span></b>
</td></tr>
<%
				}
				else {
%>
<tr><td colspan="11" style="background:skyblue;text-align:left;"><span style="cursor:pointer; font-weight:bold; text-decoration:underline;" onclick='$("#metric-group-<%=id%>").toggle()'><b><%=subTitle %></b></span>
	<b><span style="background:red;color:white" id='metric-group-<%=id%>-alert-count'></span></b>
</td></tr>
<%
				}
           
				Set<String> headers = dashboardTableBean.getCompositeHeadersBySubTitle(subTitle);
				List<String> events = dashboardTableBean.getEventsBySubTitle(subTitle);
				List<String> attrs = dashboardTableBean.getAttributesBySubTitle(subTitle);
				List<String> alertFlags = DashboardContextUtils.getAlertFlags(tableContextManager,events,attrs,tableType,tablePath,tableHost);
%>
<tr id="metric-group-<%=id%>" style="display:none"><a name="metric-group-<%=id%>">
<%
                int count = 0;
				int alertCount = 0;
				
                for(String header:headers) {	
					int headerId = (int)(Math.random() * 99999);// to keep things unique
					
					String alert = alertFlags.get(count);
					if (alert!=null&&alert.length()>0) {
						alertCount++;
						totalAlertCount++;
					}
						
                   String event = events.get(count);
                   String attr = attrs.get(count);
                   
        	       String graphUrl = DashboardContextUtils.getRelativeGraphPageUrl(tableContextManager,tableBeanSignature,subTitle);
        	       
        	       Map<String,String> sparklineParams = DashboardContextUtils.getGraphParams(tableGraphType,tableType,tablePath,tableHost,
        	                                                        event,attr,alertFlags.get(count),
        	                                                        timeWindowString,timeFromString);
        	       String sparklineServletUrl = DashboardContextUtils.getSparklineServletUrl(tableContextManager,sparklineParams);
        	       String sparklineToolTipString = DashboardContextUtils.getPopupAlertStatusString(tableContextManager,sparklineParams);
        	       count++;
%>
<td id="metric-<%=headerId%>:<%=event%>-<%=attr%>">
<b><%=event%></b>
<div style="font-size:90%;color:Sienna"><%=attr %><br/></div>
<%
                   if(graphUrl != null) {
%>
<div><a href="<%=graphUrl %>" <%=sparklineToolTipString%>><img src="<%=sparklineServletUrl%>" border="0"
																width="<%=DashboardGraphUtils.DEFAULT_SPARKLINE_WIDTH%>"
																height="<%=DashboardGraphUtils.DEFAULT_SPARKLINE_HEIGHT%>"/></a><br/></div>
<%
                   }

                   String value = dashboardTableBean.getValueStringByCompositeHeader(subTitle,header);
        	       String timeSince = dashboardTableBean.getTimeSinceStringByCompositeHeader(header);
%>
<div style="font-size:110%"><%=value %></div>
<div style="font-size:75%;color:DarkGreen"><%=timeSince%><br/></div>
</td>
<%
               }

			// if there were any metrics under alert in this <TR>, then expand it
			if (alertCount>0) {
				out.println("<script>$('#metric-group-"+id+"').show()</script>");
				out.println("<script>$('#metric-group-"+id+"-alert-count').html('ALERTS: " + alertCount+ "')</script>");
			}
%>

</tr>
<%
           }
           Iterable<DashboardTableBean> joinedTables = dashboardTableBean.getJoinedChildTables();
           if(joinedTables != null) {
				for(DashboardTableBean joinedDashboardTableBean:joinedTables) {
					joinedDashboardTableBean.initBean();
                   
					// for now, don't show sparkline tables for higher level related globalzone data
					String joinedTableGraphType = joinedDashboardTableBean.getTableGraphType().toString();
        			if(!joinedTableGraphType.equals(DashboardGraphUtils.GraphType.BY_HOST.toString()))
        				continue;
        		 
%>
<%@include file="joined_table.jsp" %>
<%
        
               }
           }
%>
</table>
</br>
<%
       }
%>