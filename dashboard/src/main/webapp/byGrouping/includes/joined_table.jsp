
<%
		String joinedTableHost = joinedDashboardTableBean.getTableHost();
		String joinedTablePath = joinedDashboardTableBean.getTableDepPath();
		String joinedTableType = joinedDashboardTableBean.getTableDepType();
		String joinedTableBeanSignature = joinedDashboardTableBean.getTableBeanSignature();
		Set<String> joinedSubTitles = joinedDashboardTableBean.getSubTitles();
		if(joinedSubTitles != null && joinedSubTitles.size() > 0) {
		    
		    for(String joinedSubTitle:joinedSubTitles) {
		 	   
				if(DashboardContextUtils.isSubTitlePerEventSpecific(joinedSubTitle)) { 
					continue;
				}
				
				int id = (int)(Math.random() * 99999);
				String joinedHostLink = DashboardContextUtils.getTableSubTitleAdHocHostLink(tableContextManager,joinedTableHost);
				
%>
<tr><td colspan="11" style="background:skyblue;text-align:left"><span style="font-weight:bold; cursor:pointer; text-decoration:underline;" onclick='$("#metric-group-<%=id%>").toggle()'><b><%=joinedSubTitle %></span>
<span style="font-size:75%;color:Sienna"><i>(from related global zone: <%=joinedHostLink %>)</i></span></b></td></tr>
<%
           
				Set<String> headers = joinedDashboardTableBean.getCompositeHeadersBySubTitle(joinedSubTitle);
				List<String> events = joinedDashboardTableBean.getEventsBySubTitle(joinedSubTitle);
				List<String> attrs = joinedDashboardTableBean.getAttributesBySubTitle(joinedSubTitle);
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
				    
				    String graphUrl = DashboardContextUtils.getRelativeGraphPageUrl(tableContextManager,joinedTableBeanSignature,joinedSubTitle);
				    
				    Map<String,String> sparklineParams = DashboardContextUtils.getGraphParams(joinedTableGraphType,joinedTableType,joinedTablePath,joinedTableHost,
				                                                     event,attr,alertFlags.get(count),
				                                                     timeWindowString,timeFromString);
				    String sparklineServletUrl = DashboardContextUtils.getSparklineServletUrl(tableContextManager,sparklineParams);
				    String sparklineToolTipString = DashboardContextUtils.getPopupAlertStatusString(tableContextManager,sparklineParams);
				    count++;
%>
<td id="metric-<%=headerId%>:<%=event%>-<%=attr%>">
<b><%=event %></b>
<div style="font-size:90%;color:Sienna"><%=attr %><br/></div>
<%
                   if(graphUrl != null) {
%>
<div><a href="<%=graphUrl %>" <%=sparklineToolTipString%>><img src="<%=sparklineServletUrl%>" border="0"
																width="<%=DashboardGraphUtils.DEFAULT_SPARKLINE_WIDTH%>"
																height="<%=DashboardGraphUtils.DEFAULT_SPARKLINE_HEIGHT%>"/></a><br/></div>
<%
                   }

                   String value = joinedDashboardTableBean.getValueStringByCompositeHeader(joinedSubTitle,header);
        	       String timeSince = joinedDashboardTableBean.getTimeSinceStringByCompositeHeader(header);
%>
<div style="font-size:110%"><%=value %></div>
<div style="font-size:75%;color:DarkGreen"><%=timeSince%><br/></div>
</td>
<%
               }
%>
</tr>
<%
           }
       }
%>