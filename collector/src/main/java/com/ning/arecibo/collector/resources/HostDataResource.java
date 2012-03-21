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
import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimesConsumer;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimesDecoded;
import com.ning.arecibo.util.timeline.TimelineChunksAndTimesViews;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.jaxrs.DateTimeParameter;
import com.ning.jersey.metrics.TimedResource;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.joda.time.DateTime;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
@Path("/rest/1.0")
public class HostDataResource
{
    private static final ObjectMapper objectMapper = new ObjectMapper().configure(SerializationConfig.Feature.DEFAULT_VIEW_INCLUSION, false);

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
    public StreamingOutput getSamplesByHostName(@QueryParam("from") @DefaultValue("") final DateTimeParameter startTimeParameter,
                                                @QueryParam("to") @DefaultValue("") final DateTimeParameter endTimeParameter,
                                                @QueryParam("pretty") @DefaultValue("false") final boolean pretty,
                                                @QueryParam("decode_samples") @DefaultValue("false") final boolean decodeSamples,
                                                @QueryParam("compact") @DefaultValue("false") final boolean compact,
                                                @PathParam("host") final String hostName) throws IOException
    {
        final Iterable<String> sampleKinds = dao.getSampleKindsByHostName(hostName);
        return getHostSamples(startTimeParameter, endTimeParameter, pretty, decodeSamples, compact, ImmutableList.<String>of(hostName), ImmutableList.<String>copyOf(sampleKinds));
    }

    @GET
    @Path("/{host}/{sample_kind}")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public StreamingOutput getSamplesByHostNameAndSampleKind(@QueryParam("from") @DefaultValue("") final DateTimeParameter startTimeParameter,
                                                             @QueryParam("to") @DefaultValue("") final DateTimeParameter endTimeParameter,
                                                             @QueryParam("pretty") @DefaultValue("false") final boolean pretty,
                                                             @QueryParam("decode_samples") @DefaultValue("false") final boolean decodeSamples,
                                                             @QueryParam("compact") @DefaultValue("false") final boolean compact,
                                                             @PathParam("host") final String hostName,
                                                             @PathParam("sample_kind") final String sampleKind) throws IOException
    {
        return getHostSamples(startTimeParameter, endTimeParameter, pretty, decodeSamples, compact, ImmutableList.<String>of(hostName), ImmutableList.<String>of(sampleKind));
    }

