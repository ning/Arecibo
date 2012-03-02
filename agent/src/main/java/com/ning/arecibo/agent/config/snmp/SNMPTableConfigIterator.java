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

package com.ning.arecibo.agent.config.snmp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.config.ConfigIterator;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.agent.datasource.snmp.SNMPDataSource;

import com.ning.arecibo.util.Logger;

public class SNMPTableConfigIterator implements ConfigIterator {
	private static final Logger log = Logger.getLogger(SNMPTableConfigIterator.class);
	
	private final static String scalarRefRE = "\\$\\{(\\w*)\\}";
	private final static String rowRefRE = "\\&\\{(\\w*)\\}";
	private final static String rowRangeRE = ".*table\\:row\\[(\\d+)\\.\\.(\\d+)\\].*";
	private final static String rowWhereClauseRE = ".*where\\[(\\d+)\\=\\=(\\d+)\\].*";
	
	private final static Pattern scalarRefPattern = Pattern.compile(scalarRefRE);
	private final static Pattern rowRefPattern = Pattern.compile(rowRefRE);
	private final static Pattern rowRangePattern = Pattern.compile(rowRangeRE);
	private final static Pattern rowWhereClausePattern = Pattern.compile(rowWhereClauseRE);
		
	private final SNMPConfig baseConfig;
	private final SNMPDataSource snmpDataSource;
	private final String oidName;
	private final String attributeWithScalarsResolved;
	
	
	private int rowRangeStart = -1;
	private int rowRangeEnd = -1;
	private int currRow = -1;
	
	public SNMPTableConfigIterator(SNMPConfig baseConfig,SNMPDataSource snmpDataSource)
		throws DataSourceException
	{
		this.baseConfig = (SNMPConfig)baseConfig;
		
		this.snmpDataSource = snmpDataSource;
		this.snmpDataSource.initialize();
		this.snmpDataSource.loadMib(baseConfig.getMib());
		this.oidName = baseConfig.getOidName();
		
		this.attributeWithScalarsResolved = resolveScalarReferences(baseConfig.getOidRow());
		
		resolveRowRange(this.attributeWithScalarsResolved);
	}
		
	private String resolveScalarReferences(String replaceString) 
		throws DataSourceException
	{
		
		// resolve any scalar reference variables
		log.debug("Looking for scalar reference values in attribute: '%s'",replaceString);
		
		Matcher matcher = scalarRefPattern.matcher(replaceString);
		
		int count = 0;
		while(matcher.find()) {
			String scalarOid = matcher.group(1);
			
			// scalars are always index 0
			String oid = scalarOid + ".0";
			
			log.debug("Found refValue '%s'",oid);
			
			Object value = snmpDataSource.getValueViaCache(oid);
			
			if(log.isDebugEnabled()) {
				log.debug("Polled: '" + oid + "' = '" + value);
			}
			
			if(value == null)
				// bail if can't resolve this one
				continue;
			
			replaceString = matcher.replaceAll(value.toString());
			
			log.debug("Replacing attribute: '" + replaceString);
			
			matcher.reset(replaceString);
		
			count++;
		}
		
		log.debug("Returning replacement string = %s", replaceString);
		return replaceString;
	} 
	
	private void resolveRowRange(String attributeString) {
		
		// resolve row range numbers in attributeString
		log.debug("Looking for range numbers in: '" + attributeString + "'");
		
		Matcher matcher = rowRangePattern.matcher(attributeString);
		
		if(matcher.matches()) {
			this.rowRangeStart = Integer.parseInt(matcher.group(1));
			this.rowRangeEnd = Integer.parseInt(matcher.group(2));
			
			log.debug("Parsed rowRangeStart = " + rowRangeStart);
			log.debug("Parsed rowRangeEnd = " + rowRangeEnd);
		}
		else {
			log.debug("Table row range not specified");
		}
	}
	
	private String resolveRowReferences(String replaceString,int row)
		throws DataSourceException
	{
		// resolve any scalar reference variables
		log.debug("Looking for row reference values in attribute: '%s'",replaceString);
		
		Matcher matcher = rowRefPattern.matcher(replaceString);
		
		int count = 0;
		while(matcher.find()) {
			String tableOID = matcher.group(1);
			
			log.debug("Found refValue tableOID '%s'",tableOID);
			String oid = tableOID + "." + row;
			
			Object value = snmpDataSource.getTableColumnValueViaCache(tableOID, row);
			
			if(log.isDebugEnabled()) {
				log.debug("Polled: '" + oid + "' = '" + value);
			}
			
			if(value != null) {
				replaceString = matcher.replaceAll(value.toString());
				log.debug("Replacing attribute: '" + replaceString);
			
				matcher.reset(replaceString);
		
				count++;
			}
			else {
				log.info("Got null value returned for OID " + oid);
				return null;
			}
		}
		
		// cleanup string
		replaceString = replaceString.replaceAll("[ ]", "");	
		replaceString = replaceString.replaceAll("[-+/]", "_");	
		log.debug("Returning replacement string = %s", replaceString);
		
		return replaceString;
	}
	
