package com.ning.arecibo.collector.resources;

import com.google.inject.Singleton;
import com.sun.jersey.api.view.Viewable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Singleton
@Path("/")
public class Dashboard
{
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable welcome()
    {
        return new Viewable("dashboard.html");
    }
}
