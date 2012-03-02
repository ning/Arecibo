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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.skife.config.TimeSpan;

import com.adventnet.snmp.beans.SnmpTarget;
import com.adventnet.snmp.mibs.LeafSyntax;
import com.adventnet.snmp.mibs.MibException;
import com.adventnet.snmp.mibs.MibModule;
import com.adventnet.snmp.mibs.MibOperations;
import com.adventnet.snmp.snmp2.SnmpAPI;
import com.adventnet.snmp.snmp2.SnmpVar;
import com.adventnet.snmp.snmp2.SnmpVarBind;
import com.adventnet.snmp.snmp2.SnmpOID;
import com.adventnet.snmp.snmp2.SnmpCounter64;
import com.adventnet.snmp.snmp2.SnmpInt;
import com.adventnet.snmp.snmp2.SnmpUnsignedInt;
import com.adventnet.snmp.snmp2.SnmpString;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.util.Logger;


final class SNMPClient
{
	private static final Logger log = Logger.getLogger(SNMPClient.class);

	private static final int MIB_LOAD_RETRIES = 3;
	private static final String MIB_RESOURCE_DIR = "mibs";
	
	private static final String[] mibFileExtensions = {"txt","my"};

	private final String host;
	private final int port;
	private final String community;
	private final TimeSpan timeout;
	private final String compiledMibWorkDir;
	
	private final SnmpTarget snmpTarget;

	public SNMPClient(String host, int port, String community, TimeSpan timeout, String compiledMibWorkDir) throws IllegalArgumentException
	{
		try {
			if (community.equals("")) {
				log.info("SNMP community is blank. Is that what you intended?");
			}
			
			this.host = host;
			this.port = port;
			this.community = community;
			this.timeout = timeout;
			this.compiledMibWorkDir = compiledMibWorkDir;

			// create connection
			this.snmpTarget = new SnmpTarget();
			this.snmpTarget.setLoadFromCompiledMibs(true);
			this.snmpTarget.setSnmpVersion(SnmpTarget.VERSION2C);
			this.snmpTarget.setCommunity(this.community);
			this.snmpTarget.setTargetHost(this.host);
			this.snmpTarget.setTargetPort(this.port);
			this.snmpTarget.setTimeout((int)(this.timeout.getMillis() / 1000l));
			this.snmpTarget.setMibPath(this.compiledMibWorkDir);
		}
		catch(RuntimeException ruEx) {
			close();
			throw ruEx;
		}
	}
	
	public void close() {
		if(this.snmpTarget != null)
			this.snmpTarget.releaseResources();
	}

	public void loadMib(String mibModule)
		throws DataSourceException
	{
		if (mibModule == null || mibModule.equals("")) {
			log.error("Error: blank mib string");   // TODO: implement blank MIB functionality
			return;
		}

		Stack<String> mibStack = new Stack<String>();
		mibStack.add(mibModule);
		while (!mibStack.isEmpty()) {
			loadMibAndDependencies(mibStack);  // may add files to mibStack
		}


	}

