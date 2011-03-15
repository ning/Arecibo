package com.ning.arecibo.dashboard.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.ning.arecibo.dashboard.context.DashboardTableContextManager;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAO;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAOException;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;

import com.ning.arecibo.util.Logger;
import static com.ning.arecibo.dashboard.context.DashboardContextUtils.*;
import static com.ning.arecibo.dashboard.table.DashboardTableBeanFactory.*;

public class DashboardTableGroupingBean
{
    private final static Logger log = Logger.getLogger(DashboardTableGroupingBean.class);

    private final static String TABLE_CACHE_SIGNATURE_DIFFERENTIATOR_1 = "TCSD1";
    
    public enum PresetTypeGroupings {
        AreciboMonitoring(new String[] {
        	"arecibo-agent",
        	"arecibo-aggregator",
        	"arecibo-alert",
        	"arecibo-collector",
        	"arecibo-dashboard"});


        private String[] typeList;
        
        private PresetTypeGroupings(String[] typeList) {
        	this.typeList = typeList;
        }
        
        public boolean isTypeInGrouping(String cmpType) {
        	if(this.typeList != null) {
                for(String type:typeList) {
                    if(cmpType.equals(type))
                        return true;
                }
                return false;
            }
            
            // return true if typeList is null (e.g. All)
            return true;
        }
        
        public List<String> typesInGrouping() {
        	ArrayList<String> retList = new ArrayList<String>();
        	if(typeList != null) {
        		for(String type:typeList) {
        			retList.add(type);
        		}
        	}
        	return retList;
        }
        
        public static List<String> getNames() {
            PresetTypeGroupings[] values = PresetTypeGroupings.values();
            ArrayList<String> resList = new ArrayList<String>();
            
            for(PresetTypeGroupings value:values) {
                resList.add(value.toString());
            }
            
            return resList;
        }
    }
    
    private final DashboardTableContextManager contextManager;
    private final DashboardTableBeanFactory tableBeanFactory;
    private final DashboardCollectorDAO collectorDAO;
    private final List<String> groupingsInContextList;
    private final List<String> pathsInContextList;
    private final List<String> hostsInContextList;
    private final boolean showHostTablesWithAggregates;
    private final boolean showAllDataValues;
    private final boolean showGlobalZoneValues;
    private final boolean showSpecialDebugValues;

    private Exception lastException = null;
    
    public DashboardTableGroupingBean(DashboardTableContextManager contextManager) {
        
        this.contextManager = contextManager;
        this.tableBeanFactory = contextManager.getTableBeanFactory();
        this.collectorDAO = contextManager.getCollectorDAO();
        this.groupingsInContextList = contextManager.getGroupings();
        this.pathsInContextList = contextManager.getPaths();
        this.hostsInContextList = contextManager.getHosts();
        this.showHostTablesWithAggregates = contextManager.getShowHostsWithAggregates();
        this.showAllDataValues = contextManager.getShowAllDataValues();
        this.showGlobalZoneValues = contextManager.getShowGlobalZoneValues();
        this.showSpecialDebugValues = contextManager.getShowDebugMode();
    }
    
