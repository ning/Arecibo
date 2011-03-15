package com.ning.arecibo.dashboard.table;

import java.util.List;
import java.util.Set;

import static com.ning.arecibo.dashboard.graph.DashboardGraphUtils.GraphType;

public interface DashboardTableBean
{
    public void initBean();
    public String getTableTitle();
    public String getPlainTableTitle();
    public Set<String> getSubTitles();
    
    public Set<String> getCompositeHeadersBySubTitle(String subTitle);
    public List<String> getEventsBySubTitle(String subTitle);
    public List<String> getAttributesBySubTitle(String subTitle);
    
    public String getAttrFromCompositeHeader(String compositeKey);
    public String getEventFromCompositeHeader(String compositeKey);
    public String getValueStringByCompositeHeader(String subTitle,String header);
    public String getMinValueStringByCompositeHeader(String header);
    public String getMaxValueStringByCompositeHeader(String header);
    public String getDatapointsStringByCompositeHeader(String header);
    public String getTimeSinceStringByCompositeHeader(String header);
    
    public String getTableHost();
    public String getTableDepPath();
    public String getTableDepType();
    public GraphType getTableGraphType();
    
    public void joinChildTable(DashboardTableBean childTableBean);
    public Iterable<DashboardTableBean> getJoinedChildTables();
    public void joinParentTable(DashboardTableBean parentTableBean);
    public DashboardTableBean getJoinedParentTable();
    
    public String getTableBeanSignature();
}
