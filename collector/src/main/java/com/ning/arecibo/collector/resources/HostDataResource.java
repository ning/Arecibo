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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;
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

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.joda.time.DateTime;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import com.ning.arecibo.collector.persistent.TimelineEventHandler;
import com.ning.arecibo.util.timeline.CategoryIdAndSampleKind;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimesConsumer;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimesDecoded;
import com.ning.arecibo.util.timeline.TimelineChunksAndTimesViews;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.ning.jaxrs.DateTimeParameter;
import com.ning.jersey.metrics.TimedResource;

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
        catch (CacheLoader.InvalidCacheLoadException e) {
            throw new WebApplicationException(e, Response.Status.NOT_FOUND);
        }
        catch (RuntimeException e) {
            // JDBI exception
            throw new WebApplicationException(e, buildServiceUnavailableResponse());
        }
    }

    @GET
    @Path("/sample_kinds")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public StreamingOutput getSampleKinds(@QueryParam("host") final List<String> hostNames,
                                          @QueryParam("pretty") @DefaultValue("false") final boolean pretty)
    {
        try {
            if (hostNames == null || hostNames.isEmpty()) {
                final BiMap<Integer, CategoryIdAndSampleKind> sampleKinds = dao.getSampleKinds();
                return streamResponse(sampleKinds.values(), pretty);
            }
            else {
                // Return the union of all sample kinds available for these hosts
                final Set<CategoryIdAndSampleKind> sampleKinds = findCategoryIdsAndSampleKindsForHosts(hostNames);
                return streamResponse(sampleKinds, pretty);
            }
        }
        catch (CacheLoader.InvalidCacheLoadException e) {
            throw new WebApplicationException(e, Response.Status.NOT_FOUND);
        }
        catch (RuntimeException e) {
            // JDBI exception
            throw new WebApplicationException(e, buildServiceUnavailableResponse());
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
        try {
            final Integer hostId = dao.getHostId(hostName);
            final Iterable<Integer> sampleKindIdsIterable = dao.getSampleKindIdsByHostId(hostId);

            return getHostSamplesUsingIds(startTimeParameter, endTimeParameter, pretty, decodeSamples, compact, ImmutableList.<Integer>of(hostId), ImmutableList.<Integer>copyOf(sampleKindIdsIterable));
        }
        catch (CacheLoader.InvalidCacheLoadException e) {
            throw new WebApplicationException(e, Response.Status.NOT_FOUND);
        }
        catch (RuntimeException e) {
            // JDBI exception
            throw new WebApplicationException(e, buildServiceUnavailableResponse());
        }
    }

    @GET
    @Path("/{host}/{category_and_sample_kind}")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public StreamingOutput getSamplesByHostNameAndSampleKind(@QueryParam("from") @DefaultValue("") final DateTimeParameter startTimeParameter,
                                                             @QueryParam("to") @DefaultValue("") final DateTimeParameter endTimeParameter,
                                                             @QueryParam("pretty") @DefaultValue("false") final boolean pretty,
                                                             @QueryParam("decode_samples") @DefaultValue("false") final boolean decodeSamples,
                                                             @QueryParam("compact") @DefaultValue("false") final boolean compact,
                                                             @PathParam("host") final String hostName,
                                                             @PathParam("category_and_sample_kind") final String categoryAndSampleKind) throws IOException
    {
        final Integer hostId = dao.getHostId(hostName);
        if (hostId == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        final Integer sampleKindId = getSampleKindIdFromQueryParameter(categoryAndSampleKind);
        return getHostSamplesUsingIds(startTimeParameter, endTimeParameter, pretty, decodeSamples, compact, ImmutableList.<Integer>of(hostId), ImmutableList.<Integer>of(sampleKindId));
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
                                          @QueryParam("category_and_sample_kind") final List<String> categoriesAndSampleKinds)
    {
        if (hostNames == null || hostNames.isEmpty()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        final List<Integer> hostIds = translateHostNamesToHostIds(hostNames);
        final List<Integer> sampleKindIds = translateCategoriesAndSampleKindsToSampleKindIds(categoriesAndSampleKinds);
        return getHostSamplesUsingIds(startTimeParameter, endTimeParameter, pretty, decodeSamples, compact, hostIds, sampleKindIds);
    }

    @GET
    @Path("/host_samples_using_ids")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public StreamingOutput getHostSamplesUsingIds(@QueryParam("from") @DefaultValue("") final DateTimeParameter startTimeParameter,
                                                  @QueryParam("to") @DefaultValue("") final DateTimeParameter endTimeParameter,
                                                  @QueryParam("pretty") @DefaultValue("false") final boolean pretty,
                                                  @QueryParam("decode_samples") @DefaultValue("false") final boolean decodeSamples,
                                                  @QueryParam("compact") @DefaultValue("false") final boolean compact,
                                                  @QueryParam("host_id") final List<Integer> hostIds,
                                                  @QueryParam("sample_kind_id") final List<Integer> sampleKindIds)
    {
        final DateTime startTime = startTimeParameter.getValue();
        final DateTime endTime = endTimeParameter.getValue();

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
                    writeJsonForAllChunks(generator, writer, hostIds, sampleKindIds, startTime, endTime, decodeSamples);
                }
                catch (CacheLoader.InvalidCacheLoadException e) {
                    throw new WebApplicationException(e, Response.Status.NOT_FOUND);
                }
                catch (RuntimeException e) {
                    // JDBI exception
                    throw new WebApplicationException(e, buildServiceUnavailableResponse());
                }
                catch (ExecutionException e) {
                    throw new WebApplicationException(e, buildServiceUnavailableResponse());
                }
                generator.writeEndArray();

                generator.flush();
                generator.close();
            }
        };
    }

    private void writeJsonForAllChunks(final JsonGenerator generator, final ObjectWriter writer, final List<Integer> hostIds,
                                       final List<Integer> sampleKindIds, final DateTime startTime, final DateTime endTime, final boolean decodeSamples)
        throws IOException, ExecutionException
    {
        // First, return all data in memory.
        // Data won't be merged with the on-disk one because we don't want to buffer samples in memory.
        writeJsonForInMemoryChunks(generator, writer, hostIds, sampleKindIds, startTime, endTime, decodeSamples);

        // Now, return all data stored in the database
        writeJsonForStoredChunks(generator, writer, hostIds, sampleKindIds, startTime, endTime, decodeSamples);
    }

    @VisibleForTesting
    void writeJsonForInMemoryChunks(final JsonGenerator generator, final ObjectWriter writer, final List<Integer> hostIdsList,
                                    final List<Integer> sampleKindIdsList, @Nullable final DateTime startTime, @Nullable final DateTime endTime, final boolean decodeSamples)
        throws IOException, ExecutionException
    {
        for (final Integer hostId : hostIdsList) {
            final Collection<? extends TimelineChunkAndTimes> inMemorySamples = processor.getInMemoryTimelineChunkAndTimes(hostId, sampleKindIdsList, startTime, endTime);
            writeJsonForChunks(generator, writer, inMemorySamples, decodeSamples);
        }
    }

    private void writeJsonForStoredChunks(final JsonGenerator generator, final ObjectWriter writer, final List<Integer> hostIdsList,
                                          final List<Integer> sampleKindIdsList, final DateTime startTime, final DateTime endTime, final boolean decodeSamples)
        throws IOException, ExecutionException
    {
        final AtomicReference<Integer> lastHostId = new AtomicReference<Integer>(null);
        final AtomicReference<Integer> lastSampleKindId = new AtomicReference<Integer>(null);
        final List<TimelineChunkAndTimes> chunksForHostAndSampleKind = new ArrayList<TimelineChunkAndTimes>();

        dao.getSamplesByHostIdsAndSampleKindIds(hostIdsList, sampleKindIdsList, startTime, endTime, new TimelineChunkAndTimesConsumer()
        {
            @Override
            public void processTimelineChunkAndTimes(final TimelineChunkAndTimes chunkAndTimes)
            {
                final Integer previousHostId = lastHostId.get();
                final Integer previousSampleKindId = lastSampleKindId.get();
                final Integer currentHostId = chunkAndTimes.getHostId();
                final Integer currentSampleKindId = chunkAndTimes.getSampleKindId();

                chunksForHostAndSampleKind.add(chunkAndTimes);
                if (previousHostId != null && (!previousHostId.equals(currentHostId) || !previousSampleKindId.equals(currentSampleKindId))) {
                    try {
                        writeJsonForChunks(generator, writer, chunksForHostAndSampleKind, decodeSamples);
                    }
                    catch (RuntimeException e) {
                        // JDBI exception
                        throw new WebApplicationException(e, buildServiceUnavailableResponse());
                    }
                    catch (IOException e) {
                        throw new WebApplicationException(e, buildServiceUnavailableResponse());
                    }
                    catch (ExecutionException e) {
                        throw new WebApplicationException(e, buildServiceUnavailableResponse());
                    }
                    chunksForHostAndSampleKind.clear();
                }

                lastHostId.set(currentHostId);
                lastSampleKindId.set(currentSampleKindId);
            }
        });

        if (chunksForHostAndSampleKind.size() > 0) {
            writeJsonForChunks(generator, writer, chunksForHostAndSampleKind, decodeSamples);
            chunksForHostAndSampleKind.clear();
        }
    }

    private void writeJsonForChunks(final JsonGenerator generator, final ObjectWriter writer, final Iterable<? extends TimelineChunkAndTimes> chunksForHostAndSampleKind, final boolean decodeSamples)
        throws IOException, ExecutionException
    {
        for (final TimelineChunkAndTimes chunk : chunksForHostAndSampleKind) {
            if (decodeSamples) {
                writer.writeValue(generator, new TimelineChunkAndTimesDecoded(chunk));
            }
            else {
                writer.writeValue(generator, chunk);
            }
        }
    }

    @VisibleForTesting
    Set<CategoryIdAndSampleKind> findCategoryIdsAndSampleKindsForHosts(final List<String> hostNames) throws CacheLoader.InvalidCacheLoadException
    {
        // Note: all of this is usually cached
        final Set<CategoryIdAndSampleKind> sampleKinds = new HashSet<CategoryIdAndSampleKind>();

        for (final String hostName : hostNames) {
            final Integer hostId = dao.getHostId(hostName);
            if (hostId == null) {
                continue;
            }

            for (final Integer sampleKindId : dao.getSampleKindIdsByHostId(hostId)) {
                final CategoryIdAndSampleKind categoryIdAndSampleKind = dao.getCategoryIdAndSampleKind(sampleKindId);
                if (categoryIdAndSampleKind != null) {
                    sampleKinds.add(categoryIdAndSampleKind);
                }
            }
        }

        return sampleKinds;
    }

    @VisibleForTesting
    Set<Integer> findSampleKindIdsForHosts(final List<String> hostNames) throws CacheLoader.InvalidCacheLoadException
    {
        // Note: all of this is usually cached
        final Set<Integer> sampleKindIds = new HashSet<Integer>();

        for (final String hostName : hostNames) {
            final Integer hostId = dao.getHostId(hostName);
            if (hostId == null) {
                continue;
            }

            for (final Integer sampleKindId : dao.getSampleKindIdsByHostId(hostId)) {
                sampleKindIds.add(sampleKindId);
            }
        }

        return sampleKindIds;
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

    private int getSampleKindIdFromQueryParameter(final String categoryAndSampleKind)
    {
        final String[] parts = categoryAndSampleKind.split(",");
        if (parts.length != 2) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        final String eventCategory = parts[0];
        final String sampleKind = parts[1];
        final Integer eventCategoryId = dao.getEventCategoryId(eventCategory);
        if (eventCategoryId == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        final Integer sampleKindId = dao.getSampleKindId(eventCategoryId, sampleKind);
        if (sampleKindId == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return sampleKindId;
    }

    private List<Integer> translateHostNamesToHostIds(final List<String> hostNames)
    {
        final List<Integer> hostIds = new ArrayList<Integer>(hostNames.size());
        for (final String hostName : hostNames) {
            hostIds.add(dao.getHostId(hostName));
        }
        return hostIds;
    }

    private List<Integer> translateCategoriesAndSampleKindsToSampleKindIds(final List<String> categoriesAndSampleKinds)
    {
        final List<Integer> sampleKindIds = new ArrayList<Integer>(categoriesAndSampleKinds.size());
        for (final String categoryAndSampleKind : categoriesAndSampleKinds) {
            sampleKindIds.add(getSampleKindIdFromQueryParameter(categoryAndSampleKind));
        }
        return sampleKindIds;
    }
}
