package com.ning.arecibo.event.transport;


import org.json.JSONException;
import org.json.JSONObject;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;

import java.io.*;

// TODO: switch to one of the better JSON libs available
public class JsonEventSerializer implements EventSerializer
{
	public static final String CONTENT_TYPE = "application/json";

	public void serialize(Event event, OutputStream stream) throws IOException
	{
		PrintWriter pw = new PrintWriter(stream);
		JSONObject json = null;
		if ( event instanceof MapEvent ) {
			json = new JSONObject(((MapEvent)event).toMap());
		}
		else {
			json = new JSONObject(event);
		}
		pw.println(json.toString());
		pw.flush();
	}

	// TODO : this won't work for streaming multiple events
	public Event deserialize(InputStream in) throws IOException
	{
		InputStreamReader reader = new InputStreamReader(in);
		BufferedReader br = new BufferedReader(reader);

        StringBuilder sb = new StringBuilder();
		String line;
        while((line = br.readLine()) != null) {
            sb.append(line);
        }
        
		try {
			JSONObject json = new JSONObject(sb.toString());
			return MapEvent.fromJSON(json);
		}
		catch (JSONException e) {
			throw new IOException(e);
		}
	}

	public String getContentType()
	{
		return CONTENT_TYPE;
	}
}
