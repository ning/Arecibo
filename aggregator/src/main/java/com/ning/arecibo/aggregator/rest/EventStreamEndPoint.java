package com.ning.arecibo.aggregator.rest;

import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import com.espertech.esper.client.EPStatement;
import com.google.inject.Inject;
import com.ning.arecibo.aggregator.dictionary.EventDictionary;
import com.ning.arecibo.aggregator.impl.AggregatorRegistry;

@Path("/event/stream/{stream}")
public class EventStreamEndPoint
{
	private final AggregatorRegistry registry;
    private final EventDictionary dict;

    @Inject
	public EventStreamEndPoint(AggregatorRegistry registry, EventDictionary dict)
	{
		this.registry = registry;
        this.dict = dict;
    }

    @Path("/event/stream/{stream}")
    @GET
    @Produces({ MediaType.TEXT_PLAIN, "text/plain+epstmts" })
	public Response getStream(String stream, @Context UriInfo uriInfo)
	{
        EPStatement ep = registry.getStatement(stream);
        if (ep != null) {
            return Response.ok(new EPStatementQuery(ep, uriInfo.getQueryParameters())).type(MediaType.TEXT_PLAIN).build();
        }
        else {
            // render table for all streams
            return Response.ok(registry.getStatementNames()).type("text/plain+epstmts").build();
        }
	}

    @Path("/event/instream/{stream}")
    @GET
    @Produces("text/plain+rawEvt")
    public Response getInStream(String stream, @Context UriInfo uriInfo)
    {
        if (dict.getEventDefintion(stream) != null) {
            return Response.ok(new StreamQuery(stream, uriInfo.getQueryParameters())).build();
        }
        else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    public static class EPStatementQuery
    {
        Map<String, List<String>> where ;
        EPStatement stmt ;

        public EPStatementQuery(EPStatement stmt, Map<String, List<String>> where)
        {
            this.where = where;
            this.stmt = stmt;
        }

        public Map<String, List<String>> getWhere()
        {
            return where;
        }

        public EPStatement getStmt()
        {
            return stmt;
        }
    }

    public static class StreamQuery
    {
        Map<String, List<String>> where ;
        String eventName ;

        public StreamQuery(String eventName, Map<String, List<String>> where)
        {
            this.where = where;
            this.eventName = eventName;
        }

        public Map<String, List<String>> getWhere()
        {
            return where;
        }

        public String getEventName()
        {
            return eventName;
        }
    }
}
