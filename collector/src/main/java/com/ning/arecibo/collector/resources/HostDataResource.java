package com.ning.arecibo.collector.resources;

import com.google.inject.Singleton;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineDAO;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Singleton
@Path("/rest/1.0")
public class HostDataResource
{
    private static final Logger log = Logger.getLogger(HostDataResource.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final TimelineDAO dao;

    @Inject
    public HostDataResource(final TimelineDAO dao)
    {
        this.dao = dao;
    }

    @GET
    @Path("/hosts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHosts()
    {
        return Response.ok(dao.getHosts()).build();
    }

    @GET
    @Path("/sample_kinds")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSampleKinds()
    {
        return Response.ok(dao.getSampleKinds()).build();
    }

    @GET
    @Path("/{host}/samples")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSamplesByHostName(@PathParam("host") final String hostName,
                                         @QueryParam("from") @DefaultValue("0") final String from,
                                         @QueryParam("to") @DefaultValue("") final String to)
    {
        final List<TimelineChunkAndTimes> samplesByHostName;

        final DateTime startTime = new DateTime(from, DateTimeZone.UTC);
        if (to.isEmpty()) {
            samplesByHostName = dao.getSamplesByHostName(hostName, startTime, new DateTime(DateTimeZone.UTC));
        }
        else {
            samplesByHostName = dao.getSamplesByHostName(hostName, startTime, new DateTime(to, DateTimeZone.UTC));
        }

        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(out);
            generator.writeStartObject();

            generator.writeFieldName("hostName");
            generator.writeString(hostName);

            generator.writeFieldName("samples");
            generator.writeStartArray();
            for (final TimelineChunkAndTimes timelineChunkAndTimes : samplesByHostName) {
                generator.writeObject(timelineChunkAndTimes);
            }
            generator.writeEndArray();

            generator.writeEndObject();
            generator.close();

            return Response.ok(out.toString()).build();
        }
        catch (IOException e) {
            log.error(e);
            return Response.serverError().build();
        }
    }
}
