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

import com.google.common.collect.Multimap;
import com.google.inject.Singleton;
import com.ning.arecibo.alert.client.AlertClient;
import com.ning.arecibo.alertmanager.models.NotificationGroupsModel;
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
import java.util.List;
import java.util.Map;

@Singleton
@Path("/ui/groups")
public class NotificationGroupsResource
{
    private static final Logger log = Logger.getLogger(NotificationGroupsResource.class);

    private final AlertClient client;

    @Inject
    public NotificationGroupsResource(final AlertClient client)
    {
        this.client = client;
    }

    @GET
    @TimedResource
    public Viewable getNotificationGroups()
    {
        final Iterable<Map<String, Object>> groups = client.findAllNotificationGroups();
        final Map<String, Multimap<String, String>> emailsAndNotificationTypesForGroup = new HashMap<String, Multimap<String, String>>();

        // Retrieve notifications for these groups to display email addresses
        for (final Map<String, Object> group : groups) {
            final String groupName = (String) group.get("label");
            final Integer groupId = (Integer) group.get("id");

            if (groupId != null) {
                final Multimap<String, String> mappings = client.findEmailsAndNotificationTypesForGroupById(groupId);
                emailsAndNotificationTypesForGroup.put(groupName, mappings);
            }
        }

        // To create new associations
        final Iterable<Map<String, Object>> allNotifications = client.findAllNotifications();

        return new Viewable("/jsp/groups.jsp", new NotificationGroupsModel(groups, emailsAndNotificationTypesForGroup, allNotifications));
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @TimedResource
    public Response createNotifGroup(@FormParam("is_group_enabled") final String enabled,
                                     @FormParam("group_name") final String groupName,
                                     @FormParam("person_or_alias") final List<Integer> notificationsIds)
    {
        final int notificationGroupId = client.createNotificationGroup(groupName, enabled != null, notificationsIds);
        log.info("Created Notif Group %s (id=%d)", groupName, notificationGroupId);

        return Response.seeOther(URI.create("/ui/groups")).build();
    }
}
