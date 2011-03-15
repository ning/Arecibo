package com.ning.arecibo.dashboard.table;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.ning.arecibo.dashboard.context.DashboardContextUtils;
import com.ning.arecibo.dashboard.context.DashboardTableContextManager;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAO;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAOException;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.dashboard.context.DashboardContextUtils.*;
import static com.ning.arecibo.dashboard.graph.DashboardGraphUtils.GraphType;

public class DashboardTableBeanByHost extends DashboardTableBeanBase
{
    private final static Logger log = Logger.getLogger(DashboardTableBeanByHost.class);
    
    private DashboardCollectorDAO collectorDAO = null;
    private boolean initialized = false;
    
    public DashboardTableBeanByHost(DashboardTableContextManager contextManager,String tableBeanSignature,Object... args) {
        
        int currArgIndex = 0;
        if(args.length > currArgIndex)
            this.host = (String)args[currArgIndex];
        else
            throw new RuntimeException("Must provide host");
        
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
        if (args.length > currArgIndex) {
            if (args[currArgIndex] instanceof String)
                this.showSpecialDebugValues = Boolean.parseBoolean((String) args[currArgIndex]);
            else if (Boolean.class.isAssignableFrom(args[currArgIndex].getClass()))
                this.showSpecialDebugValues = (Boolean) args[currArgIndex];
        }
        
        this.tableBeanSignature = tableBeanSignature;
        this.contextManager = contextManager;
        this.collectorDAO = contextManager.getCollectorDAO();
        this.formatter = contextManager.getTableFormatter();
 
        this.depType = contextManager.getGalaxyStatusManager().getCoreType(host);
        this.depPath = contextManager.getGalaxyStatusManager().getConfigSubPath(host);
        
        /*
         if(this.depPath == null)
            this.depPath = DashboardContextUtils.UNSPECIFIED_PATH_NAME;
        */
        
        if(this.depPath == null)
        	this.discoverDepPath = true;
        if(this.depType == null)
        	this.discoverDepType = true;
    }
    
    public synchronized void initBean()
    {
        if(initialized)
            return;

        Map<String, Map<String, Object>> dbResults = null;
        
        try {
            dbResults = collectorDAO.getLastValuesForHost(host);
        }
        catch(DashboardCollectorDAOException ddEx) {
            log.warn(ddEx);
        }
        
        if(dbResults == null) {
            log.info("No data retrieved for host = '" + host + "'");
            return;
        }
        
        buildFormattedDisplayTable(dbResults);
        
        initialized = true;
    }
    
    public String getTableTitle()
    {
        if(depType != null) {
        	if(depPath != null) {
        		return TABLE_TITLE_H1_START + getTableTitleDebugStatusHostLink(contextManager, depType, host) + TABLE_TITLE_LR + TABLE_TITLE_H1_END +
        				TABLE_TITLE_H2_START + 
        				"type:" + getTableTitleTypeLink(contextManager,depType) + 
        				", scope:" + getTableTitlePathWithTypeLink(contextManager,depPath,depType) + 
        				TABLE_TITLE_H2_END +
        				TABLE_TITLE_H2_START + 
        				"Add to " + getTableTitleAdHocHostLink(contextManager,host,"Ad Hoc Host List") +
        				TABLE_TITLE_H2_START;
        	}
        	else {
        		return TABLE_TITLE_H1_START + getTableTitleDebugStatusHostLink(contextManager, depType, host) + TABLE_TITLE_LR + TABLE_TITLE_H1_END +
        				TABLE_TITLE_H2_START + 
        				"type:" + getTableTitleTypeLink(contextManager,depType) + 
        				TABLE_TITLE_H2_END +
        				TABLE_TITLE_H2_START + 
        				"Add to " + getTableTitleAdHocHostLink(contextManager,host,"Ad Hoc Host List") +
        				TABLE_TITLE_H2_START;
        	}
        }
        else
        	return TABLE_TITLE_H1_START + host + TABLE_TITLE_H1_END;
    }

    public String getPlainTableTitle()
    {
        if(depType != null) {
        	if(depPath != null) {
        		return "Host: " + host + ", type: " + depType + ", scope: " + depPath;
        	}
        	else {
        		return "Host: " + host + ", type: " + depType;
        	}
        }
        else
        	return "Host: " + host;
    }

	public GraphType getTableGraphType() {
        return GraphType.BY_HOST;
    }
}
