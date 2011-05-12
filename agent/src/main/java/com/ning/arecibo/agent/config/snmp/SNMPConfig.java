package com.ning.arecibo.agent.config.snmp;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.skife.config.TimeSpan;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.config.ConfigType;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;

public final class SNMPConfig extends Config
{
    public final static String MIB = "mib";
    public final static String OID_NAME = "oidName";
    public final static String OID_ROW = "oidRow";

    protected final int port;
    protected final String communityString;
    protected final String mib;
    protected final String oidName;
    protected final String oidRow;

    // can't make this final, since we need to dynamically discover in case of snmp beans, at least for now
    protected volatile boolean isCounterOverride = false;

    public SNMPConfig(String host,
                      String fullConfigPath,
                      String deployedType,
                      GuiceDefaultsForDataSources guiceDefaults,
                      Map<String,Object> optionsMap) throws ConfigException
    {
        super((String) optionsMap.get(Config.EVENT_TYPE),
              (String) optionsMap.get(Config.EVENT_ATTRIBUTE_TYPE),
              Config.getMonitoringTypesFromObjectList((List) optionsMap.get(Config.MONITORING_TYPES)),
              optionsMap.get(Config.POLLING_INTERVAL_SECS) != null ?
                    new TimeSpan(optionsMap.get(Config.POLLING_INTERVAL_SECS).toString()) :
                    guiceDefaults.getDefaultPollingInterval(),
              host,
              fullConfigPath,
              deployedType);

        this.port = guiceDefaults.getSnmpPort();
        this.communityString = guiceDefaults.getSnmpCommunity();

        this.mib = (String) optionsMap.get(SNMPConfig.MIB);
        this.oidName = (String) optionsMap.get(SNMPConfig.OID_NAME);

        Object oidRowObj =  optionsMap.get(SNMPConfig.OID_ROW);
        if(oidRowObj != null) {
            String oidRow = oidRowObj.toString();
            if(oidRow == null)
                oidRow = "0";
            this.oidRow = oidRow;
        }
        else
            this.oidRow = "0";
    }

	public SNMPConfig(SNMPConfig orig, String newEventType, String eventAttributeType,
                      String newMib, String newOidName, String newOidRow)  throws ConfigException
	{
        super(orig,newEventType,eventAttributeType);
        this.port = orig.port;
        this.communityString = orig.communityString;
        this.mib = newMib;
        this.oidName = newOidName;

        if(newOidRow == null)
            newOidRow = "0";

        this.oidRow = newOidRow;
	}

    public int getPort() {
        return port;
    }

    public String getCommunityString() {
        return communityString;
    }

    public String getMib() {
        return mib;
    }

    public String getOidName() {
        return oidName;
    }

    public String getOidRow() {
        return oidRow;
    }
    
    public void setCounterOverride(boolean isCounter) {
        this.isCounterOverride = isCounter;
    }

    @Override
    public boolean isCounter() {
        if(this.isCounterOverride)
            return true;
        else
            return super.isCounter();
    }

    @Override
    public String getConfigDescriptor() {

        return super.getConfigDescriptor() + ":" +
                this.port + ":" + this.mib + ":" + this.oidName + ":" + this.oidRow;
    }

    @Override
    public String getConfigHashKey() {

        return this.port + ":" + this.mib + ":" + this.oidName + ":" + this.oidRow + ":" +
                super.getConfigHashKey();
    }

    @Override
    public boolean equalsConfig(Config cmpConfig) {

        if (cmpConfig == null || !(cmpConfig instanceof SNMPConfig))
            return false;

        if (!super.equalsConfig(cmpConfig))
            return false;

        if (this.port != ((SNMPConfig) cmpConfig).port)
            return false;
        if (!StringUtils.equals(this.mib, ((SNMPConfig) cmpConfig).mib))
            return false;
        if (!StringUtils.equals(this.oidName, ((SNMPConfig) cmpConfig).oidName))
            return false;
        if (!StringUtils.equals(this.oidRow, ((SNMPConfig) cmpConfig).oidRow))
            return false;

        return true;
    }

    @Override
    public ConfigType getConfigType() {
        return ConfigType.SNMP;
    }

}
