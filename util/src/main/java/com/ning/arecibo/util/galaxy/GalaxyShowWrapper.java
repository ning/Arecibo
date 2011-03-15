package com.ning.arecibo.util.galaxy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import com.ning.arecibo.util.Logger;

public class GalaxyShowWrapper
{
	private static final Logger log = Logger.getLogger(GalaxyShowWrapper.class);	
	
	private static final long ONE_SECOND = 1000L;
	
	public static List<GalaxyCoreStatus> getCoreStatusList(String gonsoleURL,
	                                                       String galaxyCommandPath,
	                                                       int galaxyCommandTimeout,
	                                                       String coreTypeFilter,
	                                                       String globalZoneFilter,
	                                                       String localZoneFilter) throws GalaxyShowWrapperException
    {
		ArrayList<String> processArgs = new ArrayList<String>();
		
		processArgs.add(galaxyCommandPath);
		
		if(gonsoleURL != null) {
		    processArgs.add("show");
		    processArgs.add("-c");
		    processArgs.add(gonsoleURL);
		}
		else {
		    processArgs.add("show");
		}
		
		// Note, only 1 of the filters can be applied, and they have a precedence order: coreType, globalZone, localZone
		StringTokenizer st = null;
		if(coreTypeFilter != null && coreTypeFilter.length() > 0) {
		    processArgs.add("-t");
		    st = new StringTokenizer(coreTypeFilter,",");
		}
		else if(globalZoneFilter != null && globalZoneFilter.length() > 0) {
		    processArgs.add("-m");
		    st = new StringTokenizer(globalZoneFilter,",");
		}
		else if(localZoneFilter != null && localZoneFilter.length() > 0) {
			
			// see if we should apply a range filter
			if(localZoneFilter.contains(":")) {
				// do call with no filters
				List<GalaxyCoreStatus> currStatii = callGalaxyProcess(processArgs,galaxyCommandTimeout);
				
				String minZone = null;
				String maxZone = null;
				st = new StringTokenizer(localZoneFilter,":");
				if(st.hasMoreTokens())
					minZone = st.nextToken();
				if(st.hasMoreTokens())
					maxZone = st.nextToken();
				
				List<GalaxyCoreStatus> retStatii = new ArrayList<GalaxyCoreStatus>();
				if(minZone != null && maxZone != null) {
					for(GalaxyCoreStatus currStatus:currStatii) {
						String host = currStatus.getZoneHostName();
						if(host.compareTo(minZone) >= 0 && host.compareTo(maxZone) <= 0)
							retStatii.add(currStatus);
					}
					
					return retStatii;
				}
			}
			
		    processArgs.add("-i");
		    st = new StringTokenizer(localZoneFilter,",");
		}
		else {
		    // no filters need to be applied
		    return callGalaxyProcess(processArgs,galaxyCommandTimeout);
		}
		
		// loop through separate iterations for each filter applied
		// galaxy may be updated to allow multiple args to be applied in a single call, in which case this won't be necessary
        List<GalaxyCoreStatus> retStatii = null;
        int baseArgCount = processArgs.size();
        while(st.hasMoreTokens()) {
            if(processArgs.size() > baseArgCount)
                processArgs.set(baseArgCount, st.nextToken());
            else
                processArgs.add(st.nextToken());
            
            List<GalaxyCoreStatus> currStatii = callGalaxyProcess(processArgs,galaxyCommandTimeout);
            
            if(retStatii == null)
                retStatii = currStatii;
            else
                retStatii.addAll(currStatii);
        }
        
        return retStatii;
	}
	
	private static List<GalaxyCoreStatus> callGalaxyProcess(ArrayList<String> processArgs,int galaxyCommandTimeout) throws GalaxyShowWrapperException {
		
        try {
            long timeoutMillis = (long)galaxyCommandTimeout * ONE_SECOND;
            
    		ProcessBuilder pb;
    		pb = new ProcessBuilder(processArgs);
    		
    		StringBuilder sb = new StringBuilder();
    		for(String processArg:processArgs) {
    		    sb.append(processArg + " ");
    		}
    		log.info("Making system call:  " + sb.toString());
    		
    		pb.redirectErrorStream(true);
    		Process p = pb.start();
    		
    		_streamReader stdoutReader = new _streamReader(p.getInputStream());
    		
            Thread stdoutReadThread = new Thread(stdoutReader);
            stdoutReadThread.start();
            
            // guard against the process hanging indefinitely
            Timer processInterruptTimer = new Timer();
            processInterruptTimer.schedule(new _processInterrupter(p),timeoutMillis);
            
            try {
                log.info("Waiting for galaxy process to complete");
                
            	int processExitValue = p.waitFor();
                log.info("Galaxy show command exited with exit value '" + processExitValue + "'");
            
            	processInterruptTimer.cancel();
            	
            	// note, galaxy exits normally with an exitValue = 1 if there are no cores available (but there were no errors),
            	// we need to consider that a normal exit
            	if(processExitValue > 1) {
                	throw new GalaxyShowWrapperException("Galaxy command exited with unexpected status: " + processExitValue);
            	}
            }
            catch(InterruptedException intEx) {
                log.warn("Galaxy process interrupted");
                
                throw new GalaxyShowWrapperException("Galaxy command prematurely interrupted");
            }
            
            	
            try {
                log.info("Waiting for galaxy stdout stream to end");
            	stdoutReadThread.join(timeoutMillis);
            	
            	if(stdoutReadThread.isAlive()) {
            	    throw new GalaxyShowWrapperException("Galaxy stdout stream interrupted");
            	}
            }
            catch(InterruptedException intEx) {
                log.warn("StdoutReadThread interrupted");
            }
            
            String stdOutString = stdoutReader.getInput();
        
        	if(stdOutString == null || stdOutString.length() == 0) {
        		log.warn("Got empty stdout output from galaxy show command");
        		return null;
        	}
        
        	StringReader sr = new StringReader(stdOutString);
        	return GalaxyStatusReader.getCoreStatusList(sr);
        }
        catch(IOException ioEx) {
		    throw new GalaxyShowWrapperException("Got IOException",ioEx);
        }
	}
	
	public static class _streamReader implements Runnable {
		
		InputStream is = null;
		StringBuilder sb = null;
		IOException ioEx = null;
		
		public _streamReader(InputStream is) {
			this.is = is;
		}
		
		public void run() {
			try {
				sb = new StringBuilder();
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
			
				int numRead = 0;
				char[] buf = new char[256];
				while((numRead = br.read(buf,0,256)) != -1) {
					sb.append(buf,0,numRead);
				}
				
				br.close();
				is.close();
			}
			catch(IOException ioEx) {
				this.ioEx = ioEx;
			}
		}
		
		public String getInput() throws IOException {
			
			if(this.ioEx != null)
				throw this.ioEx;
			
			if(sb == null)
				return null;
			
			return this.sb.toString();
		}
	}
	
	public static class _processInterrupter extends TimerTask  {
	    private Process process = null;
	    
	    public _processInterrupter(Process process) {
	        this.process = process;
	    }
	    
	    public void run() {
	        log.warn("Timeout expired, destroying process");
	        process.destroy();
	    }
	}
}