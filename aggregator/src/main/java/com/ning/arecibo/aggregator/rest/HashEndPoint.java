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

package com.ning.arecibo.aggregator.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.google.inject.Inject;
import com.ning.arecibo.event.publisher.AreciboEventServiceChooser;
import com.ning.arecibo.event.publisher.EventServiceChooser;

@Path("/xn/rest/1.0/event/hash/{name}")
public class HashEndPoint
{
    private final AreciboEventServiceChooser chooser;

    @Inject
    public HashEndPoint(EventServiceChooser chooser)
    {
        this.chooser = (AreciboEventServiceChooser)chooser;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@PathParam("name") String name)
    {
        return chooser.getHost(name);
    }
}
