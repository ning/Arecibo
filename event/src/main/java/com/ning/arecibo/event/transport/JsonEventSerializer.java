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

package com.ning.arecibo.event.transport;

import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class JsonEventSerializer implements EventSerializer
{
    public static final String CONTENT_TYPE = "application/json";
    public static final ObjectMapper mapper = new ObjectMapper();

    public void serialize(Event event, OutputStream stream) throws IOException
    {
        mapper.writeValue(stream, event);
    }

    // TODO : this won't work for streaming multiple events
    public Event deserialize(InputStream in) throws IOException
    {
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(reader);

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }

        return mapper.convertValue(sb.toString(), MapEvent.class);
    }

    public String getContentType()
    {
        return CONTENT_TYPE;
    }
}
