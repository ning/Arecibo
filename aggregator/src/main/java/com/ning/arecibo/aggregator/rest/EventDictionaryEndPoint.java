package com.ning.arecibo.aggregator.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.google.inject.Inject;
import com.ning.arecibo.aggregator.dictionary.EventDefinition;
import com.ning.arecibo.aggregator.dictionary.EventDictionary;

@Path("/xn/rest/1.0/event/dictionary/{name}")
public class EventDictionaryEndPoint
{
	private final EventDictionary dictionary;

	@Inject
	public EventDictionaryEndPoint(EventDictionary dictionary)
	{
		this.dictionary = dictionary;
	}

	@GET
	@Produces({ MediaType.TEXT_HTML, "text/html+evtdef" })
	public Response get(@PathParam("name") String name)
	{
		EventDefinition def = dictionary.getEventDefintion(name);
		if (def != null) {
		    return Response.ok(def).type("text/html+evtdef").build();
		}
		else {
			// render table for all streams
            return Response.ok(dictionary).type(MediaType.TEXT_HTML).build();
		}
	}
}