    public List<DashboardTableBean> getTableBeans() {
        
        long startMs = System.currentTimeMillis();
        
        this.lastException = null;
        ArrayList<DashboardTableBean> tableBeans = new ArrayList<DashboardTableBean>();
        
        try {
            
            List<String> groupingsList = null;
            if(groupingsInContextList != null) {
                groupingsList = new ArrayList<String>();
                groupingsList.addAll(groupingsInContextList);
            }
            
            List<String> pathsList = null;
            if(pathsInContextList != null) {
                
                pathsList = new ArrayList<String>();
                pathsList.addAll(pathsInContextList);
                
                while(pathsList.contains(UNDEFINED_PATH_NAME))
                    pathsList.remove(UNDEFINED_PATH_NAME);
            }
            
            List<String> hostsList = null;
            if(hostsInContextList != null) {
                
                hostsList = new ArrayList<String>();
                hostsList.addAll(hostsInContextList);
                
                while(hostsList.contains(UNDEFINED_HOST_NAME))
                    hostsList.remove(UNDEFINED_HOST_NAME);
            }
            
            // build list of types comprising the list of groups
            // the incoming groupingsList can contain both PresetTypeGroupings and
            // also individual specific types
            List<String> groupingTypeList = new ArrayList<String>();
            if(groupingsList != null && groupingsList.size() > 0) {
                for(String grouping:groupingsList) {
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
            
            // by type aggregates
            // only show if no sub-filtering by path
            if(pathsList == null || pathsList.size() == 0) {
                for(String type:groupingTypeList) {
                    
                    List<String> hostsWithType = collectorDAO.getHostsForType(type);
                    int numHostsWithType = hostsWithType.size();
                    
                    if(log.isDebugEnabled())
                        log.debug("adding table bean by type '" + type + "'");
                    
                    DashboardTableBean typeBean;
                    
                    if(showGlobalZoneValues) {
                    	typeBean = tableBeanFactory.getTableBeanByArgs(contextManager,DASHBOARD_TABLE_CACHE_FAST_TIMEOUT,DASHBOARD_TABLE_BEAN_BY_TYPE,
                														type,numHostsWithType,showAllDataValues,
                                                                        false,showSpecialDebugValues,TABLE_CACHE_SIGNATURE_DIFFERENTIATOR_1);
                        log.debug("adding relatedGlobalZones data for type: " + type + "->" + type);
                    	DashboardTableBean globalZonesBean = tableBeanFactory.getTableBeanByArgs(contextManager,DASHBOARD_TABLE_CACHE_FAST_TIMEOUT,DASHBOARD_TABLE_BEAN_BY_TYPE,
                																					type,numHostsWithType,showAllDataValues,true,showSpecialDebugValues);
                        typeBean.joinChildTable(globalZonesBean);
                    }
                    else {
                    	typeBean = tableBeanFactory.getTableBeanByArgs(contextManager,DASHBOARD_TABLE_CACHE_FAST_TIMEOUT,DASHBOARD_TABLE_BEAN_BY_TYPE,
                																					type,numHostsWithType,showAllDataValues,false,showSpecialDebugValues);
                    }
                    
                    tableBeans.add(typeBean);
					
					if(showHostTablesWithAggregates) {
					    for(String host:hostsWithType) {
					        hostsList = addToListUnique(hostsList,host);
					    }
					}
                }
            }
            // by path-with-type aggregates
            else {
                for(String type:groupingTypeList) {
                    List<String> pathsWithTypeList = collectorDAO.getPathsForType(type);
                    
                    if(pathsWithTypeList == null)
                        continue;
                    
                    for(String pathWithType:pathsWithTypeList) {
                        
                        if(!pathsList.contains(pathWithType)) {
                            continue;
                        }
                        
                        List<String> hostsInPathWithType = collectorDAO.getHostsForPathWithType(pathWithType, type);
                       	     
                        if(showHostTablesWithAggregates) {
                            for(String host:hostsInPathWithType) {
                                hostsList = addToListUnique(hostsList,host);
                            }
                        }
                        
                        if(log.isDebugEnabled())
                            log.debug("adding table bean by path '" + pathWithType + "' with type '" + type + "'");
                        
                        DashboardTableBean pathWithTypeBean;
                        
                        if(showGlobalZoneValues) {
                        	pathWithTypeBean = tableBeanFactory.getTableBeanByArgs(contextManager,DASHBOARD_TABLE_CACHE_FAST_TIMEOUT,DASHBOARD_TABLE_BEAN_BY_PATH_WITH_TYPE,
                        															                    pathWithType,type,hostsInPathWithType.size(),showAllDataValues,
                                                                                                        false,showSpecialDebugValues,TABLE_CACHE_SIGNATURE_DIFFERENTIATOR_1);
                            log.debug("adding relatedGlobalZones data for pathWithType: " + pathWithType + "->" + type);
                            DashboardTableBean globalZonesBean = tableBeanFactory.getTableBeanByArgs(contextManager,DASHBOARD_TABLE_CACHE_FAST_TIMEOUT,
                            																			DASHBOARD_TABLE_BEAN_BY_PATH_WITH_TYPE,
                        																				pathWithType,type,hostsInPathWithType.size(),showAllDataValues,
                                                                                                        true,showSpecialDebugValues);
                            pathWithTypeBean.joinChildTable(globalZonesBean);
                        }
                        else {
                        	pathWithTypeBean = tableBeanFactory.getTableBeanByArgs(contextManager,DASHBOARD_TABLE_CACHE_FAST_TIMEOUT,DASHBOARD_TABLE_BEAN_BY_PATH_WITH_TYPE,
                        															                    pathWithType,type,hostsInPathWithType.size(),showAllDataValues,
                                                                                                        false,showSpecialDebugValues);
                        }
                        
                        tableBeans.add(pathWithTypeBean);
                    }
                }
            }
            
            // load in all hosts requested 
            if(hostsList != null && hostsList.size() > 0) {
                
                GalaxyStatusManager galaxyStatusManager = null;
                if(showGlobalZoneValues) {
                    galaxyStatusManager = contextManager.getGalaxyStatusManager();
                }
                
                Collections.sort(hostsList);
                for(String host:hostsList) {
                    
                    if(log.isDebugEnabled())
                        log.debug("adding table bean by host '" + host + "'");
                    
                    
                    DashboardTableBean hostBean;
                    
                    if(showGlobalZoneValues) {
                        hostBean = tableBeanFactory.getTableBeanByArgs(contextManager,DASHBOARD_TABLE_CACHE_FAST_TIMEOUT,DASHBOARD_TABLE_BEAN_BY_HOST,
                                                                            host,showAllDataValues,false,
                                                                            showSpecialDebugValues,TABLE_CACHE_SIGNATURE_DIFFERENTIATOR_1);
                        String relatedGlobalZone = galaxyStatusManager.getGlobalZone(host);
                        if(relatedGlobalZone != null && !relatedGlobalZone.equals(host)) {
                            log.debug("adding relatedGlobalZone data for '" + relatedGlobalZone + "'");
                            DashboardTableBean globalZoneBean = tableBeanFactory.getTableBeanByArgs(contextManager,DASHBOARD_TABLE_CACHE_FAST_TIMEOUT,DASHBOARD_TABLE_BEAN_BY_HOST,
                            																			relatedGlobalZone,showAllDataValues,true,showSpecialDebugValues);
                            hostBean.joinChildTable(globalZoneBean);
                        }
                    }
                    else {
                        hostBean = tableBeanFactory.getTableBeanByArgs(contextManager,DASHBOARD_TABLE_CACHE_FAST_TIMEOUT,DASHBOARD_TABLE_BEAN_BY_HOST,
                                                                        host,showAllDataValues,false,showSpecialDebugValues);
                    }
                    
                	tableBeans.add(hostBean);
                }
            }
            
        }
        catch(DashboardCollectorDAOException ddEx) {
            log.warn(ddEx);
            this.lastException = ddEx;
        }
        finally {
            incrementTableGroupRequestCount();
            updateTableGroupRequestTotalMs(System.currentTimeMillis() - startMs);
        }
        
        return tableBeans;
    }
    
    public Exception getLastException() {
        return lastException;
    }
    
    private void incrementTableGroupRequestCount() {
        tableBeanFactory.incrementTableGroupRequestCount();
    }
    
    private void updateTableGroupRequestTotalMs(long updateMs) {
    	tableBeanFactory.updateTableGroupRequestTotalMs(updateMs);
    }
}
