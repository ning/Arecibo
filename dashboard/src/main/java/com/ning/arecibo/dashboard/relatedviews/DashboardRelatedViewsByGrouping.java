package com.ning.arecibo.dashboard.relatedviews;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.ning.arecibo.dashboard.context.DashboardTableContextManager;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAO;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAOException;
import com.ning.arecibo.dashboard.table.DashboardTableGroupingBean.PresetTypeGroupings;

import com.ning.arecibo.util.Logger;
import static com.ning.arecibo.dashboard.context.DashboardContextUtils.*;

public class DashboardRelatedViewsByGrouping implements DashboardRelatedViewsBean
{
    private final static Logger log = Logger.getLogger(DashboardRelatedViewsByGrouping.class);
    
    public final static String GROUPING_SEPARATOR = "--------";
    
    private DashboardTableContextManager contextManager;
    
    private List<String> groupingContextList;
    
    private List<String> hosts;
    private List<String> paths;
    private List<String> types;
    private List<String> groupings;
    private int hostCount;
    private int pathCount;
    private int typeCount;
    private int groupingCount;
    
    private DashboardCollectorDAO collectorDAO = null;
    
    public DashboardRelatedViewsByGrouping() {
    }
    
    public DashboardRelatedViewsByGrouping(DashboardTableContextManager contextManager) {
        
        this.contextManager = contextManager;
        this.groupingContextList = this.contextManager.getGroupings();
        this.collectorDAO = this.contextManager.getCollectorDAO();
    }
    
    public void initBean()
    {
        // limit available paths by the current grouping
        try {
            if(groupingContextList == null || 
                    groupingContextList.size() == 0 || 
                    groupingContextList.contains("")) {
                
                paths = collectorDAO.getPathsOverall();
                if(paths == null) {
            		log.info("No path data retrieved");
        		}
            }
            else {
                // build list of types comprising the list of groups
                // the incoming groupingsList can contain both PresetTypeGroupings and
                // also individual specific types
                List<String> groupingTypeList = new ArrayList<String>();
                if(groupingContextList != null && groupingContextList.size() > 0) {
                    for(String grouping:groupingContextList) {
                        if(PresetTypeGroupings.getNames().contains(grouping)) {
                            
                            for(String presetGrouping:PresetTypeGroupings.valueOf(grouping).typesInGrouping()) {
                                groupingTypeList = addToListUnique(groupingTypeList,presetGrouping);
                            }
                        }
                        else {
                            // assume it's an ordinary type
                            groupingTypeList = addToListUnique(groupingTypeList,grouping);
                        } 
                    }
                } 
                
                for(String type:groupingTypeList) {
                    List<String> pathsWithType = collectorDAO.getPathsForType(type);
                    for(String path:pathsWithType) {
                        paths = addToListUnique(paths,path);
                    }
                }
            }
        }
        catch(DashboardCollectorDAOException ddEx) {
            log.warn(ddEx);
            paths = null;
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx);
        }
        
        if(paths != null) {
            Collections.sort(paths);
            pathCount = paths.size();
        }
        
        try {
            types = collectorDAO.getTypesOverall();
        }
        catch(DashboardCollectorDAOException ddEx) {
            log.warn(ddEx);
            types = null;
        }
        
        if(types == null) {
            log.info("No type data retrieved");
        }
        else {
            Collections.sort(types);
            typeCount = types.size();
        }
        
        groupings = new ArrayList<String>();
        for(PresetTypeGroupings groupingByType:PresetTypeGroupings.values()) {
            groupings.add(groupingByType.toString());
        }
        
        // also add types as ad hoc groupings
        if(types != null) {
            // add separator to start
            groupings.add(GROUPING_SEPARATOR);
            for(String type:types) {
            	groupings.add(type);
        	}
        }
        
        // don't sort these
        groupingCount = groupings.size();
    }
    
    public List<String> getRelatedHosts() {
        return hosts;
    }
    
    public List<String> getRelatedPaths() {
        return paths;
    }
    
    public List<String> getRelatedTypes() {
        return types;
    }
    
    public List<String> getRelatedGroupings() {
        return groupings;
    }
    
    public String getRelatedHostsTitle() {
        return "Hosts";
    }
    
    public String getRelatedPathsTitle() {
        return "Scopes";
    }
    
    public String getRelatedTypesTitle() {
        return "Type";
    }
    
    public String getRelatedGroupingsTitle() {
        return "Groupings";
    }
    
    public int getHostCount() {
        return hostCount;
    }
    
    public int getPathCount() {
        return pathCount;
    }
    
    public int getTypeCount() {
        return typeCount;
    }
    
    public int getGroupingCount() {
        return groupingCount;
    }
}
