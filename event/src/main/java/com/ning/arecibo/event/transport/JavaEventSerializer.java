package com.ning.arecibo.event.transport;


import java.io.*;
import com.ning.arecibo.eventlogger.Event;

public class JavaEventSerializer implements EventSerializer
{
	public static final String CONTENT_TYPE = "application/x-java-serialized";

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
				return (Event) o.readObject();
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
