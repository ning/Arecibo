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

package com.ning.arecibo.alert.endpoint;

import com.google.inject.Inject;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdQualifyingAttr;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * Qualifying Attributes are like event selectors, and are comprised of attribute name -> value pairs.
 * <p/>
 * These attributes must be part of the same event that the associated threshold is defined for. Any number of
 * Qualifying Attributes can be specified for a Threshold Definition (including zero). The most common examples of
 * Qualifying Attributes within Arecibo are for selecting by core type (e.g. 'deployedType->collector'), and/or for
 * selecting by config sub path (e.g. 'deployedConfigSubPath->aclu5').
 * <p/>
 * Any attribute within an Arecibo monitored event can be used as a Qualifying Attribute.
 *
 * @see com.ning.arecibo.event.MonitoringEvent
 */
@Path("/xn/rest/1.0/ThresholdQualifyingAttr")
public class ThresholdQualifyingAttrEndPoint extends ConfDataEndPoint<ConfDataThresholdQualifyingAttr>
{
    @Inject
    public ThresholdQualifyingAttrEndPoint(final ConfDataDAO dao)
    {
        super(dao, ConfDataThresholdQualifyingAttr.TYPE_NAME, ConfDataThresholdQualifyingAttr.class);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getById(@PathParam("id") final Long id)
    {
        return findConfDataObjectById(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(final ConfDataThresholdQualifyingAttr thresholdQualifyingAttr)
    {
        return createConfDataObject(thresholdQualifyingAttr);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteById(@PathParam("id") final Long id)
    {
        return deleteConfDataObjectById(id);
    }
}
