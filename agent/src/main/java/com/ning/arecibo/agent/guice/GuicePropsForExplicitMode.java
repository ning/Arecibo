package com.ning.arecibo.agent.guice;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.google.inject.Inject;

public class GuicePropsForExplicitMode
{
	private final List<String> explicitConfigFileList;
	private final List<String> explicitHostList;
	private final List<String> explicitPathList;
	private final List<String> explicitTypeList;

	@Inject
	public GuicePropsForExplicitMode(@ExplicitConfigFiles String configFiles,
	                                 @ExplicitHosts String hosts,
	                                 @ExplicitPaths String paths,
	                                 @ExplicitTypes String types)
	{
	    // need to have all three for it to be usable
	    if(configFiles == null || hosts == null || paths == null || types == null) {
	        explicitConfigFileList = null;
	        explicitHostList = null;
	        explicitPathList = null;
	        explicitTypeList = null;
	        return;
	    }
	     
	    StringTokenizer configFilesST = new StringTokenizer(configFiles, ",");
        StringTokenizer hostsST = new StringTokenizer(hosts, ",");
        StringTokenizer pathsST = new StringTokenizer(paths, ",");
        StringTokenizer typesST = new StringTokenizer(types, ",");
        
        // make sure we have at least one of each
        if(hostsST.countTokens() == 0 || pathsST.countTokens() == 0 || typesST.countTokens() == 0) {
	        explicitConfigFileList = null;
	        explicitHostList = null;
	        explicitPathList = null;
	        explicitTypeList = null;
	        return;
	    }
	     
        explicitConfigFileList = new ArrayList<String>();
        while(configFilesST.hasMoreTokens()) {
            explicitConfigFileList.add(configFilesST.nextToken());
        }
        
        explicitHostList = new ArrayList<String>();
        while(hostsST.hasMoreTokens()) {
            explicitHostList.add(hostsST.nextToken());
        }
        
        explicitPathList = new ArrayList<String>();
        while(pathsST.hasMoreTokens()) {
            explicitPathList.add(pathsST.nextToken());
        }
        
        explicitTypeList = new ArrayList<String>();
        while(typesST.hasMoreTokens()) {
            explicitTypeList.add(typesST.nextToken());
        }
	}
	
	public List<String> getExplicitConfigFileList() {
	    return explicitConfigFileList;
	}
	
	public List<String> getExplicitHostList() {
	    return explicitHostList;
	}
	
	public List<String> getExplicitPathList() {
	    return explicitPathList;
	}
	
	public List<String> getExplicitTypeList() {
	    return explicitTypeList;
	}
}
