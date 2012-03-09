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
import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.manage.AlertManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/xn/rest/1.0/ConfigEventSend")
public class ConfigEventSendEndPoint
{
    private final ConfigManager confStatusManager;
    private final AlertManager alertManager;

    @Inject
    public ConfigEventSendEndPoint(final ConfigManager confStatusManager, final AlertManager alertManager)
    {
        this.confStatusManager = confStatusManager;
        this.alertManager = alertManager;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sendEventsToAllConfigs()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("Sent events to alertConfigs: \n");

        final List<Long> configIdList = confStatusManager.getThresholdConfigIds();
        for (final Long configId : configIdList) {
            alertManager.handleThresholdEvent(configId, null);
            sb.append(String.format("\t%d\n", configId));
        }

        return sb.toString();
    }
}