	private boolean isRowValid(int currRow,String rowAttribute)  {
		
		// resolve row range numbers in attributeString
		log.debug("checking if row is valid for row " + currRow + ": '" + rowAttribute + "'");
		
		if(rowAttribute == null)
			return false;
		
		boolean rowValid = true;
		
		Matcher matcher = rowWhereClausePattern.matcher(rowAttribute);
		
		if(matcher.matches()) {
			int lhs = Integer.parseInt(matcher.group(1));
			int rhs = Integer.parseInt(matcher.group(2));
			
			log.debug("Parsed lhs = " + lhs);
			log.debug("Parsed rhs = " + rhs);
			
			if(lhs != rhs)
				rowValid = false;
		}
		else {
			log.debug("No where clause to check");
		}
			
		return rowValid;
	}
		
	private boolean isRowPollable(int currRow) 
		throws DataSourceException
	{
		
		// resolve row range numbers in attributeString
		log.debug("checking if row is pollable for " + currRow);
		
		// verify that the OID actually has data
		String oid = this.oidName + "." + currRow;
		
		Object value = snmpDataSource.getTableColumnValueViaCache(this.oidName, currRow);
		
		if(log.isDebugEnabled()) {
			log.debug("Polled: '" + oid + "' = '" + value);
		}
		
		if(value == null) {
			log.debug("table row not pollable for " + oid);
			return false;
		}
		
		return true;
	}
		
	@Override
	public synchronized Config getNextConfig() {
		
		// see if this is the first pass
		if(currRow == -1) {
			if(rowRangeStart == -1) 
				// set to 1 if no range specified
				currRow = 1;
			else
				currRow = rowRangeStart;
		}
		
		// see if we're done already
		if(rowRangeEnd > -1 && currRow > rowRangeEnd)
			return null;
		
		boolean gotUnpollableRow = false;
		
		for(;(rowRangeEnd == -1 && !gotUnpollableRow) || currRow <= rowRangeEnd; currRow++)  {
			try {
				
				// get rowAttribute (e.g. table index)
				String rowAttribute = resolveRowReferences(this.attributeWithScalarsResolved,currRow);
				
				// make sure we resolved rowAttribute without any polling failures
				if(rowAttribute == null) {
					gotUnpollableRow = true;
					continue;
				}
				
				// check that it is valid if any where clauses, etc.
				if(!isRowValid(currRow,rowAttribute)) {
					continue;
				}
				
				// see if row is pollable
				if(!isRowPollable(currRow)) {
					gotUnpollableRow = true;
					continue;
				}
				
				String rowEventType = resolveRowReferences(this.baseConfig.getEventType(),currRow);
				if(rowEventType == null) {
					log.info("Could not retrieve valid eventType from " + this.baseConfig.getEventType() + " for row " + currRow + ", skipping");
					continue;
				}
				
				String rowEventAttributeType = resolveRowReferences(this.baseConfig.getEventAttributeType(),currRow);
				if(rowEventAttributeType == null) {
					log.info("Could not retrieve valid rowEventAttributeType from " + this.baseConfig.getEventAttributeType() + " for row " + currRow + ", skipping");
					continue;
				}
				
				
				log.debug("Creating new config: %s; %s; %s; %s",baseConfig.getOidRow(),currRow,rowEventType,rowEventAttributeType);
				
				Config newConfig = new SNMPConfig(baseConfig,rowEventType,rowEventAttributeType,
                        baseConfig.getMib(),baseConfig.getOidName(),Integer.toString(currRow));
				
				currRow++;
				return newConfig;
			}
			catch(DataSourceException e) {
				log.info(e,"Got exception preparing table row config");
				
				// if we don't have a defined rowRangeEnd, then assume we are off the end of the table and bail
				gotUnpollableRow = true;
			}
			catch(ConfigException e) {
				log.warn("Got exception preparing config for " + this.baseConfig.getEventType() + " for row " + currRow + ", skipping");
				log.info(e,"ConfigException:");
			}
		}
		
		return null;
	}
}
