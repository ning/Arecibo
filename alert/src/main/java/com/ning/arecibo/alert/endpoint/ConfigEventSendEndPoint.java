package com.ning.arecibo.alert.endpoint;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.google.inject.Inject;
import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.manage.AlertManager;

@Path("/xn/rest/1.0/JSONAlertStatus")
public class ConfigEventSendEndPoint
{
    private final ConfigManager confStatusManager;
    private final AlertManager alertManager;

    @Inject
    public ConfigEventSendEndPoint(ConfigManager confStatusManager, AlertManager alertManager) 
    {
        this.confStatusManager = confStatusManager;
        this.alertManager = alertManager;
    }

    @GET
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String sendEventsToAllConfigs()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append("Sent events to alertConfigs: \n");
    	
    	List<Long> configIdList = confStatusManager.getThresholdConfigIds();
    	for(Long configId:configIdList) {
    		alertManager.handleThresholdEvent(configId,null);
    		sb.append(String.format("\t%d\n",configId));
    	}
    	
    	return sb.toString();
    }
}
