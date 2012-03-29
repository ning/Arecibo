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

package com.ning.arecibo.alert.client;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AlertStatusJSONConverter
{
    public static final String CURRENT_ALERTS = "current_alerts";
    public static final String ALERT_ID = "alertId";
    public static final String ALERT_TYPE = "alertType";
    public static final String EVENT_TYPE = "eventType";
    public static final String ATTRIBUTE_TYPE = "attributeType";
    public static final String ACTIVATION_STATUS = "activationStatus";

    public static String serializeStatusListToJSON(final List<AlertStatus> alertStatii) throws IOException
    {
        final JsonFactory jsonFactory = new JsonFactory();
        final StringWriter sw = new StringWriter();

        final JsonGenerator out = jsonFactory.createJsonGenerator(sw);
        out.setPrettyPrinter(new DefaultPrettyPrinter());

        out.writeStartObject();

        out.writeFieldName(CURRENT_ALERTS);
        out.writeStartArray();
        for (final AlertStatus alertStatus : alertStatii) {
            out.writeStartObject();

            out.writeFieldName(ALERT_ID);
            out.writeString(alertStatus.getAlertId());

            out.writeFieldName(ALERT_TYPE);
            out.writeString(alertStatus.getAlertType().toString());

            out.writeFieldName(EVENT_TYPE);
            out.writeString(alertStatus.getEventType());

            out.writeFieldName(ATTRIBUTE_TYPE);
            out.writeString(alertStatus.getAttributeType());

            out.writeFieldName(ACTIVATION_STATUS);
            out.writeString(alertStatus.getActivationStatus().toString());

            final Map<String, String> auxAttMap = alertStatus.getAuxAttributeMap();
            if (auxAttMap != null) {

                final Set<String> auxAtts = auxAttMap.keySet();
                if (auxAtts.size() > 0) {

                    for (final String auxAtt : auxAtts) {
                        out.writeFieldName(auxAtt);
                        out.writeString(auxAttMap.get(auxAtt));
                    }
                }
            }

            out.writeEndObject();
        }
        out.writeEndArray();

        out.writeEndObject();

        out.flush();

        return sw.toString();
    }

    public static List<AlertStatus> serializeJSONToStatusList(final InputStream JSONStream)
            throws IOException
    {
        final JsonParser parser = new MappingJsonFactory().createJsonParser(JSONStream);
        parser.enable(JsonParser.Feature.ALLOW_COMMENTS);
        parser.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        parser.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

        final JsonNode rootNode = parser.readValueAsTree();
        final JsonNode currentAlerts = rootNode.path(CURRENT_ALERTS);
        if (currentAlerts.size() == 0) {
            return null;
        }

        final Iterator<JsonNode> alertNodes = currentAlerts.iterator();
        final ArrayList<AlertStatus> retList = new ArrayList<AlertStatus>();

        while (alertNodes.hasNext()) {
            final JsonNode alertNode = alertNodes.next();

            final Map<String, String> attributes = new HashMap<String, String>();

            final Iterator<String> fieldNames = alertNode.fieldNames();
            while (fieldNames.hasNext()) {
                final String fieldName = fieldNames.next();

                final JsonNode valueNode = alertNode.get(fieldName);
                final String value = valueNode.textValue();

                attributes.put(fieldName, value);
            }

            final String alertId = attributes.remove(ALERT_ID);
            final String alertTypeString = attributes.remove(ALERT_TYPE);
            final String eventType = attributes.remove(EVENT_TYPE);
            final String attributeType = attributes.remove(ATTRIBUTE_TYPE);
            final String activationStatusString = attributes.remove(ACTIVATION_STATUS);

            AlertType alertType = null;
            if (alertTypeString != null) {
                alertType = AlertType.valueOf(alertTypeString);
            }

            AlertActivationStatus activationStatus = null;
            if (activationStatusString != null) {
                activationStatus = AlertActivationStatus.valueOf(activationStatusString);
            }

            final AlertStatus alertStatus = new AlertStatus(alertId, alertType, activationStatus, eventType, attributeType);
            alertStatus.setAuxAttributeMap(attributes);
            retList.add(alertStatus);
        }

        return retList;
    }
}
