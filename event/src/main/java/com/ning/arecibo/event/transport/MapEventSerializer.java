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


import java.io.*;
import java.util.Map;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;

public class MapEventSerializer implements EventSerializer
{
    public static final String CONTENT_TYPE = "application/x-java-map";

    public void serialize(Event event, OutputStream stream) throws IOException
    {
        ObjectOutputStream o = null;
        try {
            o = new ObjectOutputStream(stream);
            o.writeObject(event);
            o.flush();
        }
        finally {
            if (o != null) {
                o.close();
            }
        }
    }

    public Event deserialize(InputStream in) throws IOException
    {
        ObjectInputStream o = null;
        try {
            o = new ObjectInputStream(in);
            try {
                Map<String, Object> map = (Map<String, Object>) o.readObject();
                return new MapEvent(map);
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
        finally {
            if (o != null) {
                o.close();
            }
        }
    }

    public String getContentType()
    {
        return CONTENT_TYPE;
    }
}