	private void loadMibAndDependencies(Stack<String> mibStack)
		throws DataSourceException
	{
		// TODO: the control flow of this is a bit clunky and difficult to follow, should be
		// cleaned up, unwrapped a bit....
		
		// this needs to be synchronized, it appears the snmpTarget class is not thread-safe....
		// plus, we are copying and deleting files, that might be needed by multiple threads....
		synchronized(SNMPClient.class) {
			String mibModule = mibStack.peek();
			
			log.debug("got " + mibModule);
	
			// first check to see if this mib has already been loaded
			// the snmp library keeps a global, vm wide list of mibs (but isn't thread safe!)
			String currMibModules = this.snmpTarget.getMibModules();
			StringTokenizer st = new StringTokenizer(currMibModules);
			while(st.hasMoreTokens()) {
				if(mibModule.equals(st.nextToken())) {
					log.debug("MIB '" + mibModule + "' is loaded, not reloading");
					mibStack.pop();
					return;
				}
			}
	
			String workSourceFilePath = copyResourceToFile(MIB_RESOURCE_DIR,mibModule,this.compiledMibWorkDir);
	
			
			// now load it
			// EOC-743: there can sometimes exist partially compiled files. If so, the load fails. Detect it here, erase the
			// cached .cds and .cmi files, and rebuild them.
			Exception thrownEx = null;
			int retries = 0;
			while (retries < MIB_LOAD_RETRIES) {
				try {
					this.snmpTarget.loadMibs(workSourceFilePath);
					break;
				}
				catch (MibException e) {
					thrownEx = e;
					
					log.info("MIB problem loading MIB file %s. Attempt %d.", workSourceFilePath, retries + 1);
					log.warn(e);
				}
				catch (FileNotFoundException e) {
					thrownEx = e;
					
					// sometimes one mib needs another mib to have been previously initialized first,
					// so we need to do the missing one first then retry
					String missingFilename = e.getMessage().substring(e.getMessage().lastIndexOf("/") + 1);
					String missingMibModule = missingFilename.replaceFirst("\\..$","");
					mibStack.push(missingMibModule);	
					
					log.info("Got FileNotFoundException for '%s'. Will first attempt missing file '%s'. Attempt %d.", 
																					workSourceFilePath, missingFilename, retries + 1);
					return;
				}
				catch (IOException e) {
					thrownEx = e;
					
					log.info("IO problem loading MIB file %s. Attempt %d.", workSourceFilePath, retries + 1);
					log.warn(e);
				}
				retries++;
				
				// remove any partially compiled mib files
				String cdsFilePath = this.compiledMibWorkDir + File.separator + mibModule + ".cds";
				String cmiFilePath = this.compiledMibWorkDir + File.separator + mibModule + ".cmi";
				removeFile(cdsFilePath);
				removeFile(cmiFilePath);
			}
	
			if (retries == MIB_LOAD_RETRIES) {
				throw new DataSourceException(String.format("Problem loading MIB file %s, maximum retries (%d) exceeded.", workSourceFilePath,MIB_LOAD_RETRIES),thrownEx);
			}
			
	
			// success
			log.info("Initialization successful loading MIB file %s", workSourceFilePath);
			mibStack.pop();
		}
	}

	private boolean removeFile(String fileName)
	{
		if (fileName.length() < 5 ) { return false; }

		File f = new File(fileName);
		if (!f.exists()) { return false; }

		if (!f.canWrite()) {
			log.error("Cached MIB file exists, but can't write it: '%s'", fileName);
			return false;
		}

		if (f.isDirectory()) {
			log.error("Cached MIB file exists, but is a directory: '%s'", fileName);
			return false;
		}

		// delete it
		return f.delete();
	}

	public Map<String, Object> getValues(String oids[])
		throws DataSourceException
	{
		Map<String, Object> results = new HashMap<String, Object>();

		// Set the OID List on the SnmpTarget instance
		this.snmpTarget.setObjectIDList(oids);

		// do a synchronous get request
		//Object resultList[] = this.snmpTarget.snmpGetList();
		SnmpVar[] resultList = this.snmpTarget.snmpGetVariables();

		if (resultList == null || this.snmpTarget.getErrorCode() != 0) {
			String errorString = "Request failed: '" + 
								this.snmpTarget.getErrorString() +
								"' for oid list: " + 
								join(oids);
			throw new DataSourceException(errorString);
		}

		for (int i = 0; i < oids.length; i++) {
			results.put(oids[i], convertToStandardJavaObject(resultList[i]));
		}
		
		return results;
	}
	
