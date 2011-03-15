package com.ning.arecibo.aggregator.rest;

import java.rmi.RemoteException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import com.google.inject.Inject;
import com.ning.arecibo.aggregator.impl.AggregatorImpl;
import com.ning.arecibo.aggregator.impl.AggregatorRegistry;
import com.ning.arecibo.aggregator.impl.AggregatorServiceImpl;
import com.ning.arecibo.util.Logger;

@Path("/xn/rest/1.0/event/aggregator/{name}")
public class EventAggregatorEndPoint
{
	private static final Logger log = Logger.getLoggerViaExpensiveMagic();

	private final AggregatorRegistry registry;
	private final AggregatorServiceImpl service;

	@Inject
	public EventAggregatorEndPoint(AggregatorRegistry registry, AggregatorServiceImpl service)
	{
		this.registry = registry;
		this.service = service;
	}

	@GET
	@Produces({MediaType.TEXT_HTML, "text/html+agg"})
	public Response get(@PathParam("name") String name)
	{
		AggregatorImpl def = registry.getAggregatorImpl(name);

		if (def != null) {
		    return Response.ok(def).type("text/html+agg").build();
		}
		else {
			// render table for all streams
		    return Response.ok(registry).type(MediaType.TEXT_HTML).build();
		}
	}

	@DELETE
	@Produces(MediaType.TEXT_HTML)
	public Response delete(@PathParam("name") String name)
	{
		log.info("Got a delete request for %s", UriBuilder.fromResource(EventAggregatorEndPoint.class).build());

		AggregatorImpl def = registry.getAggregatorImpl(name);
		if ( def != null ) {
			try {
				log.info("deleting aggregator %s", name);
				service.unregister(def.getAggregator().getFullName());
			}
			catch (RemoteException e) {
			    log.warn(e);
			}
			return Response.ok().build();
		}
		else {
			// render table for all streams
            return Response.ok(registry).build();
		}
	}
}
