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
