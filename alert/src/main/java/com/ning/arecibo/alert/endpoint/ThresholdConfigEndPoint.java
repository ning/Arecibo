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
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdConfig;

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
 * A Threshold Definition is a specific rule for defining conditions that should trigger an alert. It is specified by
 * choosing an Event Type and an associated Attribute Type. These must correspond to valid, monitored events and attributes
 * within the Arecibo system, in order to be triggered. These types are case sensitive, so be sure to spell them correctly,
 * observing case. You can refer to the Arecibo Dashboard to view currently active monitored events, for spelling.
 * The specified attribute type must be a numeric valued field.
 * <p/>
 * A Threshold Definition includes a reference to an Alerting Configuration, which allows sharing configuration between
 * multiple threshold definitions. Each Threshold Definition requires an existing Alerting Configuration, so it may be
 * necessary to set up an Alerting Configuration before creating a Threshold Definition.
 * <p/>
 * There are several Options that can be specified for a Threshold Definition.
 * <p/>
 * Min & Max Threshold: These are the values that must violated in order to trigger an alert. At least one of (or both)
 * Min and Max Thresholds must be specified. The thresholds must be numeric. Thresholds are interpreted as non-inclusive limits.
 * <p/>
 * The Min Samples option provides for a minimum number of samples required to trigger an alert (the default being 1).
 * This allows requiring multiple samples that exceed the specified threshold, within the specified Max Sample Window.
 * This can guard against spurious noise in the event data.
 * <p/>
 * The Clearing Interval is a required field which indicates the minimum amount of time required to pass before a triggered
 * alert will become de-activated, and no longer in an alerting state. The default for this field is 300000 ms (i.e. 5 minutes).
 */
@Path("/xn/rest/1.0/ThresholdConfig")
public class ThresholdConfigEndPoint extends ConfDataEndPoint<ConfDataThresholdConfig>
{
    @Inject
    public ThresholdConfigEndPoint(final ConfDataDAO dao)
    {
        super(dao, ConfDataThresholdConfig.TYPE_NAME, ConfDataThresholdConfig.class);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Iterable<Map<String, Object>> getAll()
    {
        return findAllConfDataObject();
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
    public Response create(final ConfDataThresholdConfig thresholdConfig)
    {
        return createConfDataObject(thresholdConfig);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteById(@PathParam("id") final Long id)
    {
        return deleteConfDataObjectById(id);
    }
}
