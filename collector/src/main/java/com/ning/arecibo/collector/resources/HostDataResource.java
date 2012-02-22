package com.ning.arecibo.collector.resources;

import com.google.common.collect.BiMap;
import com.google.inject.Singleton;
import com.ning.arecibo.collector.process.CollectorEventProcessor;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineDAO;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.util.JSONPObject;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Path("/rest/1.0")
public class HostDataResource
{
    private static final Logger log = Logger.getLogger(HostDataResource.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final TimelineDAO dao;
    private final CollectorEventProcessor processor;

    @Inject
    public HostDataResource(final TimelineDAO dao, final CollectorEventProcessor processor)
    {
        this.dao = dao;
        this.processor = processor;
    }

    @GET
    @Path("/hosts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHosts(@QueryParam("callback") @DefaultValue("callback") final String callback)
    {
        final BiMap<Integer, String> hosts = dao.getHosts();
        final JSONPObject object = new JSONPObject(callback, hosts);
        return Response.ok(object).build();
    }

    @GET
    @Path("/sample_kinds")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSampleKinds(@QueryParam("callback") @DefaultValue("callback") final String callback)
    {
        final BiMap<Integer, String> sampleKinds = dao.getSampleKinds();
        final JSONPObject object = new JSONPObject(callback, sampleKinds);
        return Response.ok(object).build();
    }

    @GET
    @Path("/{host}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSamplesByHostName(@QueryParam("callback") @DefaultValue("callback") final String callback,
                                         @PathParam("host") final String hostName,
                                         @QueryParam("from") @DefaultValue("0") final String from,
                                         @QueryParam("to") @DefaultValue("") final String to)
    {
        final DateTime startTime = new DateTime(from, DateTimeZone.UTC);
        final DateTime endTime;
        if (to.isEmpty()) {
            endTime = new DateTime(DateTimeZone.UTC);
        }
        else {
            endTime = new DateTime(to, DateTimeZone.UTC);
        }

        // Merge in-memory and persisted samples
        final List<TimelineChunkAndTimes> samplesByHostName = dao.getSamplesByHostName(hostName, startTime, endTime);
        try {
            samplesByHostName.addAll(processor.getInMemoryTimelineChunkAndTimes(hostName, startTime, endTime));
        }
        catch (IOException e) {
            return Response.serverError().header("Warning", "199 " + e.toString()).build();
        }

        return buildJsonpResponse(hostName, startTime, endTime, samplesByHostName, callback);
    }

    @GET
    @Path("/{host}/{sample_kind}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSamplesByHostNameAndSampleKind(@QueryParam("callback") @DefaultValue("callback") final String callback,
                                                      @PathParam("host") final String hostName,
                                                      @PathParam("sample_kind") final String sampleKind,
                                                      @QueryParam("from") @DefaultValue("0") final String from,
                                                      @QueryParam("to") @DefaultValue("") final String to)
    {
        final DateTime startTime = new DateTime(from, DateTimeZone.UTC);
        final DateTime endTime;
        if (to.isEmpty()) {
            endTime = new DateTime(DateTimeZone.UTC);
        }
        else {
            endTime = new DateTime(to, DateTimeZone.UTC);
        }

        // Merge in-memory and persisted samples
        final List<TimelineChunkAndTimes> samplesByHostNameAndSampleKind = dao.getSamplesByHostNameAndSampleKind(hostName, sampleKind, startTime, endTime);
        try {
            samplesByHostNameAndSampleKind.addAll(processor.getInMemoryTimelineChunkAndTimes(hostName, sampleKind, startTime, endTime));
        }
        catch (IOException e) {
            return Response.serverError().header("Warning", "199 " + e.toString()).build();
        }

        return buildJsonpResponse(hostName, startTime, endTime, samplesByHostNameAndSampleKind, callback);
    }

    private Response buildJsonpResponse(final String hostName, final DateTime startTime, final DateTime endTime, final List<TimelineChunkAndTimes> samples, final String callback)
    {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(out);
            generator.writeStartObject();

            generator.writeFieldName("hostName");
            generator.writeString(hostName);

            generator.writeFieldName("samples");
            generator.writeStartObject();

            final Map<String, StringBuilder> samplesBySampleKind = buildSampleLists(samples, startTime, endTime);
            for (final String sampleKind : samplesBySampleKind.keySet()) {
                generator.writeFieldName(sampleKind);
                generator.writeString(samplesBySampleKind.get(sampleKind).toString());
            }
            generator.writeEndObject();

            generator.writeEndObject();
            generator.close();

            final JSONPObject object = new JSONPObject(callback, out.toString());
            return Response.ok(object).build();
        }
        catch (IOException e) {
            log.error(e);
            return Response.serverError().build();
        }
    }

    private Map<String, StringBuilder> buildSampleLists(final List<TimelineChunkAndTimes> samples, final DateTime startTime, final DateTime endTime) throws IOException
    {
        // We merge the list of samples by type to concatenate timelines
        final Map<String, StringBuilder> samplesBySampleKind = new HashMap<String, StringBuilder>();
        for (final TimelineChunkAndTimes timelineChunkAndTimes : samples) {
            if (samplesBySampleKind.get(timelineChunkAndTimes.getSampleKind()) == null) {
                samplesBySampleKind.put(timelineChunkAndTimes.getSampleKind(), new StringBuilder());
            }
            else {
                samplesBySampleKind.get(timelineChunkAndTimes.getSampleKind()).append(",");
            }
            samplesBySampleKind.get(timelineChunkAndTimes.getSampleKind()).append(timelineChunkAndTimes.getSamplesAsCSV(startTime, endTime));
        }
        return samplesBySampleKind;
    }
}
