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

package com.ning.arecibo.collector.rest;

import com.ning.arecibo.collector.CollectorClient;
import com.ning.arecibo.collector.discovery.CollectorFinder;
import com.ning.arecibo.util.timeline.CategoryAndSampleKinds;
import com.ning.arecibo.util.timeline.CategoryAndSampleKindsForHosts;
import com.ning.arecibo.util.timeline.SamplesForSampleKindAndHost;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.spice.jersey.client.ahc.config.DefaultAhcConfig;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of the Collector client.
 * <p/>
 * When using the stream APIs, it is the responsibility of the client to close the streams.
 */
public class DefaultCollectorClient implements CollectorClient
{
    private static final Logger log = LoggerFactory.getLogger(DefaultCollectorClient.class);
    private static final String USER_AGENT = "NING-CollectorClient/1.0";
    private static final String RESOURCE_PATH = "rest/1.0";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final CollectorFinder collectorFinder;

    private Client client;

    @Inject
    public DefaultCollectorClient(final CollectorFinder collectorFinder)
    {
        this.collectorFinder = collectorFinder;
        createClient();
    }

    @Override
    public InputStream getHostsAsStream() throws UniformInterfaceException
    {
        return getPathAsStream("hosts");
    }

    @Override
    public Iterable<String> getHosts() throws UniformInterfaceException
    {
        final TypeReference<List<String>> valueTypeRef = new TypeReference<List<String>>()
        {
        };
        final InputStream stream = getHostsAsStream();

        return readValue(stream, valueTypeRef);
    }

    @Override
    public InputStream getSampleKindsAsStream() throws UniformInterfaceException
    {
        return getPathAsStream("sample_kinds");
    }

    @Override
    public Iterable<CategoryAndSampleKindsForHosts> getSampleKinds() throws UniformInterfaceException
    {
        final TypeReference<List<CategoryAndSampleKindsForHosts>> valueTypeRef = new TypeReference<List<CategoryAndSampleKindsForHosts>>()
        {
        };
        final InputStream stream = getSampleKindsAsStream();

        return readValue(stream, valueTypeRef);
    }

    @Override
    public InputStream getHostSamplesAsStream(final Iterable<String> hostNames, final Iterable<String> categoriesAndSampleKinds, final DateTime from, final DateTime to, final Integer outputCount) throws UniformInterfaceException
    {
        final MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        for (final String hostName : hostNames) {
            params.add("host", hostName);
        }
        for (final String categoriesAndSampleKind : categoriesAndSampleKinds) {
            params.add("category_and_sample_kind", categoriesAndSampleKind);
        }

        params.add("from", from.toString());
        params.add("to", to.toString());
        params.add("output_count", String.valueOf(outputCount));

        return getPathAsStream("host_samples", params);
    }

