package com.ning.arecibo.event.transport;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.ning.arecibo.eventlogger.Event;

public interface EventSerializer
{
    String HEADER_CONTENT_TYPE = "Content-type" ;

	void serialize(Event event, OutputStream stream) throws IOException;
	Event deserialize(InputStream in) throws IOException;
	String getContentType();
}
