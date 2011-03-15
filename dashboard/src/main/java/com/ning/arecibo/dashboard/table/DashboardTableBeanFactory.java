package com.ning.arecibo.dashboard.table;

import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import com.google.inject.Inject;
import com.ning.arecibo.dashboard.context.DashboardTableContextManager;
import com.ning.arecibo.dashboard.guice.TableCacheSize;
import com.ning.arecibo.util.LRUCache;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;

public class DashboardTableBeanFactory
{
    private final static Logger log = Logger.getLogger(DashboardTableBeanFactory.class);
    
    public final static String DASHBOARD_TABLE_BEAN_BY_TYPE = "byType";
    public final static String DASHBOARD_TABLE_BEAN_BY_PATH_WITH_TYPE = "byPathWithType";
    public final static String DASHBOARD_TABLE_BEAN_BY_HOST = "byHost";
    
    public final static String SIGNATURE_DELIMITER = "&";
    
    public final static long DASHBOARD_TABLE_CACHE_FAST_TIMEOUT = 20L;
    public final static long DASHBOARD_TABLE_CACHE_LONG_TIMEOUT = 100000000L;
    
    private static AtomicLong tableGroupRequestCounter = new AtomicLong();
    private static AtomicLong tableGroupRequestTotalMs = new AtomicLong(); 
    private static AtomicLong tableBeanCacheHits = new AtomicLong();
    private static AtomicLong tableBeanCacheMisses = new AtomicLong();
    
    private final int tableCacheSize;
    
    private final LRUCache<String, CachedTableBeanWrapper> lruCache;
    
    @Inject
    public DashboardTableBeanFactory(@TableCacheSize int tableCacheSize) {
    	this.tableCacheSize = tableCacheSize;
    	lruCache = new LRUCache<String, CachedTableBeanWrapper>(this.tableCacheSize);    
    }
    
    public DashboardTableBean getTableBeanBySignature(DashboardTableContextManager contextManager,long timeoutSeconds,String tableBeanSignature) {
    	return getTableBean(contextManager,timeoutSeconds,tableBeanSignature,null);
    }
    
    public DashboardTableBean getTableBeanByArgs(DashboardTableContextManager contextManager,long timeoutSeconds,String tableType,Object... args) {
    	return getTableBean(contextManager,timeoutSeconds,null,tableType,args);
    }
    
    private DashboardTableBean getTableBean(DashboardTableContextManager contextManager,long timeoutSeconds,String tableBeanSignature,String tableType,Object... args) {
    	
    	String cacheKey;
    	if(tableBeanSignature != null)
    		cacheKey = tableBeanSignature;
    	else
    		cacheKey = getTableBeanSignatureKey(tableType,args);
    	
    	CachedTableBeanWrapper cachedBean;
    	
		boolean isNew = false;
		synchronized(lruCache) {
			cachedBean = lruCache.get(cacheKey);
			if ( cachedBean == null || cachedBean.testAge(timeoutSeconds, TimeUnit.SECONDS)) {
				cachedBean = new CachedTableBeanWrapper(cacheKey);
				lruCache.put(cacheKey, cachedBean);
				isNew = true ;
			}
		}	
		if(isNew) {
			tableBeanCacheMisses.getAndIncrement();
			log.debug("Creating tableBean for cacheKey %s", cacheKey);
			DashboardTableBean newBean;
			
			if(tableType == null) {
			    String[] stringArgs = parseArgsToStrings(tableBeanSignature);
			    
			    if(stringArgs.length > 0) {
			        tableType = stringArgs[0];
			        if(stringArgs.length > 1) {
			            args = new Object[stringArgs.length - 1];
			            for(int i = 0;i < args.length;i++) {
			                args[i] = stringArgs[i+1];
			            }
			        }
			    }
			}
        
			if(tableType == null) {
			    newBean = null;
			}
			else if(tableType.equals(DASHBOARD_TABLE_BEAN_BY_HOST)) {
	            newBean = new DashboardTableBeanByHost(contextManager,cacheKey,args);
	        }
	        else if(tableType.equals(DASHBOARD_TABLE_BEAN_BY_TYPE)) {
	            newBean = new DashboardTableBeanByType(contextManager,cacheKey,args);
	        }
	        else if(tableType.equals(DASHBOARD_TABLE_BEAN_BY_PATH_WITH_TYPE)) {
	            newBean = new DashboardTableBeanByPathWithType(contextManager,cacheKey,args);
	        }
	        else
	        	newBean = null;
			
			if(newBean != null)
			    newBean.initBean();
			
	        cachedBean.setTableBean(newBean);
	        return newBean;
		}
		else {
			log.debug("waiting for tableBean cacheKey %s", cacheKey);
			try {
				//TODO: Inject this timeout value
				if(cachedBean.waitFor(60, TimeUnit.SECONDS)) {
					tableBeanCacheHits.getAndIncrement();
					return cachedBean.getTableBean();
				}
				else {
					throw new IllegalStateException("Table bean cache timed out waiting for cacheKey '" + cacheKey + "'");
				}
			} catch(InterruptedException iEx) {
				throw new IllegalStateException(iEx);
			}
		}
    }
    
    private String[] parseArgsToStrings(String tableBeanSignature) {
        
        StringTokenizer st = new StringTokenizer(tableBeanSignature,SIGNATURE_DELIMITER); 
    	int argCount = st.countTokens();
    
    	String[] stringArgs = new String[argCount];
    	
    	int index = 0;
    	while(st.hasMoreTokens()) {
        	stringArgs[index++] = st.nextToken();
    	}
    	
    	return stringArgs;
    }
    
    public String getTableBeanSignatureKey(String tableType,Object... args) {
    	StringBuffer sb = new StringBuffer();
    	
    	sb.append(tableType);
    	for(Object arg:args) {
    		sb.append(SIGNATURE_DELIMITER);
    		sb.append(arg);
    	}
    	
    	return sb.toString();
    }
    
    public void incrementTableGroupRequestCount() {
    	tableGroupRequestCounter.getAndIncrement();
    }
    
    public void updateTableGroupRequestTotalMs(long millis) {
    	tableGroupRequestTotalMs.getAndAdd(millis);
    }
    
    @MonitorableManaged(monitored = true)
    public int getTableBeanCacheSize() {
    	return lruCache.size();
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getTableBeanCacheHits() {
    	return tableBeanCacheHits.get();
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getTableBeanCacheMisses() {
    	return tableBeanCacheMisses.get();
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getTableGroupRequestCount() {
        return tableGroupRequestCounter.get();
    }
    
    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getTableGroupRequestTotalMs() {
        return tableGroupRequestTotalMs.get();
    }
}