    @Override
    public Iterable<SamplesForSampleKindAndHost> getHostSamples(final Iterable<String> hostNames, final Iterable<String> categoriesAndSampleKinds, final DateTime from, final DateTime to, final Integer outputCount) throws UniformInterfaceException
    {
        final TypeReference<List<SamplesForSampleKindAndHost>> valueTypeRef = new TypeReference<List<SamplesForSampleKindAndHost>>()
        {
        };
        final InputStream stream = getHostSamplesAsStream(hostNames, categoriesAndSampleKinds, from, to, outputCount);

        // The streaming endpoint will send data out as fast as possible, so we may end up having
        // multiple SamplesForSampleKindAndHost per host and sample kind. Let's merge them for convenience.
        //[ {
        //  "hostName" : "abc.foo.com",
        //  "eventCategory" : "JVM",
        //  "sampleKind" : "GC"
        //  "samples" : "1,20,2,23"
        //  },
        //  {
        //  "hostName" : "abc.foo.com",
        //  "eventCategory" : "JVM",
        //  "sampleKind" : "GC"
        //  "samples" : "3,22,4,20"
        //} ]
        final Iterable<SamplesForSampleKindAndHost> streamedSamples = readValue(stream, valueTypeRef);
        final Map<String, Map<String, Map<String, SamplesForSampleKindAndHost>>> mergedSamplesMap = new HashMap<String, Map<String, Map<String, SamplesForSampleKindAndHost>>>();
        for (final SamplesForSampleKindAndHost sample : streamedSamples) {
            if (mergedSamplesMap.get(sample.getHostName()) == null) {
                mergedSamplesMap.put(sample.getHostName(), new HashMap<String, Map<String, SamplesForSampleKindAndHost>>());
            }
            final Map<String, Map<String, SamplesForSampleKindAndHost>> samplesForHost = mergedSamplesMap.get(sample.getHostName());

            if (samplesForHost.get(sample.getEventCategory()) == null) {
                samplesForHost.put(sample.getEventCategory(), new HashMap<String, SamplesForSampleKindAndHost>());
            }
            final Map<String, SamplesForSampleKindAndHost> samplesForHostAndEventCategory = samplesForHost.get(sample.getEventCategory());

            // Samples might be empty (especially with the DecimatingSampleFilter).
            // Don't add empty coordinates (i.e. ,,) which would be misinterpreted by the UI as 0 values
            if (Strings.isNullOrEmpty(sample.getSamples())) {
                continue;
            }

            final String samples;
            if (samplesForHostAndEventCategory.get(sample.getSampleKind()) == null) {
                samples = sample.getSamples();
            }
            else {
                samples = samplesForHostAndEventCategory.get(sample.getSampleKind()).getSamples() +
                        "," + sample.getSamples();
            }

            final SamplesForSampleKindAndHost newSamples = new SamplesForSampleKindAndHost(sample.getHostName(), sample.getEventCategory(), sample.getSampleKind(), samples);
            samplesForHostAndEventCategory.put(sample.getSampleKind(), newSamples);
        }

        final List<SamplesForSampleKindAndHost> mergedSamples = new ArrayList<SamplesForSampleKindAndHost>();
        for (final Map<String, Map<String, SamplesForSampleKindAndHost>> samplesForHost : mergedSamplesMap.values()) {
            for (final Map<String, SamplesForSampleKindAndHost> samplesForHostAndEventCategory : samplesForHost.values()) {
                mergedSamples.addAll(samplesForHostAndEventCategory.values());
            }
        }

        return mergedSamples;
    }

    private void createClient()
    {
        final DefaultAhcConfig config = new DefaultAhcConfig();
        client = Client.create(config);
    }

    private InputStream getPathAsStream(final String path) throws UniformInterfaceException
    {
        return getPathAsStream(path, null);
    }

    private InputStream getPathAsStream(final String path, @Nullable final MultivaluedMap<String, String> queryParams) throws UniformInterfaceException
    {
        WebResource resource = createWebResource().path(path);
        if (queryParams != null) {
            resource = resource.queryParams(queryParams);
        }

        log.info("Calling: {}", resource.toString());
        return resource.get(InputStream.class);
    }

    private WebResource createWebResource()
    {
        String collectorUri = collectorFinder.getCollectorUri();
        if (!collectorUri.endsWith("/")) {
            collectorUri += "/";
        }
        collectorUri += RESOURCE_PATH;

        final WebResource resource = client.resource(collectorUri);
        resource.accept(MediaType.APPLICATION_JSON).header("User-Agent", USER_AGENT);

        return resource;
    }

    private <T> T readValue(final InputStream stream, final TypeReference<T> valueTypeRef)
    {
        try {
            return mapper.<T>readValue(stream, valueTypeRef);
        }
        catch (JsonMappingException e) {
            log.warn("Failed to map response from collector", e);
        }
        catch (JsonParseException e) {
            log.warn("Failed to parse response from collector", e);
        }
        catch (IOException e) {
            log.warn("Generic I/O Exception from collector", e);
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (IOException e) {
                    log.warn("Failed to close http-client - provided InputStream", e);
                }
            }
        }

        return null;
    }
}
