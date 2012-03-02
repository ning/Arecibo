/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.agent.datasource.snmp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import org.skife.config.TimeSpan;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.snmp.SNMPConfig;
import com.ning.arecibo.agent.datasource.DataSource;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.agent.datasource.DataSourceType;

import com.ning.arecibo.util.Logger;


public final class SNMPDataSource implements DataSource
{
	private static final Logger log = Logger.getLogger(SNMPDataSource.class);

	private final String host;
	private final int port;
	private final String community;
	private final TimeSpan timeout;
	private final String snmpCompiledMibDir;
	private final HashMap<String,SortedMap<Integer,Object>> cachedTableColumns;
	
	private volatile SNMPClient snmpClient;
	private volatile List<String> mibList;
	private volatile List<String> oidList;
	private volatile Map<String,String> configHashKeyMap;
	private volatile String[] oidArray;
	
	public SNMPDataSource(Config config, TimeSpan timeout, String snmpCompiledMibDir)
        throws DataSourceException
	{
        if(!(config instanceof SNMPConfig)) {
            throw new DataSourceException("SNMPDataSource must be initialized with an instance of SNMPConfig");
        }
        SNMPConfig snmpConfig = (SNMPConfig)config;

		this.host = snmpConfig.getHost();
		this.port = snmpConfig.getPort();
		this.timeout = timeout;
		this.snmpCompiledMibDir = snmpCompiledMibDir;
		this.snmpClient = null;

        this.community = snmpConfig.getCommunityString();

		cachedTableColumns = new HashMap<String,SortedMap<Integer,Object>>();
	}

    @Override
	public synchronized void initialize()
		throws DataSourceException
	{
		try {
			if(this.snmpClient == null) {
				this.snmpClient = new SNMPClient(this.host, this.port, this.community, this.timeout, this.snmpCompiledMibDir);
				this.mibList = new ArrayList<String>();
				this.oidList = new ArrayList<String>();
				this.configHashKeyMap = new HashMap<String,String>();
			}
		}
		catch (RuntimeException e) {
			log.info(e, "Unable to intialize SNMP Client for %s", this.host);
			closeResources();

			// throw this out to the caller, which will handle and report it
			throw new DataSourceException("RuntimeException: ",e);
		}
	}

    @Override
    public synchronized boolean isInitialized() {
        return this.snmpClient != null;
    }

    @Override
	public synchronized void closeResources()
		throws DataSourceException
	{
		if(this.snmpClient != null) {
            try {
			    this.snmpClient.close();
            }
            finally {
			    this.snmpClient = null;
            }
		}
	}

    @Override
    public boolean canExpandConfigs() {
        return false;
    }

    @Override
    public Map<String, Config> expandConfigs(Map<String, Config> configs) {
        throw new IllegalStateException("expandConfigs not supported");
    }

    @Override
	public synchronized void prepareConfig(Config config)
		throws DataSourceException
	{
        SNMPConfig snmpConfig;
        if(config instanceof SNMPConfig) {
            snmpConfig = (SNMPConfig)config;
        }
        else {
            // shouldn't happen
            throw new DataSourceException("Passed in config is not an instanceof SNMPConfig");
        }

		String mib = snmpConfig.getMib();
		String oidName = snmpConfig.getOidName();
		
		try {
			Integer.parseInt(snmpConfig.getOidRow());	  // confirm it's an int, but leave as string
		}
		catch(NumberFormatException nfEx) {
			throw new DataSourceException("NumberFormatException: ",nfEx);
		}
		
		String attributeOid = oidName + "." + snmpConfig.getOidRow();
		this.oidList.add(attributeOid);
		this.configHashKeyMap.put(attributeOid,snmpConfig.getConfigHashKey());
		
		if(!mibList.contains(mib)) {
			snmpClient.loadMib(mib);
			mibList.add(mib);
		}

		// try to discover the data type for this config (may want to make this explicit in monitoring profile)
		if(this.snmpClient.isCounterDataType(mib,attributeOid)) {
			log.info("setting counterOverride flag for OID '" + attributeOid + "', for config: " + snmpConfig.getConfigHashKey());
			snmpConfig.setCounterOverride(true);
		}
		else {
			log.info("NOT setting counterOverride flag for OID '" + attributeOid + "', for config: " + snmpConfig.getConfigHashKey());
		}
	}

