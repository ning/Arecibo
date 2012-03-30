/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.dashboard.resources;

import com.ning.arecibo.collector.CollectorClient;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.timeline.CategoryAndSampleKinds;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.jersey.metrics.TimedResource;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Path("/rest/1.0")
public class CollectorResource
{
    private static final Logger log = Logger.getLogger(CollectorResource.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CollectorClient client;
    private final GalaxyStatusManager manager;

    @Inject
    public CollectorResource(final CollectorClient client, final GalaxyStatusManager manager)
    {
        this.client = client;
        this.manager = manager;
    }

    @GET
    @Path("/hosts")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public Response getHosts(@QueryParam("callback") @DefaultValue("callback") final String callback)
    {
        try {
            final ImmutableList.Builder<Map<String, String>> builder = new ImmutableList.Builder<Map<String, String>>();
            final Iterable<String> hosts = client.getHosts();

            for (final String hostName : hosts) {
                builder.add(ImmutableMap.<String, String>of(
                        "hostName", hostName,
                        "globalZone", Strings.nullToEmpty(manager.getGlobalZone(hostName)),
                        "configPath", Strings.nullToEmpty(manager.getConfigPath(hostName)),
                        "configSubPath", Strings.nullToEmpty(manager.getConfigSubPath(hostName)),
                        "coreType", Strings.nullToEmpty(manager.getCoreType(hostName))
                ));
            }

            final JSONPObject object = new JSONPObject(callback, builder.build());
            return Response.ok(object).build();
        }
        catch (RuntimeException e) {
            // Likely UniformInterfaceException from the collector client library
            throw new WebApplicationException(e, buildServiceUnavailableResponse());
        }
    }

    @GET
    @Path("/sample_kinds")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public Response getSampleKinds(@QueryParam("host") final List<String> hostNames,
                                   @QueryParam("callback") @DefaultValue("callback") final String callback)
    {
        try {
            final Iterable<CategoryAndSampleKinds> sampleKinds = client.getSampleKinds(hostNames);
            final JSONPObject object = new JSONPObject(callback, sampleKinds);
            return Response.ok(object).build();
        }
        catch (RuntimeException t) {
            // Likely UniformInterfaceException from the collector client library
            throw new WebApplicationException(t.getCause(), buildServiceUnavailableResponse());
        }
    }

    @GET
    @Path("/{host}")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
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

        try {
            final Iterable<TimelineChunkAndTimes> samplesByHostName = client.getSamplesByHostName(hostName, startTime, endTime);
            return buildJsonpResponse(hostName, startTime, endTime, samplesByHostName, callback);
        }
        catch (Throwable t) {
            // Likely UniformInterfaceException from the collector client library
            throw new WebApplicationException(t.getCause(), buildServiceUnavailableResponse());
        }
    }

    @GET
    @Path("/{host}/{sample_kind}")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
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

        try {
            final Iterable<TimelineChunkAndTimes> samplesByHostNameAndSampleKind = client.getSamplesByHostNameAndSampleKind(hostName, sampleKind, startTime, endTime);
            return buildJsonpResponse(hostName, startTime, endTime, samplesByHostNameAndSampleKind, callback);
        }
        catch (Throwable t) {
            // Likely UniformInterfaceException from the collector client library
            throw new WebApplicationException(t.getCause(), buildServiceUnavailableResponse());
        }
    }

    private Response buildJsonpResponse(final String hostName, final DateTime startTime, final DateTime endTime, final Iterable<TimelineChunkAndTimes> samples, final String callback)
    {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(out);
            generator.writeStartObject();

            generator.writeFieldName("hostName");
            generator.writeString(hostName);

            generator.writeFieldName("samples");
            generator.writeStartObject();

            final Map<Integer, StringBuilder> samplesBySampleKind = buildSampleLists(samples, startTime, endTime);
            for (final Integer sampleKindId : samplesBySampleKind.keySet()) {
                generator.writeFieldName(sampleKindId.toString());
                generator.writeString(samplesBySampleKind.get(sampleKindId).toString());
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

    private Map<Integer, StringBuilder> buildSampleLists(final Iterable<TimelineChunkAndTimes> samples, final DateTime startTime, final DateTime endTime) throws IOException
    {
        // We merge the list of samples by type to concatenate timelines
        final Map<Integer, StringBuilder> samplesBySampleKind = new HashMap<Integer, StringBuilder>();
        for (final TimelineChunkAndTimes timelineChunkAndTimes : samples) {
            if (samplesBySampleKind.get(timelineChunkAndTimes.getSampleKindId()) == null) {
                samplesBySampleKind.put(timelineChunkAndTimes.getSampleKindId(), new StringBuilder());
            }
            else {
                samplesBySampleKind.get(timelineChunkAndTimes.getSampleKindId()).append(",");
            }
            samplesBySampleKind.get(timelineChunkAndTimes.getSampleKindId()).append(timelineChunkAndTimes.getSamplesAsCSV(startTime, endTime));
        }
        return samplesBySampleKind;
    }

    private Response buildServiceUnavailableResponse()
    {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
}
