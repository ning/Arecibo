package com.ning.arecibo.alert.endpoint;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.google.inject.Inject;
import com.ning.arecibo.alert.conf.ConfigManager;

@Path("/xn/rest/1.0/ConfigStatus")
public class ConfigStatusEndPoint
{
    private final ConfigManager confStatusManager;

    @Inject
    public ConfigStatusEndPoint(ConfigManager confStatusManager) 
    {
        this.confStatusManager = confStatusManager;
    }

    @GET
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String getConfigStatus()
    {
        return confStatusManager.getConfigStatus();
    }
}
