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

package com.ning.arecibo.event.receiver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import com.google.inject.Inject;
import com.ning.arecibo.event.transport.EventSerializer;
import com.ning.arecibo.eventlogger.Event;

public class EventParser implements MessageBodyReader<Event>
{
	private final Map<String, EventSerializer> serializers ;

	@Inject
	public EventParser(@EventSerializers List<EventSerializer> list)
	{		
		this.serializers = new HashMap<String, EventSerializer>();
		for (EventSerializer serializer : list) {
			serializers.put(serializer.getContentType(), serializer);
		}
	}

	@Override
    public boolean isReadable(Class<?> type,
                              Type genericType,
                              Annotation[] annotations,
                              MediaType mediaType)
    {
        if ( Event.class.isAssignableFrom(type) ) {
            EventSerializer  serializer = serializers.get(mediaType.toString());
            if ( serializer != null ) {
                return mediaType.toString().equals(serializer.getContentType())  ;
            }
        }
        return false ;
    }

    @Override
    public Event readFrom(Class<Event> type,
                          Type genericType,
                          Annotation[] annotations,
                          MediaType mediaType,
                          MultivaluedMap<String, String> httpHeaders,
                          InputStream entityStream) throws IOException,
                                                   WebApplicationException
    {
		EventSerializer  serializer = serializers.get(mediaType.toString());
		if ( serializer != null ) {
			return serializer.deserialize(entityStream);
		}
		throw new IOException("cannot locate parser for "+mediaType.toString());
	}
}
