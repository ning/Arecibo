package com.ning.arecibo.agent.config.jmx;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.config.ConfigType;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;

public final class JMXConfig extends Config
{
    public final static String OBJECT_NAME = "objectName";
    public final static String ATTRIBUTE = "attribute";

    protected final String objectName;
    protected final String attribute;
	protected final boolean dynamic;
    protected final int port;

    public JMXConfig(String host, String fullConfigPath, String deployedType, GuiceDefaultsForDataSources guiceDefaults, Map<String,Object> optionsMap) throws ConfigException
    {
        super((String)optionsMap.get(Config.EVENT_TYPE),
             (String)optionsMap.get(Config.EVENT_ATTRIBUTE_TYPE),
             Config.getMonitoringTypesFromObjectList((List)optionsMap.get(Config.MONITORING_TYPES)),
             optionsMap.get(Config.POLLING_INTERVAL_SECS) != null?
                    Integer.parseInt(optionsMap.get(Config.POLLING_INTERVAL_SECS).toString()) : guiceDefaults.getDefaultPollingIntervalSeconds(),
             host,
             fullConfigPath,
             deployedType);

        this.objectName = (String)optionsMap.get(OBJECT_NAME);
        this.attribute = (String)optionsMap.get(ATTRIBUTE);
        this.port = guiceDefaults.getJmxPort();

        String wildcardChar = JMXDynamicUtils.wildCardDelim;
        this.dynamic = this.objectName.contains(wildcardChar) || this.attribute.contains(wildcardChar) || this.eventType.contains(wildcardChar) || this.eventAttributeType.contains(wildcardChar);

        checkWildcardNaming();
    }

    public JMXConfig(JMXConfig orig, String newEventType, String newEventAttributeType,
                     String newObjectName, String newAttribute)  throws ConfigException
	{
        super(orig,newEventType,newEventAttributeType);
        this.objectName = newObjectName;
        this.attribute = newAttribute;
        this.port = orig.port;

		String wildcardChar = JMXDynamicUtils.wildCardDelim;
		this.dynamic = this.objectName.contains(wildcardChar) || this.attribute.contains(wildcardChar) || this.eventType.contains(wildcardChar) || this.eventAttributeType.contains(wildcardChar);

		checkWildcardNaming();
	}

    public JMXConfig(JMXConfig orig, String newEventType,
                     String newObjectName)  throws ConfigException
    {
        this(orig,newEventType,orig.getEventAttributeType(),newObjectName,orig.attribute);
    }

	private void checkWildcardNaming() throws ConfigException
	{
		String wildcardChar = JMXDynamicUtils.wildCardDelim;
		// New: allow accessor/name mismatch because we want to drop IDs from cache MBeans: XXXX_ID=* => XXXX
		if (!this.objectName.contains(wildcardChar) && this.eventType.contains(wildcardChar)) {
			String msg = "Config wildcard mismatch between accessor and its event name: ";
			msg += String.format("Accessor='%s', Event name='%s'", this.objectName, this.eventType);
			throw new ConfigException(msg);
		}
		if (this.attribute.contains(wildcardChar) != this.eventAttributeType.contains(wildcardChar)) {
			String msg = "Config wildcard mismatch between attribute and its key name: ";
			msg += String.format("Attribute='%s', Key name='%s'", this.attribute, this.eventAttributeType);
			throw new ConfigException(msg);
		}
	}

	public boolean isDynamic() {
		return this.dynamic;
	}

    public String getObjectName() {
        return this.objectName;
    }

    public String getAttribute() {
        return this.attribute;
    }

    public int getPort() {
        return port;
    }
    
    @Override
    public String getConfigDescriptor()  {
        
        return super.getConfigDescriptor() + ":" +
               this.objectName + ":" + this.attribute + ":" + this.port;
    }

    @Override
    public String getConfigHashKey() {
        
        return this.objectName + ":" + this.attribute + ":" +
               super.getConfigHashKey();
    }

    @Override
	public boolean equalsConfig(Config cmpConfig) {

        if(cmpConfig == null || !(cmpConfig instanceof JMXConfig))
            return false;

        if(!super.equalsConfig(cmpConfig))
            return false;

        if(!StringUtils.equals(this.objectName, ((JMXConfig)cmpConfig).objectName))
            return false;
        if(!StringUtils.equals(this.attribute, ((JMXConfig)cmpConfig).attribute))
            return false;
        if (this.port != ((JMXConfig)cmpConfig).port)
            return false; 
		if(this.dynamic != ((JMXConfig)cmpConfig).dynamic)
			return false;

		return true;
	}

    @Override
    public ConfigType getConfigType() {
        return ConfigType.JMX;
    }

}
