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
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimesConsumer;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimesDecoded;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.jersey.metrics.TimedResource;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
@Path("/rest/1.0")
public class HostDataResource
{
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final TimelineDAO dao;
    private final TimelineEventHandler processor;

    @Inject
    public HostDataResource(final TimelineDAO dao, final TimelineEventHandler processor)
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
        @QueryParam("decode_samples") @DefaultValue("false") final boolean decodeSamples,
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
            return streamResponse(samplesByHostName, pretty, decodeSamples);
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
        @QueryParam("decode_samples") @DefaultValue("false") final boolean decodeSamples,
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
            return streamResponse(samplesByHostNameAndSampleKind, pretty, decodeSamples);
        }
        catch (Throwable e) {
            // JDBI exception
            throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
        }
    }

    /**
     * This entrypoint support multiple "host" parameters and multiple "sample_kind" query parameters, in
     * addition to the DateTime query parameters "to" and "from", and the boolean parameters "pretty" and
     * "decodeSamples".  If "pretty" is true, the json is pretty-printed; if "decodeSamples" is
     * true, each timeline sample string is presented in human-readable form, not hex.
     * <p>
     * A typical url might be "/rest/1.0/host_samples?host=z111111.ningops.com&host=z222222.ningops.com&sample_kind=99thPercentile&sample_kind=50thPercentile&from=2012-03-10 08:30:00&to=2012-03-10 10:30:00"
     * <p><p>
     * If no "sample_kind" query parameters are supplied, all sample kinds for the host will be returned.
     * TODO: Does this actually make sense, or should no sample kinds be an error?
     * <p>
     * If "from" is not supplied, the chunks returned start with the oldest available chunks for the
     * host and sample type.  If the "to" is not supplied, it defaults to the current time.
     * <p><p>
     * This entrypoint does real streaming, so that very large queries will still have a small memory
     * footprints.  It gets this done by returning a StreamingOutput object that does the query inside
     * the DAO's createHandle() method, invokes the TimelineChunkAndTimesConsumer method
     * processTimelineChunkAndTimes() to process each TimelineChunkAndTimes instance.  This
     * approach provides guarantees that the database connection will get closed in the StreamingOutput's
     * try/finally clause.
     * <p><p>
     * This method offers guarantees on the order of the chunks processed:
     * they are ordered first by host name ascending, then by sample kind ascending, then by
     * start_time ascending.  This ordering is preserved both for the chunks that come from the
     * DAO as well as the chunks that come from the sample accumulators.
     * <p>
     * This entrypoint is expected to be the workhorse of the dashboard UI.
     * @param ui the UriInfo for this query, from which the query parameters are extracted.
     * @return a StreamingOutput object whose write() method invokes the database query and
     * processes the chunks retrieved.
     * @throws IOException
     */
    @GET
    @Path("/host_samples")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public StreamingOutput getHostSamples(@Context final UriInfo ui) throws IOException
    {
        final MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        final String fromString = optionalStringParamSingle(queryParams, "from");
        final String toString = optionalStringParamSingle(queryParams, "to");
        final DateTime startTime = new DateTime(fromString == null ? "0" : fromString, DateTimeZone.UTC);
        final DateTime endTime = toString == null ? new DateTime(DateTimeZone.UTC) : new DateTime(toString, DateTimeZone.UTC);
        final boolean pretty = optionalBooleanParamSingle(queryParams, "pretty");
        final boolean decodeSamples = optionalBooleanParamSingle(queryParams, "decode_samples");
        final List<String> hostNames = stringParamMultiple(queryParams, "host");
        final List<String> sampleKinds = stringParamMultiple(queryParams, "sample_kind");
        final AtomicReference<String> lastHostName = new AtomicReference<String>(null);
        final AtomicReference<String> lastSampleKind = new AtomicReference<String>(null);
        final List<TimelineChunkAndTimes> chunksForHostAndSampleKind = new ArrayList<TimelineChunkAndTimes>();
        return new StreamingOutput() {

            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                final JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(output);
                generator.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
                if (pretty) {
                    generator.setPrettyPrinter(new DefaultPrettyPrinter());
                }

                generator.writeStartArray();
                try {
                    // Merge in-memory and persisted samples
                    dao.getSamplesByHostNamesAndSampleKinds(hostNames, sampleKinds, startTime, endTime, new TimelineChunkAndTimesConsumer() {

                        @Override
                        public void processTimelineChunkAndTimes(TimelineChunkAndTimes chunkAndTimes) {
                            try {
                                String previousHostName = lastHostName.get();
                                String previousSampleKind = lastSampleKind.get();
                                final String currentHostName = chunkAndTimes.getHostName();
                                final String currentSampleKind = chunkAndTimes.getSampleKind();
                                if (previousHostName != null && (!previousHostName.equals(currentHostName) || !previousSampleKind.equals(currentSampleKind))) {
                                    lastHostName.set(currentHostName);
                                    lastSampleKind.set(currentSampleKind);
                                    writeJsonForChunks(generator, chunksForHostAndSampleKind, previousHostName, previousSampleKind, startTime, endTime, decodeSamples);
                                }
                            }
                            catch (Throwable e) {
                                // JDBI exception
                                throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
                            }
                        }
                    });
                    if (chunksForHostAndSampleKind.size() > 0) {
                        writeJsonForChunks(generator, chunksForHostAndSampleKind, lastHostName.get(), lastSampleKind.get(), startTime, endTime, decodeSamples);
                    }
                }
                catch (Throwable e) {
                    // JDBI exception
                    throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
                }
                generator.writeEndArray();
                generator.close();
            }
        };
    }

    private void writeJsonForChunks(final JsonGenerator generator, final List<TimelineChunkAndTimes> chunksForHostAndSampleKind, final String hostName,
                                final String sampleKind, final DateTime startTime, final DateTime endTime, final boolean decodeSamples)
            throws IOException, ExecutionException
    {
        final List<TimelineChunkAndTimes> chunksToConsume = new ArrayList<TimelineChunkAndTimes>();
        Collections.copy(chunksToConsume, chunksForHostAndSampleKind);
        chunksToConsume.addAll(processor.getInMemoryTimelineChunkAndTimes(hostName, sampleKind, startTime, endTime));
        chunksForHostAndSampleKind.clear();
        for (TimelineChunkAndTimes chunk : chunksForHostAndSampleKind) {
            writeJsonForChunk(generator, chunk, decodeSamples);
        }
    }

    private void writeJsonForChunk(final JsonGenerator generator, final TimelineChunkAndTimes chunk, final boolean decodeSamples) throws IOException
    {
        if (decodeSamples) {
            generator.writeObject(new TimelineChunkAndTimesDecoded(chunk));
        }
        else {
            generator.writeObject(chunk);
        }
    }

    public List<String> stringParamMultiple(final MultivaluedMap<String, String> queryParams, final String key)
    {
        final List<String> results = new ArrayList<String>();
        final List<String> valueStrings = queryParams.get(key);
        for (String valueString : valueStrings) {
            results.add(valueString);
        }
        return results;
    }

    public String optionalStringParamSingle(final MultivaluedMap<String, String> queryParams, final String key)
    {
        final List<String> values = queryParams.get(key);
        if (values == null) {
            return null;
        }
        else if (values.size() == 1) {
            return values.get(0);
        }
        else {
            throw createBadRequest(String.format("Must have at most one %s param", key));
        }
    }

    public boolean optionalBooleanParamSingle(final MultivaluedMap<String, String> queryParams, final String key)
    {
        final List<String> values = queryParams.get(key);
        if (values == null) {
            return false;
        }
        else if (values.size() == 1) {
            return Boolean.parseBoolean(values.get(0));
        }
        else {
            throw createBadRequest(String.format("Must have at most one %s param", key));
        }
    }

    private WebApplicationException createBadRequest(final String message)
    {
        return new WebApplicationException(new ResponseBuilderImpl().status(Response.Status.BAD_REQUEST).entity(message).build());
    }

    private StreamingOutput streamResponse(final Iterable iterable, final boolean pretty)
    {
        return streamResponse(iterable, pretty, false);
    }

    private StreamingOutput streamResponse(final Iterable iterable, final boolean pretty, final boolean decodeSamples)
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
                    if (decodeSamples && pojo instanceof TimelineChunkAndTimes) {
                        generator.writeObject(new TimelineChunkAndTimesDecoded((TimelineChunkAndTimes)pojo));
                    }
                    else {
                        generator.writeObject(pojo);
                    }
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
