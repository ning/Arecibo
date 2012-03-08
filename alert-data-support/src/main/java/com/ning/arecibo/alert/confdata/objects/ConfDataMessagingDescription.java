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

package com.ning.arecibo.alert.confdata.objects;

import java.util.Map;

public class ConfDataMessagingDescription extends ConfDataObject
{
    public static final String TYPE_NAME = "messaging_description";

    private static final String MESSAGE_TYPE_FIELD = "message_type";
    private static final String MESSAGE_TEXT_FIELD = "message_text";

    protected volatile String messageType = null;
    protected volatile String messageText = null;

    public ConfDataMessagingDescription()
    {
    }

    @Override
    public String getTypeName()
    {
        return TYPE_NAME;
    }

    @Override
    public void populatePropertiesFromMap(final Map<String, Object> map)
    {
        super.populatePropertiesFromMap(map);
        setMessageType(getString(map, MESSAGE_TYPE_FIELD));
        setMessageText(getString(map, MESSAGE_TEXT_FIELD));
    }

    @Override
    public Map<String, Object> toPropertiesMap()
    {
        final Map<String, Object> map = super.toPropertiesMap();
        setString(map, MESSAGE_TYPE_FIELD, getMessageType());
        setString(map, MESSAGE_TEXT_FIELD, getMessageText());

        return map;
    }

    @Override
    public void toStringBuilder(final StringBuilder sb)
    {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", MESSAGE_TYPE_FIELD, getMessageType()));
        sb.append(String.format("   %s -> %s\n", MESSAGE_TEXT_FIELD, getMessageText()));
    }

    public String getMessageType()
    {
        return this.messageType;
    }

    public void setMessageType(final String messagingType)
    {
        this.messageType = messageType;
    }

    public String getMessageText()
    {
        return this.messageText;
    }

    public void setMessageText(final String messageText)
    {
        this.messageText = messageText;
    }
}
