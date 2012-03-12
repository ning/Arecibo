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

import com.google.inject.Singleton;
import com.ning.arecibo.alert.client.AlertClient;
import com.ning.arecibo.util.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import javax.ws.rs.Path;

@Singleton
@Path("/rest/1.0")
public class PeopleAndAliasesResource
{
    private static final Logger log = Logger.getLogger(PeopleAndAliasesResource.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AlertClient client;

    @Inject
    public PeopleAndAliasesResource(final AlertClient client)
    {
        this.client = client;
    }
}
