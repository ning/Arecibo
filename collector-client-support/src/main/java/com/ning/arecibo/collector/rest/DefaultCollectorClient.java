package com.ning.arecibo.collector.rest;

import com.google.inject.Inject;
import com.ning.arecibo.collector.CollectorClient;
import com.ning.arecibo.collector.discovery.CollectorFinder;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;
import com.sun.jersey.api.client.Client;
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
    public InputStream getHostsAsStream()
    {
        return getPathAsStream("hosts");
    }

    @Override
    public Iterable<String> getHosts()
    {
        final TypeReference<List<String>> valueTypeRef = new TypeReference<List<String>>()
        {
        };
        final InputStream stream = getHostsAsStream();

        return readValue(stream, valueTypeRef);
    }

    @Override
    public InputStream getSampleKindsAsStream()
    {
        return getPathAsStream("sample_kinds");
    }

    @Override
    public Iterable<String> getSampleKinds()
    {
        final TypeReference<List<String>> valueTypeRef = new TypeReference<List<String>>()
        {
        };
        final InputStream stream = getSampleKindsAsStream();

        return readValue(stream, valueTypeRef);
    }

    @Override
    public InputStream getSamplesByHostNameAsStream(final String hostName)
    {
        return getSamplesByHostNameAsStream(hostName, new DateTime("0"), new DateTime(DateTimeZone.UTC));
    }

    @Override
    public Iterable<TimelineChunkAndTimes> getSamplesByHostName(final String hostName)
    {
        return getSamplesByHostName(hostName, new DateTime("0"), new DateTime(DateTimeZone.UTC));
    }

    @Override
    public InputStream getSamplesByHostNameAsStream(final String hostName, final DateTime from)
    {
        return getSamplesByHostNameAsStream(hostName, from, new DateTime(DateTimeZone.UTC));
    }

    @Override
    public Iterable<TimelineChunkAndTimes> getSamplesByHostName(final String hostName, final DateTime from)
    {
        return getSamplesByHostName(hostName, from, new DateTime(DateTimeZone.UTC));
    }

    @Override
    public InputStream getSamplesByHostNameAsStream(final String hostName, final DateTime from, final DateTime to)
    {
        final MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("from", from.toString());
        params.add("to", to.toString());

        return getPathAsStream(hostName, params);
    }

    @Override
    public Iterable<TimelineChunkAndTimes> getSamplesByHostName(final String hostName, final DateTime from, final DateTime to)
    {
        final TypeReference<List<TimelineChunkAndTimes>> valueTypeRef = new TypeReference<List<TimelineChunkAndTimes>>()
        {
        };
        final InputStream stream = getSamplesByHostNameAsStream(hostName, from, to);

        return readValue(stream, valueTypeRef);
    }

    @Override
    public InputStream getSamplesByHostNameAndSampleKindAsStream(final String hostName, final String sampleKind)
    {
        return getSamplesByHostNameAndSampleKindAsStream(hostName, sampleKind, new DateTime("0"), new DateTime(DateTimeZone.UTC));
    }

    @Override
    public Iterable<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(final String hostName, final String sampleKind)
    {
        return getSamplesByHostNameAndSampleKind(hostName, sampleKind, new DateTime("0"), new DateTime(DateTimeZone.UTC));
    }

    @Override
    public InputStream getSamplesByHostNameAndSampleKindAsStream(final String hostName, final String sampleKind, final DateTime from)
    {
        return getSamplesByHostNameAndSampleKindAsStream(hostName, sampleKind, from, new DateTime(DateTimeZone.UTC));
    }

    @Override
    public Iterable<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(final String hostName, final String sampleKind, final DateTime from)
    {
        return getSamplesByHostNameAndSampleKind(hostName, sampleKind, new DateTime("0"), new DateTime(DateTimeZone.UTC));
    }

    @Override
    public InputStream getSamplesByHostNameAndSampleKindAsStream(final String hostName, final String sampleKind, final DateTime from, final DateTime to)
    {
        final MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("from", from.toString());
        params.add("to", to.toString());

        return getPathAsStream(hostName + "/" + sampleKind, params);
    }

    @Override
    public Iterable<TimelineChunkAndTimes> getSamplesByHostNameAndSampleKind(final String hostName, final String sampleKind, final DateTime from, final DateTime to)
    {
        final TypeReference<List<TimelineChunkAndTimes>> valueTypeRef = new TypeReference<List<TimelineChunkAndTimes>>()
        {
        };
        final InputStream stream = getSamplesByHostNameAsStream(hostName, from, to);

        return readValue(stream, valueTypeRef);
    }

    private void createClient()
    {
        final DefaultAhcConfig config = new DefaultAhcConfig();
        client = Client.create(config);
    }

    private InputStream getPathAsStream(final String path)
    {
        return getPathAsStream(path, null);
    }

    private InputStream getPathAsStream(final String path, @Nullable final MultivaluedMap<String, String> queryParams)
    {
        final WebResource resource = createWebResource();
        if (queryParams != null) {
            return resource.path(path).queryParams(queryParams).get(InputStream.class);
        }
        else {
            return resource.path(path).get(InputStream.class);
        }
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
            return mapper.readValue(stream, valueTypeRef);
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
