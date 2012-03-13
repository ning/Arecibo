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

package com.ning.arecibo.alert.client.rest;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.ning.arecibo.alert.client.AlertClient;
import com.ning.arecibo.alert.client.discovery.AlertFinder;
import com.ning.arecibo.alert.confdata.AlertingConfig;
import com.ning.arecibo.alert.confdata.NotifConfig;
import com.ning.arecibo.alert.confdata.NotifGroup;
import com.ning.arecibo.alert.confdata.Person;
import com.ning.arecibo.alert.confdata.ThresholdContextAttr;
import com.ning.arecibo.alert.confdata.ThresholdDefinition;
import com.ning.arecibo.alert.confdata.ThresholdQualifyingAttr;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.spice.jersey.client.ahc.config.DefaultAhcConfig;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultAlertClient implements AlertClient
{
    private static final Logger log = LoggerFactory.getLogger(DefaultAlertClient.class);
    private static final String USER_AGENT = "NING-AlertClient/1.0";
    private static final String RESOURCE_PATH = "xn/rest/1.0";
    private static final Splitter PATH_SPLITTER = Splitter.on("/");

    private static final String PERSON_PATH = "Person";
    private static final String NOTIF_CONFIG_PATH = "NotifConfig";
    private static final String NOTIF_GROUP_PATH = "NotifGroup";
    private static final String NOTIF_MAPPING_PATH = "NotifMapping";
    private static final String ALERTING_CONFIG_PATH = "AlertingConfig";
    private static final String NOTIF_GROUP_MAPPING_PATH = "NotifGroupMapping";
    private static final String THRESHOLD_CONFIG_PATH = "ThresholdConfig";
    private static final String THRESHOLD_QUALIFYING_ATTR_PATH = "ThresholdQualifyingAttr";
    private static final String THRESHOLD_CONTEXT_ATTR_PATH = "ThresholdContextAttr";

    private final AlertFinder alertFinder;

    private Client client;

    @Inject
    public DefaultAlertClient(final AlertFinder alertFinder)
    {
        this.alertFinder = alertFinder;
        createClient();
    }

    @Override
    public int createPerson(final String firstName, final String lastName, final String nickName) throws UniformInterfaceException
    {
        final Map<String, String> person = ImmutableMap.of(
            "firstName", firstName,
            "lastName", lastName,
            "label", createLabel(nickName),
            "isGroupAlias", "false");

        final URI location = doPost(PERSON_PATH, person);
        return extractIdFromURI(location);
    }

    @Override
    public int createGroup(final String name)
    {
        final Map<String, ?> group = ImmutableMap.of(
            "label", createLabel(name),
            "isGroupAlias", Boolean.TRUE);

        final URI location = doPost(PERSON_PATH, group);
        return extractIdFromURI(location);
    }

    @Override
    public Iterable<Person> findAllPeopleAndGroups() throws UniformInterfaceException
    {
        return fetchObject(PERSON_PATH, new GenericType<List<Person>>()
        {
        });
    }

    @Override
    public Person findPersonOrGroupById(final int id) throws UniformInterfaceException
    {
        return fetchObject(PERSON_PATH + "/" + id, new GenericType<Person>()
        {
        });
    }

    @Override
    public void deletePersonOrGroupById(final int id) throws UniformInterfaceException
    {
        doDelete(PERSON_PATH + "/" + id);
    }

    @Override
    public int createEmailNotificationForPersonOrGroup(final int id, final String address)
    {
        return createNotificationForPersonOrGroup(id, address, "REGULAR_EMAIL");
    }

    @Override
    public int createSmsNotificationForPersonOrGroup(final int id, final String address)
    {
        return createNotificationForPersonOrGroup(id, address, "SMS_VIA_EMAIL");
    }

    @Override
    public Iterable<NotifConfig> findAllNotifications() throws UniformInterfaceException
    {
        return fetchObject(NOTIF_CONFIG_PATH, new GenericType<List<NotifConfig>>()
        {
        });
    }

    @Override
    public NotifConfig findNotificationById(final int id) throws UniformInterfaceException
    {
        return fetchObject(NOTIF_CONFIG_PATH + "/" + id, new GenericType<NotifConfig>()
        {
        });
    }

    @Override
    public Iterable<NotifConfig> findNotificationsForPersonOrGroupId(final int id) throws UniformInterfaceException
    {
        return fetchObject(NOTIF_CONFIG_PATH + "/Person/" + id, new GenericType<List<NotifConfig>>()
        {
        });
    }

    @Override
    public void deleteNotificationById(final int id)
    {
        doDelete(NOTIF_CONFIG_PATH + "/" + id);
    }

    @Override
    public int createNotificationGroup(final String groupName, final boolean enabled, final Iterable<Integer> notificationsIds)
    {
        // First, create the Notification Group
        final Map<String, ?> group = ImmutableMap.of(
            "label", createLabel(groupName),
            "enabled", enabled);
        final URI location = doPost(NOTIF_GROUP_PATH, group);
        final int notifGroupId = extractIdFromURI(location);

        // Now, create a Notification Mapping for each Notification Config
        // TODO should we consider a bulk resource?
        for (final int notifConfigId : notificationsIds) {
            final Map<String, ?> mapping = ImmutableMap.of(
                "label", createLabel(String.format("%d_to_%d", notifGroupId, notifConfigId)),
                "notifGroupId", notifGroupId,
                "notifConfigId", notifConfigId);
            doPost(NOTIF_MAPPING_PATH, mapping);
        }

        return notifGroupId;
    }

    @Override
    public List<NotifGroup> findAllNotificationGroups() throws UniformInterfaceException
    {
        return fetchObject(NOTIF_GROUP_PATH, new GenericType<List<NotifGroup>>()
        {
        });
    }

    @Override
    public List<NotifConfig> findEmailsAndNotificationTypesForGroupById(final int id) throws UniformInterfaceException
    {
        return fetchObject(NOTIF_MAPPING_PATH + "/NotifGroup/" + id, new GenericType<List<NotifConfig>>()
        {
        });
    }

    @Override
    public int createAlertingConfig(final String name, final boolean repeatUntilCleared, final boolean notifyOnRecovery,
                                    final boolean enabled, final Iterable<Integer> notificationGroupsIds) throws UniformInterfaceException
    {
        // First, create the Alerting Config
        final Map<String, ?> alertingConfig = ImmutableMap.of(
            "label", createLabel(name),
            "notifRepeatMode", repeatUntilCleared ? "UNTIL_CLEARED" : "NO_REPEAT",
            "notifOnRecovery", notifyOnRecovery,
            "enabled", enabled);
        final URI location = doPost(ALERTING_CONFIG_PATH, alertingConfig);
        final int alertingConfigId = extractIdFromURI(location);

        // Now, create a Notification Group Mapping for each Notification Group
        // TODO should we consider a bulk resource?
        for (final int notifGroupId : notificationGroupsIds) {
            final Map<String, ?> mapping = ImmutableMap.of(
                "label", createLabel(String.format("%d_to_%d", alertingConfigId, notifGroupId)),
                "alertingConfigId", alertingConfigId,
                "notifGroupId", notifGroupId);
            doPost(NOTIF_GROUP_MAPPING_PATH, mapping);
        }

        return alertingConfigId;
    }

    @Override
    public Iterable<AlertingConfig> findAllAlertingConfigurations() throws UniformInterfaceException
    {
        return fetchObject(ALERTING_CONFIG_PATH, new GenericType<List<AlertingConfig>>()
        {
        });
    }

    @Override
    public AlertingConfig findAlertingConfigById(final Long id) throws UniformInterfaceException
    {
        return fetchObject(ALERTING_CONFIG_PATH + "/" + id, new GenericType<AlertingConfig>()
        {
        });
    }

    @Override
    public Iterable<NotifGroup> findNotificationGroupsForAlertingConfigById(final int id) throws UniformInterfaceException
    {
        return fetchObject(NOTIF_GROUP_MAPPING_PATH + "/AlertingConfig/" + id, new GenericType<List<NotifGroup>>()
        {
        });
    }

    @Override
    public int createThresholdConfig(final String name, final String monitoredEventType, final String monitoredAttributeType,
                                     @Nullable final Double minThresholdValue, @Nullable final Double maxThresholdValue,
                                     final Long minThresholdSamples, final Long maxSampleWindowMs,
                                     final Long clearingIntervalMs, final int alertingConfigId) throws UniformInterfaceException
    {
        final ImmutableMap.Builder<String, Object> thresholdConfigBuilder = new ImmutableMap.Builder<String, Object>()
            .put("label", createLabel(name))
            .put("monitoredEventType", monitoredEventType)
            .put("monitoredAttributeType", monitoredAttributeType)
            .put("minThresholdSamples", minThresholdSamples)
            .put("maxSampleWindowMs", maxSampleWindowMs)
            .put("clearingIntervalMs", clearingIntervalMs)
            .put("alertingConfigId", alertingConfigId);

        if (minThresholdValue != null) {
            thresholdConfigBuilder.put("minThresholdValue", minThresholdValue);
        }
        if (maxThresholdValue != null) {
            thresholdConfigBuilder.put("maxThresholdValue", maxThresholdValue);
        }

        final Map<String, ?> thresholdConfig = thresholdConfigBuilder.build();
        final URI location = doPost(THRESHOLD_CONFIG_PATH, thresholdConfig);
        return extractIdFromURI(location);
    }

    @Override
    public int createThresholdQualifyingAttr(final int thresholdConfigId, final String attributeType, final String attributeValue)
        throws UniformInterfaceException
    {
        final Map<String, ?> thresholdQualifyingAttr = ImmutableMap.of(
            "label", createLabel(String.format("%d: %s -> %s", thresholdConfigId, attributeType, attributeValue)),
            "thresholdConfigId", thresholdConfigId,
            "attributeType", attributeType,
            "attributeValue", attributeValue);
        final URI location = doPost(THRESHOLD_QUALIFYING_ATTR_PATH, thresholdQualifyingAttr);
        return extractIdFromURI(location);
    }

    @Override
    public int createThresholdContextAttr(final int thresholdConfigId, final String attributeType) throws UniformInterfaceException
    {
        final Map<String, ?> thresholdContextAttr = ImmutableMap.of(
            "label", createLabel(String.format("%d: %s", thresholdConfigId, attributeType)),
            "thresholdConfigId", thresholdConfigId,
            "attributeType", attributeType);
        final URI location = doPost(THRESHOLD_CONTEXT_ATTR_PATH, thresholdContextAttr);
        return extractIdFromURI(location);
    }

    @Override
    public Iterable<ThresholdDefinition> findAllThresholdConfigs() throws UniformInterfaceException
    {
        return fetchObject(THRESHOLD_CONFIG_PATH, new GenericType<List<ThresholdDefinition>>()
        {
        });
    }

    @Override
    public Iterable<ThresholdQualifyingAttr> findThresholdQualifyingAttrsForThresholdId(final int thresholdConfigId) throws UniformInterfaceException
    {
        return fetchObject(THRESHOLD_QUALIFYING_ATTR_PATH + "/ThresholdConfig/" + thresholdConfigId, new GenericType<List<ThresholdQualifyingAttr>>()
        {
        });
    }

    @Override
    public Iterable<ThresholdContextAttr> findThresholdContextAttrsForThresholdId(final int thresholdConfigId) throws UniformInterfaceException
    {
        return fetchObject(THRESHOLD_CONTEXT_ATTR_PATH + "/ThresholdConfig/" + thresholdConfigId, new GenericType<List<ThresholdContextAttr>>()
        {
        });
    }

    // PRIVATE

    private int createNotificationForPersonOrGroup(final int id, final String address, final String notificationType)
    {
        //TODO for now just use a truncated version of the email address, need to devise something better
        final Map<String, ?> group = ImmutableMap.of(
            "personId", id,
            "address", address,
            "notifType", notificationType,
            "label", createLabel(address));

        final URI location = doPost(NOTIF_CONFIG_PATH, group);
        return extractIdFromURI(location);
    }

    private String createLabel(final String address)
    {
        String label = address;
        if (label.length() > 32) {
            label = label.substring(0, 31);
        }
        return label;
    }

    private <T> T fetchObject(final String path, final GenericType<T> genericType)
    {
        final WebResource resource = createWebResource().path(path);

        log.info("Calling: GET {}", resource.toString());
        final ClientResponse response = resource.type(MediaType.APPLICATION_JSON).get(ClientResponse.class);

        if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            return null;
        }
        else if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            final String message = extractErrorMessageFromResponse(response);
            throw new UniformInterfaceException(message, response);
        }
        else {
            return response.getEntity(genericType);
        }
    }

    private URI doPost(final String path, final Map<String, ?> payload)
    {
        final WebResource resource = createWebResource().path(path);

        log.info("Calling: POST {}", resource.toString());
        final ClientResponse response = resource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, payload);

        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            final String message = extractErrorMessageFromResponse(response);
            throw new UniformInterfaceException(message, response);
        }
        else {
            return response.getLocation();
        }
    }

    private void doDelete(final String path)
    {
        final WebResource resource = createWebResource().path(path);

        log.info("Calling: DELETE {}", resource.toString());
        final ClientResponse response = resource.type(MediaType.APPLICATION_JSON).delete(ClientResponse.class);

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            final String message = extractErrorMessageFromResponse(response);
            throw new UniformInterfaceException(message, response);
        }
    }

    private int extractIdFromURI(final URI location)
    {
        final Iterable<String> explodedPath = PATH_SPLITTER.split(location.getPath());
        final Iterator<String> iterator = explodedPath.iterator();
        if (!iterator.hasNext()) {
            return -1;
        }
        else {
            String lastSplit = null;
            while (iterator.hasNext()) {
                lastSplit = iterator.next();
            }
            return Integer.valueOf(lastSplit);
        }
    }

    private String extractErrorMessageFromResponse(final ClientResponse response)
    {
        String message = response.toString();

        // Extract Warning header, if any
        final MultivaluedMap<String, String> headers = response.getHeaders();
        if (headers != null) {
            final List<String> warnings = headers.get("Warning");
            if (warnings != null && warnings.size() > 0) {
                message = message + " - Warning " + warnings.get(0);
            }
        }
        return message;
    }

    private void createClient()
    {
        final DefaultAhcConfig config = new DefaultAhcConfig();
        config.getClasses().add(JacksonJsonProvider.class);
        client = Client.create(config);
    }

    private WebResource createWebResource()
    {
        String collectorUri = alertFinder.getAlertUri();
        if (!collectorUri.endsWith("/")) {
            collectorUri += "/";
        }
        collectorUri += RESOURCE_PATH;

        final WebResource resource = client.resource(collectorUri);
        resource
            .accept(MediaType.APPLICATION_JSON)
            .header("User-Agent", USER_AGENT);

        return resource;
    }
}