	public SortedMap<Integer,Object> getTableColumnValues(String tableColumnOidString) 
	{
		TreeMap<Integer,Object> resMap = new TreeMap<Integer,Object>();
		SnmpOID tableColumnOID = this.snmpTarget.getMibOperations().getSnmpOID(tableColumnOidString);
		
		// this is just the max, actual number returned might be less, dependendent on the device, etc.
		this.snmpTarget.setMaxRepetitions(100);
		this.snmpTarget.setObjectIDList(new String[] {tableColumnOidString});
		
		boolean done = false;
		do {
			
			// get next batch of data
			SnmpVarBind[][] resultArray = this.snmpTarget.snmpGetBulkVariableBindings();
			
			if(resultArray == null || resultArray.length == 0)
				break;
			
			for(SnmpVarBind[] results:resultArray) {
				for(SnmpVarBind result:results) {
					
					SnmpOID oid = result.getObjectID();
					if(oid == null || tableColumnOID == null || !SnmpTarget.isInSubTree(tableColumnOID, oid)) {
						// bail if we are off the end of this table
						done = true;
						break;
					}
					
					int[] oidNumbers = oid.toIntArray();
					Integer rowNum = oidNumbers[oidNumbers.length-1];
					
					SnmpVar var = result.getVariable();
					Object resultObj = convertToStandardJavaObject(var);
					
					resMap.put(rowNum, resultObj);
				}
				if(done)
					break;
			}	
		} while(!done);
		
		return resMap;
	}
	
	private Object convertToStandardJavaObject(Object snmpVar) {
		
		if(snmpVar == null)
			// shouldn't happen
			return null;
		
		if(snmpVar instanceof SnmpCounter64) {
			return ((SnmpCounter64)snmpVar).toBigInteger();
		}
		else if(snmpVar instanceof SnmpInt) {
			return (Integer)((SnmpInt)snmpVar).intValue();
		}
		else if(snmpVar instanceof SnmpUnsignedInt) {
			return (Long)((SnmpUnsignedInt)snmpVar).longValue();
		}
		else if(snmpVar instanceof SnmpString) {
			return (String)((SnmpString)snmpVar).toString();
		}
		else
			// could be SnmpNull
			return null;
	}

	public boolean isCounterDataType(String mib,String attributeOid) {
		
		MibOperations mibOps = this.snmpTarget.getMibOperations();
		MibModule mibMod = mibOps.getMibModule(mib);
		SnmpOID snmpOid = mibMod.getSnmpOID(attributeOid);
		
		if(snmpOid == null)
			return false;
		
		LeafSyntax leaf = mibMod.getLeafSyntax(snmpOid);
		
		if(leaf == null)
			return false;
		
		byte dataTypeb = leaf.getType();
		
		if(dataTypeb == SnmpAPI.COUNTER || dataTypeb == SnmpAPI.COUNTER64)
			return true;
		else
			return false;
	}

	private String join(String[] strings)
	{
		if (strings.length == 0) { return ""; }
		String join = strings[0];
		for (int i = 1; i < strings.length; i++) {
			join += ", " + strings[i];
		}
		return join;
	}

	private static String copyResourceToFile(String resourceDir, String fromResource, String toFileDir)
	{
		try {
			
			ClassLoader cLoader = Thread.currentThread().getContextClassLoader();
			
			String resourcePath = resourceDir + File.separator + fromResource;
			
			// try different file extensions to find the full file resource
			String resourceExtension = null;
			InputStream fromStream = null;
			for(String extension:mibFileExtensions) {
				String resourceFile = resourcePath + "." + extension;
				fromStream = cLoader.getResourceAsStream(resourceFile);	
				if(fromStream != null) {
					resourceExtension = extension;
					break;
				}
			}
			
			if(fromStream == null) {
				throw new IOException("Could not find resource " + resourcePath);
			}
			
			File toFileDirFile = new File(toFileDir);
			if(!toFileDirFile.exists()) {
				if(!toFileDirFile.mkdirs()) {
                    throw new IOException("call to mkdirs, to make intermediate directories failed");
                }
			}
			File toFile = new File(toFileDirFile,fromResource + "." + resourceExtension);
				
			FileOutputStream toStream = new FileOutputStream(toFile);
			
			int numBytes;
			byte[] buf = new byte[1024];
			while((numBytes = fromStream.read(buf)) > 0) {
				toStream.write(buf,0,numBytes);
			}
			
			fromStream.close();
			toStream.flush();
			toStream.close();
			
			return toFile.getPath();
		}
		catch (IOException e) {
			throw new RuntimeException(String.format("IOException when copying from resource '%s/%s' to directory '%s'", resourceDir,fromResource,toFileDir),e);
		}
	}

}
