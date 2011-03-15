package com.ning.arecibo.agent.config.tcp;

import java.util.Map;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.config.ConfigType;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;


public class TCPConnectCheckConfig extends Config {

    public final static String PORT = "port";

    protected final int port;

    public TCPConnectCheckConfig(String host, String fullConfigPath, String deployedType, GuiceDefaultsForDataSources guiceDefaults,Map<String,Object> optionsMap) throws ConfigException {

        super((String) optionsMap.get(Config.EVENT_TYPE),
                Config.EVENT_ATTRIBUTE_TYPE_VIRTUAL,
                null,
                optionsMap.get(Config.POLLING_INTERVAL_SECS) != null?
                        Integer.parseInt(optionsMap.get(Config.POLLING_INTERVAL_SECS).toString()) : guiceDefaults.getDefaultPollingIntervalSeconds(),
                host,
                fullConfigPath,
                deployedType);

        Object portObj = optionsMap.get(PORT);
        if(portObj == null) {
            this.port = 80;
        }
        else {
            this.port = Integer.parseInt(portObj.toString());
        }
    }

    public TCPConnectCheckConfig(TCPConnectCheckConfig orig, String newEventAttributeType) throws ConfigException {
        super(orig,orig.eventType, newEventAttributeType);
        this.port = orig.port;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String getConfigDescriptor() {

        return super.getConfigDescriptor() + ":" + this.port;
    }

    @Override
    public String getConfigHashKey() {

        return  this.port + ":" + super.getConfigHashKey();
    }

    @Override
    public boolean equalsConfig(Config cmpConfig) {

        if (cmpConfig == null || !(cmpConfig instanceof TCPConnectCheckConfig))
            return false;

        if (!super.equalsConfig(cmpConfig))
            return false;

        TCPConnectCheckConfig tcpCmpConfig = (TCPConnectCheckConfig)cmpConfig;
        if(tcpCmpConfig.port != this.port)
            return false;

        return true;
    }

    @Override
    public ConfigType getConfigType() {
        return ConfigType.HTTP;
    }
}
