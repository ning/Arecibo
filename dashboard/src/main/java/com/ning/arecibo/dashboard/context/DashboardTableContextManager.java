package com.ning.arecibo.dashboard.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.dashboard.context.DashboardContextUtils.*;

public class DashboardTableContextManager extends DashboardContextManager
{
    private final static Logger log = Logger.getLogger(DashboardTableContextManager.class);
    
    private final List<String> hostsInContextList;
    private final List<String> pathsInContextList;
    private final List<String> groupingsInContextList;
    private final boolean showHostsWithAggregates;

    private final static String pageTitle = "Arecibo Dashboard";

    public DashboardTableContextManager(HttpServletRequest request) {
        super(request);

        this.hostsInContextList = new ArrayList<String>();
        this.pathsInContextList = new ArrayList<String>();
        this.groupingsInContextList = new ArrayList<String>();

        // see if these flags changed, if so, force showHostsWithAggregates on
        boolean prevShowAllDataValuesFlag;
        String prevShowAllDataValuesFlagString = getPrimaryParamValue(PREV_SHOW_ALL_DATA_VALUES_FLAG);
        if (prevShowAllDataValuesFlagString != null)
            prevShowAllDataValuesFlag = Boolean.parseBoolean(prevShowAllDataValuesFlagString);
        else
            prevShowAllDataValuesFlag = false;
        boolean showAllDataValuesChanged = prevShowAllDataValuesFlag != showAllDataValues;

        boolean prevShowGlobalZoneValuesFlag;
        String prevShowGlobalZoneValuesFlagString = getPrimaryParamValue(PREV_SHOW_GLOBAL_ZONE_VALUES_FLAG);
        if (prevShowGlobalZoneValuesFlagString != null)
            prevShowGlobalZoneValuesFlag = Boolean.parseBoolean(prevShowGlobalZoneValuesFlagString);
        else
            prevShowGlobalZoneValuesFlag = false;
        boolean showGlobalZoneValuesChanged = prevShowGlobalZoneValuesFlag != showGlobalZoneValues;

        this.showHostsWithAggregates = (getPrimaryParamValue(SHOW_HOSTS_WITH_AGGREGATES) != null) ||
                ((getPrimaryParamValue(SHOW_HOSTS_WITH_AGGREGATES_ON_UPDATE_BUTTON) != null) &&
                        ((getPrimaryParamValue(UPDATE_BUTTON) != null) ||
                                showAllDataValuesChanged ||
                                showGlobalZoneValuesChanged));
    }
    
    public void initTableViewParams() {
    
        // deal with any new ad hoc host
        String newHost = null;
        String newHostInContext = getPrimaryParamValue("new_host");
        String newHostDomainInContext = getPrimaryParamValue("new_host_domain");
            
        if(newHostInContext != null && newHostInContext.length() > 0 && 
                newHostDomainInContext != null && newHostDomainInContext.length() > 0) {
            newHost = newHostInContext + "." + newHostDomainInContext;
        }
        // update ad hoc hosts (remove any no longer flagged as valid)
        String[] hostsInContext = parameterMap.get("host");
        this.hostsInContextList.clear();
        if(hostsInContext == null) {
            if(newHost == null)
                addToListUnique(hostsInContextList,DashboardContextUtils.UNDEFINED_HOST_NAME);
        }
        else {
            for(String hostInContext:hostsInContext) {
            	if(getPrimaryParamValue(HOST_CHECKBOX_VALID_PREFIX + hostInContext) != null) {
                	addToListUnique(hostsInContextList,hostInContext);
            	}
            }
        }
        if(newHost != null)
            addToListUnique(hostsInContextList,newHost);
        
        Collections.sort(hostsInContextList);
    
        
        this.pathsInContextList.clear();
        // deal with a new path
        String newPathInContext = getPrimaryParamValue("new_path");
        if(newPathInContext != null && newPathInContext.length() > 0)
            addToListUnique(pathsInContextList,newPathInContext);
        String[] pathsInContext = parameterMap.get("path");
        if(pathsInContext != null) {
            for(String pathInContext:pathsInContext) {
                if(getPrimaryParamValue(PATH_CHECKBOX_VALID_PREFIX + pathInContext) != null) {
                    addToListUnique(pathsInContextList,pathInContext);
                }
            }
        }
        Collections.sort(pathsInContextList);
    
        
        groupingsInContextList.clear();
        // deal with a new grouping
        String newGroupingInContext = getPrimaryParamValue("new_grouping");
        if(newGroupingInContext != null && newGroupingInContext.length() > 0)
            addToListUnique(groupingsInContextList,newGroupingInContext);
        String[] groupingsInContext = parameterMap.get("grouping");
        if(groupingsInContext != null) {
            for(String groupingInContext:groupingsInContext) {
                if(getPrimaryParamValue(GROUPING_CHECKBOX_VALID_PREFIX + groupingInContext) != null) {
                    addToListUnique(groupingsInContextList,groupingInContext);
                }
            }
        }
        Collections.sort(groupingsInContextList);
        
    }
    public List<String> getHosts() {
        return hostsInContextList;
    }
        
    public List<String> getPaths() {
        return pathsInContextList;
    }
        
    public List<String> getGroupings() {
        return groupingsInContextList;
    }
    
    public boolean getShowHostsWithAggregates() {
        return showHostsWithAggregates;
    }
    
    public String getPageTitle() {
        return pageTitle;
    }
}
