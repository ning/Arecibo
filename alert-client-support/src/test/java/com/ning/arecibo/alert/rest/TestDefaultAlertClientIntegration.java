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

package com.ning.arecibo.alert.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.ning.arecibo.alert.client.AlertClient;
import com.ning.arecibo.alert.client.AlertClientConfig;
import com.ning.arecibo.alert.client.discovery.AlertFinder;
import com.ning.arecibo.alert.client.discovery.DefaultAlertFinder;
import com.ning.arecibo.alert.client.rest.DefaultAlertClient;
import org.skife.config.ConfigurationObjectFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TestDefaultAlertClientIntegration
{
    AlertClient client;

    @BeforeMethod
    public void setUp() throws Exception
    {
        final AlertClientConfig config = new ConfigurationObjectFactory(System.getProperties()).build(AlertClientConfig.class);
        final AlertFinder finder = new DefaultAlertFinder(config);
        client = new DefaultAlertClient(finder);
    }

    @Test(groups = "integration,slow", enabled = true)
    public void testPersonEndPoint() throws Exception
    {
        final String firstName = "Pierre";
        final String lastName = "Meyer";
        final String nickName = UUID.randomUUID().toString();

        // Create a contact
        final int personId = client.createPerson(firstName, lastName, nickName);
        Assert.assertTrue(personId > 0);

        // Make sure we can find it
        final Map<String, Object> personFound = client.findPersonOrGroupById(personId);
        Assert.assertEquals(personFound.get("first_name"), firstName);
        Assert.assertEquals(personFound.get("last_name"), lastName);
        Assert.assertEquals(personFound.get("is_group_alias"), "0");

        // Create some notifications mechanisms
        final String address = UUID.randomUUID().toString();
        final int emailNotificationId = client.createEmailNotificationForPersonOrGroup(personId, address);
        Assert.assertTrue(emailNotificationId > 0);
        final String smsAddress = UUID.randomUUID().toString();
        final int smsNotificationId = client.createSmsNotificationForPersonOrGroup(personId, smsAddress);
        Assert.assertTrue(smsNotificationId > 0);

        // Make sure we can find the notifications by user
        final Iterator<Map<String, Object>> iterator = client.findNotificationsForPersonOrGroupId(personId).iterator();
        final int notificationsFound = ImmutableList.copyOf(iterator).size();
        Assert.assertEquals(notificationsFound, 2);

        // Create a group for these notifications
        final int groupId = client.createNotificationGroup("page-pierre-" + UUID.randomUUID().toString(), true,
            ImmutableList.<Integer>of(emailNotificationId, smsNotificationId));
        Assert.assertTrue(groupId > 0);

        // Make sure we can find it
        final Multimap<String, String> mappings = client.findEmailsAndNotificationTypesForGroupById(groupId);
        Assert.assertEquals(mappings.keys().size(), 2);
        Assert.assertEquals(mappings.get(address).size(), 1);
        Assert.assertEquals(mappings.get(address).iterator().next(), "REGULAR_EMAIL");
        Assert.assertEquals(mappings.get(smsAddress).size(), 1);
        Assert.assertEquals(mappings.get(smsAddress).iterator().next(), "SMS_VIA_EMAIL");

        // Create an Alerting Configuration for this group
        final int alertingConfigurationId = client.createAlertingConfig("critical-alerts-" + UUID.randomUUID().toString(),
            true, true, true, ImmutableList.<Integer>of(groupId));
        Assert.assertTrue(alertingConfigurationId > 0);

        // Create a Threshold Definition
        final int thresholdDefinitionId = client.createThresholdConfig("jetty-connections-" + UUID.randomUUID().toString(),
            "JettyServer", "connectionsOpen", null, 2000.0, 6L, 120000L, 300000L, alertingConfigurationId);
        Assert.assertTrue(thresholdDefinitionId > 0);

        // Add a couple of Qualifying Attributes for this Threshold
        int thresholdQualifyingAttrId = client.createThresholdQualifyingAttr(thresholdDefinitionId, "deployedType", "collector");
        Assert.assertTrue(thresholdQualifyingAttrId > 0);
        thresholdQualifyingAttrId = client.createThresholdQualifyingAttr(thresholdDefinitionId, "deployedVersion", "1.2.3");
        Assert.assertTrue(thresholdQualifyingAttrId > 0);

        // Add a couple of Context Attributes for this Threshold
        int thresholdContextAttrId = client.createThresholdContextAttr(thresholdDefinitionId, "deployedType");
        Assert.assertTrue(thresholdContextAttrId > 0);
        thresholdContextAttrId = client.createThresholdContextAttr(thresholdDefinitionId, "deployedVersion");
        Assert.assertTrue(thresholdContextAttrId > 0);

        // Make sure we can delete one of the two notifications
        client.deleteNotificationById(emailNotificationId);
        Assert.assertNull(client.findNotificationById(emailNotificationId));
        // The second one should still be around though
        Assert.assertNotNull(client.findNotificationById(smsNotificationId));

        // Clean ourselves up (notifications are automatically deleted due to the fk constraint)
        client.deletePersonOrGroupById(personId);
        Assert.assertNull(client.findPersonOrGroupById(personId));
    }
}