package com.ning.arecibo.alert.endpoint;

import static com.ning.arecibo.alert.client.AlertActivationStatus.ERROR;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.google.inject.Inject;
import com.ning.arecibo.alert.client.AlertStatus;
import com.ning.arecibo.alert.client.AlertStatusJSONConverter;
import com.ning.arecibo.alert.conf.ConfigManager;

@Path("/xn/rest/1.0/JSONAlertStatus")
public class JSONAlertStatusEndPoint
{
    private final ConfigManager confStatusManager;

    @Inject
    public JSONAlertStatusEndPoint(ConfigManager confStatusManager) 
    {
        this.confStatusManager = confStatusManager;
    }

    @GET
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String getAlertStatusJSON() throws IOException
    {
        List<AlertStatus> alertStatii = confStatusManager.getAlertStatus(ERROR);
        	
        return AlertStatusJSONConverter.serializeStatusListToJSON(alertStatii);
    }
}
