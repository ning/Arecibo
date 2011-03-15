package com.ning.arecibo.agent.config;

import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import com.ning.arecibo.util.jmx.MonitoringType;

public abstract class Config
{
    public final static String EVENT_TYPE = "eventType";
    public final static String EVENT_ATTRIBUTE_TYPE = "eventAttributeType";
    public final static String EVENT_ATTRIBUTE_TYPE_VIRTUAL = "eventAttributeTypeVirtual";
    public final static String MONITORING_TYPES = "monitoringTypes";
    public final static String POLLING_INTERVAL_SECS = "pollingIntervalSecs";

	protected final String host;
	protected final String eventType;
    protected final String eventAttributeType;
    protected final List<MonitoringType> monitoringTypes;
    protected final String monitoringTypesString;
	protected final int pollingIntervalSeconds;

	// galaxy config path components
	protected final String deployedEnv;
	protected final String deployedVersion;
	protected final String deployedType;
	protected final String deployedConfigSubPath;

    public Config(String eventType, String eventAttributeType,List<MonitoringType> monitoringTypes, int pollingIntervalSeconds,
                  String host, String fullConfigPath, String deployedType) throws ConfigException
	{

        if(eventType == null || eventAttributeType == null)
            throw new ConfigException("eventType and eventAttributeType cannot be null");

		this.host = host;
		this.eventType = eventType;
		this.eventAttributeType = eventAttributeType;
		this.deployedType = deployedType;
		
		this.pollingIntervalSeconds = pollingIntervalSeconds;

        if(fullConfigPath == null)
            fullConfigPath = "";

		StringTokenizer st = new StringTokenizer(fullConfigPath, "/");
		if (st.hasMoreTokens()) {
			this.deployedEnv = st.nextToken();
		}
		else {
			this.deployedEnv = null;
		}

		if (st.hasMoreTokens()) {
			this.deployedVersion = st.nextToken();
		}
		else {
			this.deployedVersion = null;
		}

		// skip over the abbreviated core type in the config path
		if (st.hasMoreTokens())
			st.nextToken();

		// grab the path, but not all of the parts; take just the first one so we can group on that.
		// QA adds some additional parts to make the paths unique. We'll use the first one only.
		String configSubPath = null;
		while(st.hasMoreTokens()) {
		    if(configSubPath == null) {
		        configSubPath = st.nextToken();
		    }
		    else {
		        configSubPath += "_" + st.nextToken();
		    }
		}
		
		if(configSubPath == null)
			configSubPath = "unspecified";

		this.deployedConfigSubPath = configSubPath;

        // default monitoring type to by VALUE
        if(monitoringTypes == null) {
            monitoringTypes = new ArrayList<MonitoringType>();
            monitoringTypes.add(MonitoringType.VALUE);
        }

        this.monitoringTypes = monitoringTypes;
        this.monitoringTypesString = this.getMonitoringTypesString();
	}

	public Config(Config orig, String newEventType, String eventAttributeType)  throws ConfigException
	{
		this.eventType = newEventType;
		this.eventAttributeType = eventAttributeType;

        this.host = orig.host;
		this.pollingIntervalSeconds = orig.pollingIntervalSeconds;

		this.deployedEnv = orig.deployedEnv;
		this.deployedVersion = orig.deployedVersion;
		this.deployedType = orig.deployedType;
		this.deployedConfigSubPath = orig.deployedConfigSubPath;

        this.monitoringTypes = orig.copyMonitoringTypes();
        this.monitoringTypesString = this.getMonitoringTypesString();
	}

    public Config(Config orig, String newEventType)  throws ConfigException
    {
        this(orig,newEventType,orig.getEventAttributeType());
    }

    public abstract ConfigType getConfigType();

    public String getHost()
	{
		return host;
	}

	public String getEventType()
	{
		return eventType;
	}

	public String getEventAttributeType()
	{
		return eventAttributeType;
	}

	public String getDeployedEnv()
	{
		return deployedEnv;
	}

	public String getDeployedVersion()
	{
		return deployedVersion;
	}

	public String getDeployedType()
	{
		return deployedType;
	}

	public String getDeployedConfigSubPath()
	{
		return deployedConfigSubPath;
	}

	public int getPollingIntervalSeconds()
	{
		return pollingIntervalSeconds;
	}

	public String getConfigDescriptor()
	{
		// this is used primarily for informational logging
		return this.getHost();
	}

