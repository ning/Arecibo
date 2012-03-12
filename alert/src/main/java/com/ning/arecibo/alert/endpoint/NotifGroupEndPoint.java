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
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroup;

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
 * A Notification Group is a way for grouping multiple recipients to receive the same alert notification.
 * <p/>
 * A Notification Group can be associated with one or more Alerting Configurations. A Notification Group must include at
 * least one email contact, which can be for an individual person or an email alias. It may be necessary to set up a
 * Person or Alias before creating a Threshold Definition.
 */
@Path("/xn/rest/1.0/NotifGroup")
public class NotifGroupEndPoint extends ConfDataEndPoint<ConfDataNotifGroup>
{
    @Inject
    public NotifGroupEndPoint(final ConfDataDAO dao)
    {
        super(dao, ConfDataNotifGroup.TYPE_NAME, ConfDataNotifGroup.class);
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
    public Response create(final ConfDataNotifGroup notifGroup)
    {
        return createConfDataObject(notifGroup);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteById(@PathParam("id") final Long id)
    {
        return deleteConfDataObjectById(id);
    }
}
