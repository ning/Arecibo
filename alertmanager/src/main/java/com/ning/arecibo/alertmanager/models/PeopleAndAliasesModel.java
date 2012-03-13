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

package com.ning.arecibo.alertmanager.models;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PeopleAndAliasesModel
{
    private final List<PersonOrGroup> peopleAndGroups = new ArrayList<PersonOrGroup>();

    public static final class PersonOrGroup
    {
        private final String nickName;
        private final String firstName;
        private final String lastName;
        private final Multimap<String, String> emails;

        public PersonOrGroup(final String nickName, final String firstName, final String lastName, final Multimap<String, String> emails)
        {
            this.nickName = nickName;
            this.firstName = firstName;
            this.lastName = lastName;
            this.emails = emails;
        }

        public Multimap<String, String> getEmails()
        {
            return emails;
        }

        public String getFirstName()
        {
            return firstName;
        }

        public String getLastName()
        {
            return lastName;
        }

        public String getNickName()
        {
            return nickName;
        }
    }

    public PeopleAndAliasesModel(final Iterable<Map<String, Object>> peopleAndAliases, final Map<String, List<Map<String, Object>>> notificationsForPersonOrGroup)
    {
        for (final Map<String, Object> person : peopleAndAliases) {
            final String nickName = (String) person.get("label");
            final Multimap<String, String> emailsAndNotifications = HashMultimap.create();

            final List<Map<String, Object>> notifications = notificationsForPersonOrGroup.get(nickName);
            for (final Map<String, Object> notification : notifications) {
                emailsAndNotifications.put((String) notification.get("address"), (String) notification.get("notif_type"));
            }

            final PersonOrGroup peopleAndGroup = new PersonOrGroup(
                nickName,
                (String) person.get("first_name"),
                (String) person.get("last_name"),
                emailsAndNotifications
            );
            peopleAndGroups.add(peopleAndGroup);
        }
    }

    public List<PersonOrGroup> getPeopleAndGroups()
    {
        return peopleAndGroups;
    }
}
