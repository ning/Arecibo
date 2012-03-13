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

package com.ning.arecibo.alertmanager.resources;

import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import com.ning.arecibo.alert.client.AlertClient;
import com.ning.arecibo.alert.confdata.AlertingConfig;
import com.ning.arecibo.alert.confdata.NotifGroup;
import com.ning.arecibo.alertmanager.models.AlertingConfigurationsModel;
import com.ning.arecibo.util.Logger;
import com.ning.jersey.metrics.TimedResource;
import com.sun.jersey.api.view.Viewable;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Singleton
@Path("/ui/alerting")
public class AlertingConfigurationsResource
{
    private static final Logger log = Logger.getLogger(AlertingConfigurationsResource.class);

    private final AlertClient client;

    @Inject
    public AlertingConfigurationsResource(final AlertClient client)
    {
        this.client = client;
    }

    @GET
    @TimedResource
    public Viewable getAlertingConfigurations()
    {
        final Iterable<AlertingConfig> alertingConfigurations = client.findAllAlertingConfigurations();
        final Map<String, Iterable<NotifGroup>> notificationsGroupsForAlertingConfig = new HashMap<String, Iterable<NotifGroup>>();

        // Retrieve notifications groups for these alerting configurations
        for (final AlertingConfig alertingConfig : alertingConfigurations) {
            final String configName = alertingConfig.getAlertingConfigurationName();
            final Integer alertingConfigId = alertingConfig.getId();

            if (alertingConfigId != null) {
                final Iterator<NotifGroup> iterator = client.findNotificationGroupsForAlertingConfigById(alertingConfigId).iterator();
                notificationsGroupsForAlertingConfig.put(configName, ImmutableList.copyOf(iterator));
            }
        }

        final Iterable<NotifGroup> notificationGroups = client.findAllNotificationGroups();

        return new Viewable("/jsp/alerting.jsp", new AlertingConfigurationsModel(alertingConfigurations, notificationsGroupsForAlertingConfig, notificationGroups));
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @TimedResource
    public Response createAlertingConfiguration(@FormParam("config_name") final String alertingConfigName,
                                                @FormParam("is_config_enabled") final String enabled,
                                                @FormParam("notify_on_recovery") final String notifyOnRecovery,
                                                @FormParam("repeat_mode") final String repeatMode,
                                                @FormParam("notification_groups") final List<Integer> notificationGroups)
    {
        boolean repeatUntilCleared = false;
        if (repeatMode != null && repeatMode.equals("UNTIL_CLEARED")) {
            repeatUntilCleared = true;
        }

        final int alertingConfigId = client.createAlertingConfig(alertingConfigName, repeatUntilCleared, notifyOnRecovery != null, enabled != null, notificationGroups);
        log.info("Created alerting config %s (id=%d)", alertingConfigName, alertingConfigId);

        return Response.seeOther(URI.create("/ui/alerting")).build();
    }
}
