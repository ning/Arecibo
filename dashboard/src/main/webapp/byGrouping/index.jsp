<%@include file="includes/setup.jsp" %>
<%@include file="../global_includes/header.jsp" %>

<script type="text/javascript" src="../js/wz_tooltip.js"></script>
<script type="text/javascript" src="../js/jquery-1.3.2.min.js"></script>

<%
    DashboardRelatedViewsBean dashboardRelatedViewsBean = new DashboardRelatedViewsByGrouping(tableContextManager);
    dashboardRelatedViewsBean.initBean();
%>
<%@include file="includes/filter.jsp" %>

<script>
function updateFilter() {
	var filter = $("#filterTerm").val().toLowerCase()
	if (filter.length == 0 || filter.length > 2) {
		var metrics = $("td")
		for (var i=0; i<metrics.length; i++) {
			var id = metrics[i].id
			if (id.indexOf("metric-")==0 && id.indexOf(":") > 0) {
				var id = id.substring(id.indexOf(":")+1).toLowerCase()
				if (id.indexOf(filter) == -1) {
					$(metrics[i]).hide()
				} else {
					if (filter.length > 0) {
						$(metrics[i]).parent().show()
					}
					$(metrics[i]).show()
				}
			}
		}
	}
}

function clearFilter() {
	$("#filterTerm").val("")
	updateFilter()
}

function expandAll(doExpand) {
	clearFilter()
	var metrics = $("tr")
	for (var i=0; i<metrics.length; i++) {
		if (metrics[i].id.indexOf("metric-group")==0) {
			if (doExpand) {
				$(metrics[i]).show() 
			} else {
				$(metrics[i]).hide()
			}			
		}
	}
}
</script>

<%
    
    String timeWindowString = new Long(DashboardGraphUtils.TimeWindow.ONE_HOUR.getMillis()).toString();
    String timeFromString = Long.toString(System.currentTimeMillis() - DashboardGraphUtils.TimeWindow.ONE_HOUR.getMillis());

    DashboardTableGroupingBean tableGroupingBean = new DashboardTableGroupingBean(tableContextManager);
    
    List<DashboardTableBean> dashboardTableBeans = tableGroupingBean.getTableBeans();

    if(dashboardTableBeans == null || dashboardTableBeans.size() == 0) {
        if(tableGroupingBean.getLastException() != null) {
%>
            Got exception retrieving data:
            <pre>
<%            
            java.io.PrintWriter printWriter = new java.io.PrintWriter(out);
            tableGroupingBean.getLastException().printStackTrace(printWriter);
%>
            </pre>
<%
        }
        else {
%>
            No tables selected
<%
        }
    }
    else {
%>
<div style="padding:15px">
<span style="background:#cccccc; padding:10px">
<b>Filter:</b> <input type="text" name="filterTerm" id="filterTerm" onkeyup="updateFilter()"/>  (min 3 chars)
</span>
<input type="button" value="Collapse All" onclick="expandAll(false)"/><input type="button" value="Expand All" onclick="expandAll(true)"/>
</div>
<%
        int tableCount = 0;
        for(DashboardTableBean dashboardTableBean:dashboardTableBeans) {
            dashboardTableBean.initBean();
%>           
<%@include file="includes/table.jsp" %>
<%
        }
        
        if(tableCount == 0) {
%>
    No tables available
<%
           
        }
    }
%>

<%@include file="../global_includes/footer.jsp" %>