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
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;

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

@Path("/xn/rest/1.0/NotifConfig")
public class NotifConfigEndPoint extends ConfDataEndPoint<ConfDataNotifConfig>
{
    @Inject
    public NotifConfigEndPoint(final ConfDataDAO dao)
    {
        super(dao, ConfDataNotifConfig.TYPE_NAME, ConfDataNotifConfig.class);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getById(@PathParam("id") final Long id)
    {
        return findConfDataObjectById(id);
    }

    @GET
    @Path("/user/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> getByPersonId(@PathParam("id") final Long personId)
    {
        return findConfDataObjectById("person_id", personId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(final ConfDataNotifConfig notifConfig)
    {
        return createConfDataObject(notifConfig);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteById(@PathParam("id") final Long id)
    {
        return deleteConfDataObjectById(id);
    }
}
