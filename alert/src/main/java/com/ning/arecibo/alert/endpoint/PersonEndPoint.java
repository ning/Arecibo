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
import com.ning.arecibo.alert.confdata.objects.ConfDataPerson;

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
 * There are 2 types of contacts that can receive emails, either an individual Person or an Email Group Alias.
 * <p/>
 * We want to know if a contact is a Person or not, since it will be used as part of the Alert Acknowledgement system.
 * Only Persons will be allowed to acknowledge alerts. A Person or Alias can be associated with a Notification Group.
 */
@Path("/xn/rest/1.0/Person")
public class PersonEndPoint extends ConfDataEndPoint<ConfDataPerson>
{
    @Inject
    public PersonEndPoint(final ConfDataDAO dao)
    {
        super(dao, ConfDataPerson.TYPE_NAME, ConfDataPerson.class);
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
    public Response create(final ConfDataPerson person)
    {
        return createConfDataObject(person);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteById(@PathParam("id") final Long id)
    {
        return deleteConfDataObjectById(id);
    }
}
