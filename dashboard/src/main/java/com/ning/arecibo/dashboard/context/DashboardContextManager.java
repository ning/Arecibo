package com.ning.arecibo.dashboard.context;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;
import com.ning.arecibo.dashboard.alert.AlertStatusManager;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAO;
import com.ning.arecibo.dashboard.dao.DashboardCollectorDAOKeepAliveManager;
import com.ning.arecibo.dashboard.format.DashboardFormatManager;
import com.ning.arecibo.dashboard.format.DashboardGraphFormatter;
import com.ning.arecibo.dashboard.format.DashboardTableFormatter;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;
import com.ning.arecibo.dashboard.table.DashboardTableBeanFactory;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.dashboard.context.DashboardContextUtils.DEBUG_MODE_PARAM;
import static com.ning.arecibo.dashboard.context.DashboardContextUtils.SHOW_ALL_DATA_VALUES;
import static com.ning.arecibo.dashboard.context.DashboardContextUtils.SHOW_GLOBAL_ZONE_VALUES;

public class DashboardContextManager
{
    private final static Logger log = Logger.getLogger(DashboardContextManager.class);
    
    public final static String CONTEXT_COLLECTOR_DAO = "collectorDAO";
    public final static String CONTEXT_TABLE_BEAN_FACTORY = "tableBeanFactory";
    public final static String CONTEXT_FORMAT_MANAGER = "dashboardFormatManager";
    public final static String CONTEXT_GALAXY_STATUS_MANAGER = "galaxyStatusManager";
    public final static String CONTEXT_COLLECTOR_DAO_KEEP_ALIVE_MANAGER = "collectorDAOKeepAliveManager";
    public final static String CONTEXT_ALERT_STATUS_MANAGER = "alertStatusManager";
    public final static String CONTEXT_MBEAN_MANAGER = "mbeanManager";
    public final static String CONTEXT_E2EZ_CONFIG_MANAGER = "e2ezConfigManager";
    public final static String CONTEXT_E2EZ_STATUS_MANAGER = "e2ezStatusManager";

    protected final Map<String,String[]> parameterMap;
    protected final ServletContext servletContext;
    protected final String contextPath;
    protected final boolean showDebugMode;
    protected final boolean showAllDataValues;
    protected final boolean showGlobalZoneValues;

    public DashboardContextManager(HttpServletRequest request) {
        
        this.parameterMap = request.getParameterMap();
        this.servletContext = request.getSession().getServletContext();
        this.contextPath = request.getContextPath();
        this.showDebugMode = (getPrimaryParamValue(DEBUG_MODE_PARAM) != null);
        this.showAllDataValues = (getPrimaryParamValue(SHOW_ALL_DATA_VALUES) != null);
        this.showGlobalZoneValues = (getPrimaryParamValue(SHOW_GLOBAL_ZONE_VALUES) != null);
    }
    
    public DashboardContextManager(ServletContext servletContext) {
        this.servletContext = servletContext;
        this.parameterMap = null;
        this.contextPath = null;
        this.showDebugMode = false;
        this.showAllDataValues = false;
        this.showGlobalZoneValues = false;
    }
    
    public String getPrimaryParamValue(String key) {
        String[] tempParamArray = parameterMap.get(key);
        if(tempParamArray != null && tempParamArray.length > 0)
            return tempParamArray[0];
        else
            return null;
    }
    
    /*
     * these don't work, the map is apparently immutable
    public void removeParam(String key) {
        this.parameterMap.remove(key);
    }
    
    public void setParam(String key,String value) {
        String[] valueArray = new String[1];
        valueArray[0] = value;
        this.parameterMap.put(key,valueArray);
    }
    */
    
    public Map<String,String[]> getParameterMap() {
        return parameterMap;
    }
    
    public String getContextPath() {
        return contextPath;
    }
    
    public DashboardCollectorDAO getCollectorDAO() {
        return (DashboardCollectorDAO)servletContext.getAttribute(CONTEXT_COLLECTOR_DAO);
    }
    
    public void setCollectorDAO(DashboardCollectorDAO collectorDAO) {
        servletContext.setAttribute(CONTEXT_COLLECTOR_DAO,collectorDAO);
    }
    
    public int[] getDataReductionFactors() {
    	DashboardCollectorDAO dao = getCollectorDAO();
    	if(dao == null) {
    		return new int[]{1};
    	}
    	else
    		return dao.getReductionFactors();
    }
    
    public DashboardTableBeanFactory getTableBeanFactory() {
        return (DashboardTableBeanFactory)servletContext.getAttribute(CONTEXT_TABLE_BEAN_FACTORY);
    }
    
    public void setTableBeanFactory(DashboardTableBeanFactory tableBeanFactory) {
        servletContext.setAttribute(CONTEXT_TABLE_BEAN_FACTORY,tableBeanFactory);
    }
    
    public DashboardTableFormatter getTableFormatter() {
        return (DashboardTableFormatter)servletContext.getAttribute(CONTEXT_FORMAT_MANAGER);
    }
    
    public DashboardGraphFormatter getGraphFormatter() {
        return (DashboardGraphFormatter)servletContext.getAttribute(CONTEXT_FORMAT_MANAGER);
    }
    
    public void setFormatManager(DashboardFormatManager formatManager) {
        servletContext.setAttribute(CONTEXT_FORMAT_MANAGER,formatManager);
    }

    public GalaxyStatusManager getGalaxyStatusManager() {
        return (GalaxyStatusManager)servletContext.getAttribute(CONTEXT_GALAXY_STATUS_MANAGER);
    }

    public void setGalaxyStatusManager(GalaxyStatusManager galaxyStatusManager) {
        servletContext.setAttribute(CONTEXT_GALAXY_STATUS_MANAGER,galaxyStatusManager);
    }

    public DashboardCollectorDAOKeepAliveManager getCollectorDAOKeepAliveManager() {
        return (DashboardCollectorDAOKeepAliveManager)servletContext.getAttribute(CONTEXT_COLLECTOR_DAO_KEEP_ALIVE_MANAGER);
    }
    
    public void setCollectorDAOKeepAliveManager(DashboardCollectorDAOKeepAliveManager collectorDAOKeepAliveManager) {
        servletContext.setAttribute(CONTEXT_COLLECTOR_DAO_KEEP_ALIVE_MANAGER,collectorDAOKeepAliveManager);
    }
    
    public AlertStatusManager getAlertStatusManager() {
        return (AlertStatusManager)servletContext.getAttribute(CONTEXT_ALERT_STATUS_MANAGER);
    }
    
    public void setAlertStatusManager(AlertStatusManager alertStatusManager) {
        servletContext.setAttribute(CONTEXT_ALERT_STATUS_MANAGER,alertStatusManager);
    }
    
    public ContextMbeanManager getMbeanManager() {
        return (ContextMbeanManager)servletContext.getAttribute(CONTEXT_MBEAN_MANAGER);
    }
    
    public void setMbeanManager(ContextMbeanManager mbeanManager) {
        servletContext.setAttribute(CONTEXT_MBEAN_MANAGER,mbeanManager);
    }

    public boolean getShowDebugMode() {
        return showDebugMode;
    }

    public boolean getShowAllDataValues() {
        return showAllDataValues;
    }

    public boolean getShowGlobalZoneValues() {
        return showGlobalZoneValues;
    }
}