    /**
     * This entrypoint support multiple "host" parameters and multiple "sample_kind" query parameters, in
     * addition to the DateTime query parameters "to" and "from", and the boolean parameters "pretty" and
     * "decodeSamples".  If "pretty" is true, the json is pretty-printed; if "decodeSamples" is
     * true, each timeline sample string is presented in human-readable form, not hex.
     * <p/>
     * A typical url might be "/rest/1.0/host_samples?host=z111111.ningops.com&host=z222222.ningops.com&sample_kind=99thPercentile&sample_kind=50thPercentile&from=2012-03-10 08:30:00&to=2012-03-10 10:30:00"
     * <p><p>
     * If no "sample_kind" query parameters are supplied, all sample kinds for the host will be returned.
     * TODO: Does this actually make sense, or should no sample kinds be an error?
     * <p/>
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
     * <p/>
     * This entrypoint is expected to be the workhorse of the dashboard UI.
     *
     * @param startTimeParameter start time for the samples
     * @param endTimeParameter   end time for the samples
     * @param pretty             whether pretty printing is enabled
     * @param decodeSamples      whether samples should be decoded in human-readable form
     * @param compact            whether compact representation should be used (csv default) - TODO
     * @param hostNames          list of host names
     * @param sampleKinds        list of sample kinds
     * @return a StreamingOutput object whose write() method invokes the database query and
     *         processes the chunks retrieved.
     */
    @GET
    @Path("/host_samples")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public StreamingOutput getHostSamples(@QueryParam("from") @DefaultValue("") final DateTimeParameter startTimeParameter,
                                          @QueryParam("to") @DefaultValue("") final DateTimeParameter endTimeParameter,
                                          @QueryParam("pretty") @DefaultValue("false") final boolean pretty,
                                          @QueryParam("decode_samples") @DefaultValue("false") final boolean decodeSamples,
                                          @QueryParam("compact") @DefaultValue("false") final boolean compact,
                                          @QueryParam("host") final List<String> hostNames,
                                          @QueryParam("sample_kind") final List<String> sampleKinds)
    {
        final DateTime startTime = startTimeParameter.getValue();
        final DateTime endTime = endTimeParameter.getValue();

        final AtomicReference<String> lastHostName = new AtomicReference<String>(null);
        final AtomicReference<String> lastSampleKind = new AtomicReference<String>(null);
        final List<TimelineChunkAndTimes> chunksForHostAndSampleKind = new ArrayList<TimelineChunkAndTimes>();

        return new StreamingOutput()
        {
            @Override
            public void write(final OutputStream output) throws IOException, WebApplicationException
            {
                final ObjectWriter writer;
                if (compact) {
                    writer = objectMapper.writerWithView(TimelineChunksAndTimesViews.Compact.class);
                }
                else {
                    writer = objectMapper.writerWithView(TimelineChunksAndTimesViews.Loose.class);
                }
                final JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(output);

                if (pretty) {
                    generator.useDefaultPrettyPrinter();
                }

                generator.writeStartArray();
                try {
                    // writeJsonForChunks will merge in-memory and persisted samples
                    dao.getSamplesByHostNamesAndSampleKinds(hostNames, sampleKinds, startTime, endTime, new TimelineChunkAndTimesConsumer()
                    {
                        @Override
                        public void processTimelineChunkAndTimes(final TimelineChunkAndTimes chunkAndTimes)
                        {
                            try {
                                final String previousHostName = lastHostName.get();
                                final String previousSampleKind = lastSampleKind.get();
                                final String currentHostName = chunkAndTimes.getHostName();
                                final String currentSampleKind = chunkAndTimes.getSampleKind();
                                chunksForHostAndSampleKind.add(chunkAndTimes);

                                if (previousHostName != null && (!previousHostName.equals(currentHostName) || !previousSampleKind.equals(currentSampleKind))) {
                                    writeJsonForChunks(generator, writer, chunksForHostAndSampleKind, previousHostName, previousSampleKind, startTime, endTime, decodeSamples);
                                }

                                lastHostName.set(currentHostName);
                                lastSampleKind.set(currentSampleKind);
                            }
                            catch (Throwable e) {
                                // JDBI exception
                                throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
                            }
                        }
                    });

                    if (chunksForHostAndSampleKind.size() > 0) {
                        writeJsonForChunks(generator, writer, chunksForHostAndSampleKind, lastHostName.get(), lastSampleKind.get(), startTime, endTime, decodeSamples);
                    }
                }
                catch (Throwable e) {
                    // JDBI exception
                    throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
                }
                generator.writeEndArray();

                generator.flush();
                generator.close();
            }
        };
    }

    private void writeJsonForChunks(final JsonGenerator generator, final ObjectWriter writer, final List<TimelineChunkAndTimes> chunksForHostAndSampleKind,
                                    final String hostName, final String sampleKind, final DateTime startTime, final DateTime endTime, final boolean decodeSamples)
        throws IOException, ExecutionException
    {
        chunksForHostAndSampleKind.addAll(processor.getInMemoryTimelineChunkAndTimes(hostName, sampleKind, startTime, endTime));

        for (final TimelineChunkAndTimes chunk : chunksForHostAndSampleKind) {
            if (decodeSamples) {
                writer.writeValue(generator, new TimelineChunkAndTimesDecoded(chunk));
            }
            else {
                writer.writeValue(generator, chunk);
            }
        }

        chunksForHostAndSampleKind.clear();
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