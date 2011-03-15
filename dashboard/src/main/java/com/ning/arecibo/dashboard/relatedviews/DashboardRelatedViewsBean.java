package com.ning.arecibo.dashboard.relatedviews;

import java.util.List;

public interface DashboardRelatedViewsBean
{
    public void initBean();
    public List<String> getRelatedHosts();
    public List<String> getRelatedPaths();
    public List<String> getRelatedTypes();
    public List<String> getRelatedGroupings();
    public String getRelatedHostsTitle();
    public String getRelatedPathsTitle();
    public String getRelatedTypesTitle();
    public String getRelatedGroupingsTitle();
    public int getHostCount();
    public int getPathCount();
    public int getTypeCount();
    public int getGroupingCount();
}
