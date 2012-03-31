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
import com.ning.arecibo.util.timeline.SamplesForSampleKindAndHost;
import com.ning.jaxrs.DateTimeParameter;
import com.ning.jersey.metrics.TimedResource;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.util.JSONPObject;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    @Path("/host_samples")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public Response getSamplesByHostName(@QueryParam("callback") @DefaultValue("callback") final String callback,
                                         @QueryParam("from") @DefaultValue("") final DateTimeParameter startTimeParameter,
                                         @QueryParam("to") @DefaultValue("") final DateTimeParameter endTimeParameter,
                                         @QueryParam("host") final List<String> hostNames,
                                         @QueryParam("category_and_sample_kind") final List<String> categoriesAndSampleKinds)
    {
        try {
            final Iterable<SamplesForSampleKindAndHost> samplesByHostName = client.getHostSamples(hostNames, categoriesAndSampleKinds, startTimeParameter.getValue(), endTimeParameter.getValue());
            final JSONPObject object = new JSONPObject(callback, samplesByHostName);
            return Response.ok(object).build();
        }
        catch (Throwable t) {
            // Likely UniformInterfaceException from the collector client library
            throw new WebApplicationException(t.getCause(), buildServiceUnavailableResponse());
        }
    }

    private Response buildServiceUnavailableResponse()
    {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
}
