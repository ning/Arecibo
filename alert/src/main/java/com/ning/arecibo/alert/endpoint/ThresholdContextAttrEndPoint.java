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
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdContextAttr;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * Context Attributes provide bookkeeping for alerts once they have been triggered.
 * <p/>
 * They indicate a particular attributeType, whose value is used to independently track alerts, which are otherwise
 * triggered by the same Threshold Definition. Any number of Context Attributes can be specified for a Threshold Definition
 * (including zero).
 * <p/>
 * Any attribute within an Arecibo monitored event can be used as a Context Attribute. The most common
 * example of a Context Attribute is 'hostName'. This allows specifying a single Threshold Definition that can be used
 * to monitor a single metric for any number of individual hosts separately.
 */
@Path("/xn/rest/1.0/ThresholdContextAttr")
public class ThresholdContextAttrEndPoint extends ConfDataEndPoint<ConfDataThresholdContextAttr>
{
    @Inject
    public ThresholdContextAttrEndPoint(final ConfDataDAO dao)
    {
        super(dao, ConfDataThresholdContextAttr.TYPE_NAME, ConfDataThresholdContextAttr.class);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getById(@PathParam("id") final Long id)
    {
        return findConfDataObjectById(id);
    }

    @GET
    @Path("/ThresholdConfig/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getAllByThresholdConfigId(@PathParam("id") final Long thresholdConfigId)
    {
        return findConfDataObjectById("threshold_config_id", thresholdConfigId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(final ConfDataThresholdContextAttr thresholdContextAttr)
    {
        return createConfDataObject(thresholdContextAttr);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteById(@PathParam("id") final Long id)
    {
        return deleteConfDataObjectById(id);
    }
}
