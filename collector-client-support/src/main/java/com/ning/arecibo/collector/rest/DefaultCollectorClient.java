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

import com.google.inject.Inject;
import com.ning.arecibo.collector.CollectorClient;
import com.ning.arecibo.collector.discovery.CollectorFinder;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.spice.jersey.client.ahc.config.DefaultAhcConfig;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
    public Iterable<String> getSampleKinds() throws UniformInterfaceException
    {
        final TypeReference<List<String>> valueTypeRef = new TypeReference<List<String>>()
        {
        };
        final InputStream stream = getSampleKindsAsStream();

        return readValue(stream, valueTypeRef);
    }

    @Override
    public InputStream getSamplesByHostNameAsStream(final String hostName) throws UniformInterfaceException
    {
        return getSamplesByHostNameAsStream(hostName, new DateTime("0"), new DateTime(DateTimeZone.UTC));
    }

    @Override
    public Iterable<TimelineChunkAndTimes> getSamplesByHostName(final String hostName) throws UniformInterfaceException
    {
        return getSamplesByHostName(hostName, new DateTime("0"), new DateTime(DateTimeZone.UTC));
    }

    @Override
    public InputStream getSamplesByHostNameAsStream(final String hostName, final DateTime from) throws UniformInterfaceException
    {
        return getSamplesByHostNameAsStream(hostName, from, new DateTime(DateTimeZone.UTC));
    }

    @Override
    public Iterable<TimelineChunkAndTimes> getSamplesByHostName(final String hostName, final DateTime from) throws UniformInterfaceException
    {
        return getSamplesByHostName(hostName, from, new DateTime(DateTimeZone.UTC));
    }

    @Override
    public InputStream getSamplesByHostNameAsStream(final String hostName, final DateTime from, final DateTime to) throws UniformInterfaceException
    {
        final MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("from", from.toString());
        params.add("to", to.toString());

        return getPathAsStream(hostName, params);
    }

    @Override
    public Iterable<TimelineChunkAndTimes> getSamplesByHostName(final String hostName, final DateTime from, final DateTime to) throws UniformInterfaceException
    {
        final TypeReference<List<TimelineChunkAndTimes>> valueTypeRef = new TypeReference<List<TimelineChunkAndTimes>>()
        {
        };
        final InputStream stream = getSamplesByHostNameAsStream(hostName, from, to);

        return readValue(stream, valueTypeRef);
    }

    @Override
    public InputStream getSamplesByHostNameAndSampleKindAsStream(final String hostName, final String sampleKind) throws UniformInterfaceException
    {
        return getSamplesByHostNameAndSampleKindAsStream(hostName, sampleKind, new DateTime("0"), new DateTime(DateTimeZone.UTC));
    }

    @Override
    public Iterable<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(final String hostName, final String sampleKind) throws UniformInterfaceException
    {
        return getSamplesByHostNameAndSampleKind(hostName, sampleKind, new DateTime("0"), new DateTime(DateTimeZone.UTC));
    }

    @Override
    public InputStream getSamplesByHostNameAndSampleKindAsStream(final String hostName, final String sampleKind, final DateTime from) throws UniformInterfaceException
    {
        return getSamplesByHostNameAndSampleKindAsStream(hostName, sampleKind, from, new DateTime(DateTimeZone.UTC));
    }

    @Override
    public Iterable<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(final String hostName, final String sampleKind, final DateTime from) throws UniformInterfaceException
    {
        return getSamplesByHostNameAndSampleKind(hostName, sampleKind, new DateTime("0"), new DateTime(DateTimeZone.UTC));
    }

    @Override
    public InputStream getSamplesByHostNameAndSampleKindAsStream(final String hostName, final String sampleKind, final DateTime from, final DateTime to) throws UniformInterfaceException
    {
        final MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("from", from.toString());
        params.add("to", to.toString());

        return getPathAsStream(hostName + "/" + sampleKind, params);
    }

    @Override
    public Iterable<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(final String hostName, final String sampleKind, final DateTime from, final DateTime to) throws UniformInterfaceException
    {
        final TypeReference<List<TimelineChunkAndTimes>> valueTypeRef = new TypeReference<List<TimelineChunkAndTimes>>()
        {
        };
        final InputStream stream = getSamplesByHostNameAndSampleKindAsStream(hostName, sampleKind, from, to);

        return readValue(stream, valueTypeRef);
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