	public String getCollectorHashKey()
	{
		// this is used to determine the unique collector this config will get assigned to.
		return this.getHost() + ":" + 
				this.getEventType() + ":" +
				this.getDeployedType() + ":" +
				this.getDeployedConfigSubPath() + ":" +
				this.getDeployedEnv() + ":" +
				this.getDeployedVersion() + ":" +
				this.getPollingIntervalSeconds();
	}

	public String getConfigHashKey()
	{
		// this is used to determine the unique configs, per collector
		return  this.getEventAttributeType() + ":" +
				this.monitoringTypesString + ":" +
				// add in all the collectorHashKey components
				getCollectorHashKey();
	}

	public boolean isRate()
	{
        return this.monitoringTypes.contains(MonitoringType.RATE);
	}

	public boolean isValue()
	{
		return this.monitoringTypes.contains(MonitoringType.VALUE);
	}

	public boolean isBothValueAndRate()
	{
        return this.monitoringTypes.contains(MonitoringType.VALUE) && this.monitoringTypes.contains(MonitoringType.RATE);
	}
	
	public boolean isCounter() {
        return this.monitoringTypes.contains(MonitoringType.COUNTER);
	}

    private String getMonitoringTypesString() {

        if(this.monitoringTypes == null)
            return "";

        Collections.sort(this.monitoringTypes);

        StringBuilder sb = new StringBuilder();
        for(MonitoringType mType:this.monitoringTypes) {
            sb.append(mType);
        }

        return sb.toString();
    }

    public List<MonitoringType> copyMonitoringTypes() {

        if(this.monitoringTypes == null)
            return null;

        ArrayList<MonitoringType> retList = new ArrayList<MonitoringType>();
        for(MonitoringType mType:this.monitoringTypes) {
            retList.add(mType);
        }

        return retList;
    }

    public static List<MonitoringType> getMonitoringTypesFromObjectList(List objList) {

        ArrayList<MonitoringType> monitoringTypes = new ArrayList<MonitoringType>();

        if(objList == null) {
            monitoringTypes.add(MonitoringType.VALUE);
            return monitoringTypes;
        }

        for(Object obj:objList) {
            if(obj instanceof MonitoringType) {
                monitoringTypes.add((MonitoringType)obj);
            }
            else {
                MonitoringType mType = MonitoringType.valueOf((String)obj);
                monitoringTypes.add(mType);
            }
        }

        return monitoringTypes;
    }

    public static List<MonitoringType> getMonitoringTypesFromControlString(String control) {
        // convert old-style config control flags to monitoring types
        ArrayList<MonitoringType> monitoringTypes = new ArrayList<MonitoringType>();

        if(control.isEmpty()) {
            monitoringTypes.add(MonitoringType.VALUE);
            return monitoringTypes;
        }

        StringTokenizer st = new StringTokenizer(control, ",");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if(token.contains("v") || token.contains("V"))
                monitoringTypes.add(MonitoringType.VALUE);
            else if(token.contains("r") || token.contains("R"))
                monitoringTypes.add(MonitoringType.RATE);
            else if(token.contains("c") || token.contains("C"))
                monitoringTypes.add(MonitoringType.COUNTER);
            else if(token.contains("b") || token.contains("B")) {
                monitoringTypes.add(MonitoringType.VALUE);
                monitoringTypes.add(MonitoringType.RATE);
            }
        }

        return monitoringTypes;
    }

	public boolean equalsConfig(Config cmpConfig) {
		if(!StringUtils.equals(this.host,cmpConfig.host))
			return false;
		if(!StringUtils.equals(this.eventType,cmpConfig.eventType))
			return false;
		if(!StringUtils.equals(this.eventAttributeType,cmpConfig.eventAttributeType))
			return false;
		if(!StringUtils.equals(this.monitoringTypesString,cmpConfig.monitoringTypesString))
			return false;
		if(!StringUtils.equals(this.deployedEnv,cmpConfig.deployedEnv))
			return false;
		if(!StringUtils.equals(this.deployedVersion,cmpConfig.deployedVersion))
			return false;
		if(!StringUtils.equals(this.deployedType,cmpConfig.deployedType))
			return false;
		if(!StringUtils.equals(this.deployedConfigSubPath,cmpConfig.deployedConfigSubPath))
			return false;
			
		if(this.pollingIntervalSeconds != cmpConfig.pollingIntervalSeconds)
			return false;
		
		return true;
	}

}
