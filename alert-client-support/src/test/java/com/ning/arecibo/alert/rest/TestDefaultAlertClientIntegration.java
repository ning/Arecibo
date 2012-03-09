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
        Assert.assertEquals(personFound.get("label"), nickName);
        Assert.assertEquals(personFound.get("is_group_alias"), "0");

        // Create some notifications mechanisms
        final int emailNotificationId = client.createEmailNotificationForPersonOrGroup(personId, UUID.randomUUID().toString());
        Assert.assertTrue(emailNotificationId > 0);
        final int smsNotificationId = client.createSmsNotificationForPersonOrGroup(personId, UUID.randomUUID().toString());
        Assert.assertTrue(smsNotificationId > 0);
        final Iterator<Map<String, Object>> iterator = client.findNotificationsForPersonOrGroupId(personId).iterator();
        final int notificationsFound = ImmutableList.copyOf(iterator).size();
        Assert.assertEquals(notificationsFound, 2);

        // Make sure we can delete one of the two
        client.deleteNotificationById(emailNotificationId);
        Assert.assertNull(client.findNotificationById(emailNotificationId));
        // The second one should still be around though
        Assert.assertNotNull(client.findNotificationById(smsNotificationId));

        // Clean ourselves up (notifications are automatically deleted due to the fk constraint)
        client.deletePersonOrGroupById(personId);
        Assert.assertNull(client.findPersonOrGroupById(personId));
    }
}
