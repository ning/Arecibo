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
