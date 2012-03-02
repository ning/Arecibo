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

import java.io.StringWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.impl.DefaultPrettyPrinter;

public class AlertStatusJSONConverter {

    public final static String CURRENT_ALERTS = "current_alerts";
    public final static String ALERT_ID = "alertId";
    public final static String ALERT_TYPE = "alertType";
    public final static String EVENT_TYPE = "eventType";
    public final static String ATTRIBUTE_TYPE = "attributeType";
    public final static String ACTIVATION_STATUS = "activationStatus";

    public static String serializeStatusListToJSON(List<AlertStatus> alertStatii) throws IOException {

        JsonFactory jsonFactory = new JsonFactory();
        StringWriter sw = new StringWriter();

        JsonGenerator out = jsonFactory.createJsonGenerator(sw);
        out.setPrettyPrinter(new DefaultPrettyPrinter());

        out.writeStartObject();

        out.writeFieldName(CURRENT_ALERTS);
        out.writeStartArray();
        for (AlertStatus alertStatus : alertStatii) {
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

            Map<String, String> auxAttMap = alertStatus.getAuxAttributeMap();
            if (auxAttMap != null) {

                Set<String> auxAtts = auxAttMap.keySet();
                if (auxAtts.size() > 0) {

                    for (String auxAtt : auxAtts) {
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

    public static List<AlertStatus> serializeJSONToStatusList(InputStream JSONStream)
            throws IOException {

        JsonParser parser = new MappingJsonFactory().createJsonParser(JSONStream);
        parser.enable(JsonParser.Feature.ALLOW_COMMENTS);
        parser.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        parser.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

        JsonNode rootNode = parser.readValueAsTree();
        JsonNode currentAlerts = rootNode.path(CURRENT_ALERTS);
        if (currentAlerts.size() == 0) {
            return null;
        }

        Iterator<JsonNode> alertNodes = currentAlerts.getElements();
        ArrayList<AlertStatus> retList = new ArrayList<AlertStatus>();

        while (alertNodes.hasNext()) {
            JsonNode alertNode = alertNodes.next();

            Map<String,String> attributes = new HashMap<String,String>();

            Iterator<String> fieldNames = alertNode.getFieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();

                JsonNode valueNode = alertNode.get(fieldName);
                String value = valueNode.getTextValue();

                attributes.put(fieldName, value);
            }

            String alertId = attributes.remove(ALERT_ID);
            String alertTypeString = attributes.remove(ALERT_TYPE);
            String eventType = attributes.remove(EVENT_TYPE);
            String attributeType = attributes.remove(ATTRIBUTE_TYPE);
            String activationStatusString = attributes.remove(ACTIVATION_STATUS);

            AlertType alertType = null;
            if(alertTypeString != null)
                alertType = AlertType.valueOf(alertTypeString);

            AlertActivationStatus activationStatus = null;
            if(activationStatusString != null)
                activationStatus = AlertActivationStatus.valueOf(activationStatusString);

            AlertStatus alertStatus = new AlertStatus(alertId,alertType,activationStatus,eventType,attributeType);
            alertStatus.setAuxAttributeMap(attributes);
            retList.add(alertStatus);
        }

        return retList;
    }
}
