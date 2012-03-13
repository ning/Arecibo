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
import com.ning.arecibo.alert.confdata.AlertingConfig;
import com.ning.arecibo.alert.confdata.ThresholdContextAttr;
import com.ning.arecibo.alert.confdata.ThresholdDefinition;
import com.ning.arecibo.alert.confdata.ThresholdQualifyingAttr;
import com.ning.arecibo.alertmanager.models.ThresholdDefinitionsModel;
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
@Path("/ui/thresholds")
public class ThresholdDefinitionsResource
{
    private static final Logger log = Logger.getLogger(ThresholdDefinitionsResource.class);

    private final AlertClient client;

    @Inject
    public ThresholdDefinitionsResource(final AlertClient client)
    {
        this.client = client;
    }

    @GET
    @TimedResource
    public Viewable getThresholds()
    {
        final Iterable<ThresholdDefinition> thresholdConfigs = client.findAllThresholdConfigs();
        final Map<String, Iterable<ThresholdQualifyingAttr>> thresholdQualifyingAttrsForThresholdConfig = new HashMap<String, Iterable<ThresholdQualifyingAttr>>();
        final Map<String, Iterable<ThresholdContextAttr>> thresholdContextAttrsForThresholdConfig = new HashMap<String, Iterable<ThresholdContextAttr>>();
        final Map<String, AlertingConfig> alertingConfigsForThresholdConfig = new HashMap<String, AlertingConfig>();

        for (final ThresholdDefinition thresholdConfig : thresholdConfigs) {
            final String thresholdConfigName = thresholdConfig.getThresholdDefinitionName();
            final Integer thresholdConfigId = thresholdConfig.getId();
            final Long alertingConfigId = thresholdConfig.getAlertingConfigurationId();

            if (thresholdConfigId != null) {
                // Retrieve associated Qualifying Attributes
                final Iterable<ThresholdQualifyingAttr> qualifyingAttrs = client.findThresholdQualifyingAttrsForThresholdId(thresholdConfigId);
                thresholdQualifyingAttrsForThresholdConfig.put(thresholdConfigName, qualifyingAttrs);

                // Retrieve associated Context Attributes
                final Iterable<ThresholdContextAttr> contextAttrs = client.findThresholdContextAttrsForThresholdId(thresholdConfigId);
                thresholdContextAttrsForThresholdConfig.put(thresholdConfigName, contextAttrs);
            }

            if (alertingConfigId != null) {
                // Retrieve associated Alerting Configuration
                final AlertingConfig alertingConfig = client.findAlertingConfigById(alertingConfigId);
                if (alertingConfig != null) {
                    alertingConfigsForThresholdConfig.put(alertingConfig.getAlertingConfigurationName(), alertingConfig);
                }
            }
        }

        // To create new Threshold definitions
        final Iterable<AlertingConfig> allAlertingConfigurations = client.findAllAlertingConfigurations();

        return new Viewable("/jsp/thresholds.jsp", new ThresholdDefinitionsModel(thresholdConfigs, thresholdQualifyingAttrsForThresholdConfig, thresholdContextAttrsForThresholdConfig, alertingConfigsForThresholdConfig, allAlertingConfigurations));
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @TimedResource
    public Response createThreshold(@FormParam("threshold_name") final String thresholdName,
                                    @FormParam("event_name") final String monitoredEventType,
                                    @FormParam("attribute_name") final String monitoredAttributeType,
                                    @FormParam("alerting_configuration") final List<Integer> alertingConfigurationIds,
                                    @FormParam("min_threshold") final String minThreshold,
                                    @FormParam("max_threshold") final String maxThreshold,
                                    @FormParam("min_samples") final String minSamples,
                                    @FormParam("max_samples") final String maxSamples,
                                    @FormParam("clearing_interval") final String clearingInterval,
                                    @FormParam("qualifying_attribute_type") final String qualifyingAttrType,
                                    @FormParam("qualifying_attribute_value") final String qualifyingAttrValue,
                                    @FormParam("context_attribute_type") final String contextAttrType)
    {
        final Double minThresholdValue = minThreshold.isEmpty() ? null : Double.valueOf(minThreshold);
        final Double maxThresholdValue = maxThreshold.isEmpty() ? null : Double.valueOf(maxThreshold);
        final Long minThresholdSamples = minSamples.isEmpty() ? null : Long.valueOf(minSamples);
        final Long maxSamplesWindowMs = maxSamples.isEmpty() ? null : Long.valueOf(maxSamples);
        final Long clearingIntervalMs = clearingInterval.isEmpty() ? null : Long.valueOf(clearingInterval);
        // TODO support multiple Alerting Configurations
        final Integer alertingConfigId = alertingConfigurationIds.size() == 0 ? 0 : alertingConfigurationIds.get(0);

        final int thresholdDefinitionId = client.createThresholdConfig(thresholdName, monitoredEventType, monitoredAttributeType,
            minThresholdValue, maxThresholdValue, minThresholdSamples, maxSamplesWindowMs, clearingIntervalMs, alertingConfigId);
        log.info("Created threshold definition %s (id=%d)", thresholdName, thresholdDefinitionId);

        if (qualifyingAttrType != null) {
            final int thresholdQualifyingAttrId = client.createThresholdQualifyingAttr(thresholdDefinitionId, qualifyingAttrType, qualifyingAttrValue);
            log.info("Created qualifying attribute %s -> %s (id=%d) for threshold definition %s (id=%d)",
                qualifyingAttrType, qualifyingAttrValue, thresholdQualifyingAttrId, thresholdName, thresholdDefinitionId);
        }
        if (contextAttrType != null) {
            final int thresholdContextAttrId = client.createThresholdContextAttr(thresholdDefinitionId, contextAttrType);
            log.info("Created context attribute %s (id=%d) for threshold definition %s (id=%d)",
                contextAttrType, thresholdContextAttrId, thresholdName, thresholdDefinitionId);
        }

        return Response.seeOther(URI.create("/ui/thresholds")).build();
    }
}
