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
import com.ning.arecibo.alert.confdata.objects.ConfDataAlertingConfig;

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
 * An Alerting configuration allows defining notification options, that can be shared by one or more Threshold Definitions.
 * Thus, Alerting Configurations allow managing groups of alerts with a single configuration.
 * <p/>
 * An Alerting Configuration includes references to Notification Groups, which allow sending notification to multiple
 * recipients. At least 1 Notification Group must be specified for each Alerting Configuration, so it may be necessary to
 * set up a Notification Group before creating an Alerting Configuration.
 * <p/>
 * Also, Managing Rules can be specified, so that all associated Threshold Definitions can be suppressed by schedule or
 * manually. Managing Rules are optional for Alerting Configurations.
 * <p/>
 * There is an option to enable an Alerting Configuration, and this must be selected in order for any alerting
 * notification or internal state management to occur.
 * <p/>
 * The Repeat Mode option allows customizing how often notifications will be sent in response to a triggered alert.
 * The allowed options include NO_REPEAT and UNTIL_CLEARED.
 * <p/>
 * The NO_REPEAT option indicates that only an initial notification will be sent upon initial activation of the triggered alert.
 * <p/>
 * The UNTIL_CLEARED option indicates that notification will be repeated until the triggered alert is no longer active.
 * The Repeat Interval option will then be used to determine the interval between repeated notifications.
 * <p/>
 * The Notify On Recovery option, if enabled, causes an additional notification to be sent indicating that the alert
 * activation has cleared.
 */
@Path("/xn/rest/1.0/AlertingConfig")
public class AlertingConfigEndPoint extends ConfDataEndPoint<ConfDataAlertingConfig>
{
    @Inject
    public AlertingConfigEndPoint(final ConfDataDAO dao)
    {
        super(dao, ConfDataAlertingConfig.TYPE_NAME, ConfDataAlertingConfig.class);
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
    public Response create(final ConfDataAlertingConfig notifGroup)
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
