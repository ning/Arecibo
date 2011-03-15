package com.ning.arecibo.dashboard.table;

import java.util.Map;
import com.ning.arecibo.dashboard.context.DashboardTableContextManager;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAO;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAOException;
import com.ning.arecibo.dashboard.graph.DashboardGraphUtils.GraphType;

import com.ning.arecibo.util.Logger;
import static com.ning.arecibo.dashboard.context.DashboardContextUtils.*;

public class DashboardTableBeanByType extends DashboardTableBeanBase
{
    private final static Logger log = Logger.getLogger(DashboardTableBeanByType.class);
    
    private int numHosts = -1;
    private DashboardCollectorDAO collectorDAO = null;
    
    private String relatedDepType;
    private boolean initialized = false;
    
    public DashboardTableBeanByType(DashboardTableContextManager contextManager,String tableBeanSignature,Object... args) {
        
        int currArgIndex = 0;
        if(args.length > currArgIndex)
            this.depType = (String)args[currArgIndex];
        else
            throw new RuntimeException("Must provide type");
        
        currArgIndex++;
        if(args.length > currArgIndex) {
            if(args[currArgIndex] instanceof String)
                this.numHosts = Integer.parseInt((String)args[currArgIndex]);
            else if(Integer.class.isAssignableFrom(args[currArgIndex].getClass()))
                this.numHosts = (Integer)args[currArgIndex]; 
        }
        
        currArgIndex++;
        if(args.length > currArgIndex) {
            if(args[currArgIndex] instanceof String)
                this.showAllDataValues = Boolean.parseBoolean((String)args[currArgIndex]);
            else if(Boolean.class.isAssignableFrom(args[currArgIndex].getClass()))
                this.showAllDataValues = (Boolean)args[currArgIndex]; 
        }

        currArgIndex++;
        if(args.length > currArgIndex) {
            if(args[currArgIndex] instanceof String)
                this.limitToRelatedGlobalZoneData = Boolean.parseBoolean((String)args[currArgIndex]);
            else if(Boolean.class.isAssignableFrom(args[currArgIndex].getClass()))
                this.limitToRelatedGlobalZoneData = (Boolean)args[currArgIndex];
        }

        currArgIndex++;
        if(args.length > currArgIndex) {
            if(args[currArgIndex] instanceof String)
                this.showSpecialDebugValues = Boolean.parseBoolean((String)args[currArgIndex]);
            else if(Boolean.class.isAssignableFrom(args[currArgIndex].getClass()))
                this.showSpecialDebugValues = (Boolean)args[currArgIndex]; 
        }
        
        this.tableBeanSignature = tableBeanSignature;
        this.contextManager = contextManager;
        this.collectorDAO = contextManager.getCollectorDAO();
        this.formatter = contextManager.getTableFormatter();
    }
        
    public synchronized void initBean()
    {
        if(initialized)
            return;

        Map<String, Map<String, Object>> dbResults=null;
        
        if(!this.limitToRelatedGlobalZoneData) {
	        try {
	            dbResults = collectorDAO.getLastValuesForType(this.depType);
	        }
	        catch(DashboardCollectorDAOException ddEx) {
	            log.warn(ddEx);
	        }
	        
	        if(dbResults == null) {
	            log.info("No data retrieved for type = '" + this.depType + "'");
	            return;
	        }
        }
        else {
        	try {
        		//TODO: use better constants here, or perhaps this isn't what we want to see here...
        		dbResults = collectorDAO.getLastValuesForType(GLOBAL_ZONE_TYPE);
        	}
        	catch(DashboardCollectorDAOException ddEx) {
            	log.warn(ddEx);
        	}
        
        	if(dbResults == null || dbResults.size() == 0) {
            	log.info("No data retrieved for type '" + GLOBAL_ZONE_TYPE + "'");
            	return;
        	}
        } 
        
        buildFormattedDisplayTable(dbResults);
        
        initialized = true;
    }
    
    public String getTableTitle()
    {
    	if(numHosts > -1) {
    	    
            String hosts = (numHosts > 1)?"Hosts":"Host";
            
        	return TABLE_TITLE_H1_START + "Type: " + this.depType.toUpperCase() + TABLE_TITLE_LR + TABLE_TITLE_H1_END +
        				TABLE_TITLE_H2_START + 
        				"aggregated:" + getTableTitleTypeLink(contextManager,this.depType,numHosts + " " + hosts) + 
        				TABLE_TITLE_H2_END;
    	}
    	else {
        	return TABLE_TITLE_H1_START + "Type: " + this.depType.toUpperCase() + TABLE_TITLE_H1_END;
    	}
    } 
    
    public String getPlainTableTitle()
    {
    	if(numHosts > -1) {
    	    
            String hosts = (numHosts > 1)?"Hosts":"Host";
            
            return "Type: " + this.depType.toUpperCase() + ", aggregated: " + numHosts + " " + hosts;
    	}
    	else {
            return "Type: " + this.depType.toUpperCase();
    	}
    } 
    
    public GraphType getTableGraphType() {
    	if(this.limitToRelatedGlobalZoneData)
    		return GraphType.BY_GLOBAL_ZONE_LIST_FOR_TYPE;
    	else
    		return GraphType.BY_TYPE;
    }  
}
