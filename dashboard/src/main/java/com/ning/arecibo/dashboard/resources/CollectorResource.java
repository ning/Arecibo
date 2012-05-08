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
import com.ning.arecibo.dashboard.config.LegendConfigurationsManager;
import com.ning.arecibo.util.timeline.SamplesForSampleKindAndHost;
import com.ning.jaxrs.DateTimeParameter;
import com.ning.jersey.metrics.TimedResource;

import com.google.inject.Singleton;
import org.codehaus.jackson.map.util.JSONPObject;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.util.List;

@Singleton
@Path("/rest/1.0")
public class CollectorResource
{
    private final HostsStore hostsStore;
    private final GroupsAndSampleKindsStore groupsAndSampleKindsStore;
    private final LegendConfigurationsManager legendsManager;
    private final CollectorClient client;

    @Inject
    public CollectorResource(final HostsStore hostsStore,
                             final GroupsAndSampleKindsStore groupsAndSampleKindsStore,
                             final LegendConfigurationsManager legendsManager,
                             final CollectorClient client)
    {
        this.hostsStore = hostsStore;
        this.groupsAndSampleKindsStore = groupsAndSampleKindsStore;
        this.legendsManager = legendsManager;
        this.client = client;
    }

    @GET
    @Path("/hosts")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public Response getHosts(@Context final Request request,
                             @QueryParam("callback") @DefaultValue("callback") final String callback)
    {
        try {
            final String etag = hostsStore.getEtag();
            final Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(new EntityTag(etag));
            if (responseBuilder != null) {
                return responseBuilder.tag(etag).build();
            }

            final JSONPObject object = new JSONPObject(callback, hostsStore);
            return Response.ok(object).tag(etag).build();
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
    public Response getSampleKinds(@Context final Request request,
                                   @QueryParam("callback") @DefaultValue("callback") final String callback)
    {
        final String etag = groupsAndSampleKindsStore.getEtag();
        final Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(new EntityTag(etag));
        if (responseBuilder != null) {
            return responseBuilder.tag(etag).build();
        }

        try {
            final JSONPObject object = new JSONPObject(callback, groupsAndSampleKindsStore);
            return Response.ok(object).tag(etag).build();
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
                                         @QueryParam("output_count") final Integer outputCount,
                                         @QueryParam("category_and_sample_kind") final List<String> categoriesAndSampleKinds)
    {
        try {
            final Iterable<SamplesForSampleKindAndHost> samplesByHostName = client.getHostSamples(hostNames, categoriesAndSampleKinds, startTimeParameter.getValue(), endTimeParameter.getValue(), outputCount);
            final JSONPObject object = new JSONPObject(callback, samplesByHostName);
            return Response.ok(object).build();
        }
        catch (Throwable t) {
            // Likely UniformInterfaceException from the collector client library
            throw new WebApplicationException(t.getCause(), buildServiceUnavailableResponse());
        }
    }

    @GET
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    @TimedResource
    public Response getDashboardConfig(@QueryParam("callback") @DefaultValue("callback") final String callback)
    {
        final JSONPObject object = new JSONPObject(callback, legendsManager.getConfiguration());
        return Response.ok(object).build();
    }

    private Response buildServiceUnavailableResponse()
    {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
}