    @Override
	public synchronized void finalizePreparation()
	{
	    oidArray = new String[oidList.size()];
	    
	    int i=0;
	    for(String oid:oidList) {
	    	oidArray[i++] = oid;
	    }
	}


	
	public synchronized void loadMib(String mib)
		throws DataSourceException
	{
		// make sure the mib is loaded
		snmpClient.loadMib(mib);
	}

    @Override
	public synchronized Map<String, Object> getValues()
		throws DataSourceException
	{
		return getValues(oidArray,false);
	}

	public synchronized Map<String, Object> getValues(String[] oidArray) 
		throws DataSourceException
	{
		return getValues(oidArray,true);
	}
	
	private synchronized Map<String, Object> getValues(String[] oidArray,boolean returnOidsAsKeys)
		throws DataSourceException
	{
		try {
			if (this.snmpClient == null) {
				throw new DataSourceException("Client not initialized.");
			}

			// get values from snmp host
			Map<String, Object> rawResults = this.snmpClient.getValues(oidArray);

			// convert them to attribute/value pairs
			Map<String, Object> results = new HashMap<String, Object>();
			for (Map.Entry<String, Object> entry : rawResults.entrySet()) {
			
				//log.debug("entry.getKey() = '%s', entry.getValue() = '%s'",entry.getKey(),entry.getValue());
			
				if(returnOidsAsKeys) {
					results.put(entry.getKey(),entry.getValue());
				}
				else {
					results.put(configHashKeyMap.get(entry.getKey()), entry.getValue());
				}
			}
			return results;
		}
		catch(DataSourceException dsEx) {
			log.info(dsEx, "Unable to getValues for SNMP Client for %s", this.host);
			closeResources();

			// throw this out to the caller, which will handle and report it
			throw dsEx;
		}
	}
	
	public synchronized SortedMap<Integer,Object> getTableColumnValuesViaCache(String tableColumnOID)
		throws DataSourceException
	{
		if(cachedTableColumns.containsKey(tableColumnOID)) {
			return cachedTableColumns.get(tableColumnOID);
		}
		
		if (this.snmpClient == null) {
			throw new DataSourceException("Client not initialized.");
		}
		
		SortedMap<Integer,Object> tableColumnValues = this.snmpClient.getTableColumnValues(tableColumnOID);
		
		cachedTableColumns.put(tableColumnOID,tableColumnValues);
		return tableColumnValues;
	}
	
	public synchronized Object getTableColumnValueViaCache(String tableColumnOID,int row) 
		throws DataSourceException
	{
		SortedMap<Integer,Object> tableColumnValues = getTableColumnValuesViaCache(tableColumnOID);
		if(tableColumnValues != null)
			return tableColumnValues.get(row);
		
		return null;
	}
	
	public synchronized Object getValueViaCache(String oid) 
		throws DataSourceException
	{
		int divider = oid.lastIndexOf('.');
		
		if(divider == -1) {
			// treat it as a single scalar OID, with one row (0)
			return getTableColumnValueViaCache(oid,0);
		}
		
		String tableColumnOID = oid.substring(0,divider);
		String rowString = oid.substring(divider+1);
		
		int row;
		try{
			row = Integer.parseInt(rowString);
		}
		catch(NumberFormatException nfEx) {
			throw new DataSourceException("Couldn't parse row number from string '" + oid + "'",nfEx);
		}
		
		return getTableColumnValueViaCache(tableColumnOID,row);
	}
	
	public synchronized void clearTableColumnCache() {
		cachedTableColumns.clear();
	}

	public static boolean matchesConfig(Config config) {
        return config instanceof SNMPConfig;
	}

	@Override
	public DataSourceType getDataSourceType() {
		return DataSourceType.SNMP;
	}
}
