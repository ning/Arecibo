package com.ning.arecibo.aggregator.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.google.inject.Inject;
import com.ning.arecibo.event.publisher.AreciboEventServiceChooser;
import com.ning.arecibo.event.publisher.EventServiceChooser;

@Path("/xn/rest/1.0/event/hash/{name}")
public class HashEndPoint
{
    private final AreciboEventServiceChooser chooser;

    @Inject
    public HashEndPoint(EventServiceChooser chooser)
    {
        this.chooser = (AreciboEventServiceChooser)chooser;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@PathParam("name") String name)
    {
        return chooser.getHost(name);
    }
}
