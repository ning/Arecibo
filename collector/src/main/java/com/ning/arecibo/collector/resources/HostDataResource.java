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

package com.ning.arecibo.collector.resources;

import com.google.common.collect.BiMap;
import com.google.inject.Singleton;
import com.ning.arecibo.collector.process.CollectorEventProcessor;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.jersey.metrics.TimedResource;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
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
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Singleton
@Path("/rest/1.0")
public class HostDataResource
{
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
    @TimedResource
    public StreamingOutput getHosts(@QueryParam("pretty") @DefaultValue("false") final boolean pretty)
    {
        try {
            final BiMap<Integer, String> hosts = dao.getHosts();
            return streamResponse(hosts.values(), pretty);
        }
        catch (Throwable e) {
            // JDBI exception
            throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
        }
    }

    @GET
    @Path("/sample_kinds")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public StreamingOutput getSampleKinds(@QueryParam("pretty") @DefaultValue("false") final boolean pretty)
    {
        try {
            final BiMap<Integer, String> sampleKinds = dao.getSampleKinds();
            return streamResponse(sampleKinds.values(), pretty);
        }
        catch (Throwable e) {
            // JDBI exception
            throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
        }
    }

    @GET
    @Path("/{host}")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public StreamingOutput getSamplesByHostName(
        @QueryParam("pretty") @DefaultValue("false") final boolean pretty,
        @PathParam("host") final String hostName,
        @QueryParam("from") @DefaultValue("0") final String from,
        @QueryParam("to") @DefaultValue("") final String to) throws IOException
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
            // Merge in-memory and persisted samples
            final List<TimelineChunkAndTimes> samplesByHostName = dao.getSamplesByHostName(hostName, startTime, endTime);
            samplesByHostName.addAll(processor.getInMemoryTimelineChunkAndTimes(hostName, startTime, endTime));

            // Stream the response out - the caller still needs to filter the samples by time range
            return streamResponse(samplesByHostName, pretty);
        }
        catch (Throwable e) {
            // JDBI exception
            throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
        }
    }

    @GET
    @Path("/{host}/{sample_kind}")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public StreamingOutput getSamplesByHostNameAndSampleKind(
        @QueryParam("pretty") @DefaultValue("false") final boolean pretty,
        @PathParam("host") final String hostName,
        @PathParam("sample_kind") final String sampleKind,
        @QueryParam("from") @DefaultValue("0") final String from,
        @QueryParam("to") @DefaultValue("") final String to) throws IOException
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
            // Merge in-memory and persisted samples
            final List<TimelineChunkAndTimes> samplesByHostNameAndSampleKind = dao.getSamplesByHostNameAndSampleKind(hostName, sampleKind, startTime, endTime);
            samplesByHostNameAndSampleKind.addAll(processor.getInMemoryTimelineChunkAndTimes(hostName, sampleKind, startTime, endTime));

            // Stream the response out - the caller still needs to filter the samples by time range
            return streamResponse(samplesByHostNameAndSampleKind, pretty);
        }
        catch (Throwable e) {
            // JDBI exception
            throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
        }
    }

    private StreamingOutput streamResponse(final Iterable iterable, final boolean pretty)
    {
        return new StreamingOutput()
        {
            @Override
            public void write(final OutputStream output) throws IOException, WebApplicationException
            {
                final JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(output);
                generator.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
                if (pretty) {
                    generator.setPrettyPrinter(new DefaultPrettyPrinter());
                }

                generator.writeStartArray();
                for (final Object pojo : iterable) {
                    generator.writeObject(pojo);
                }
                generator.writeEndArray();

                generator.close();
            }
        };
    }

    private Response buildServiceUnavailableResponse()
    {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
}
