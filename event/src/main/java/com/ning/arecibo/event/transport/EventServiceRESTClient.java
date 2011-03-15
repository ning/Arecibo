package com.ning.arecibo.event.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.ning.arecibo.eventlogger.Event;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

public class EventServiceRESTClient
{
	private final AsyncHttpClient client;
	private final EventSerializer serializer;
	private final String senderType;

	public EventServiceRESTClient(AsyncHttpClient client, EventSerializer serializer, String senderType)
	{
		this.client = client;
		this.serializer = serializer;
		this.senderType = senderType;
	}

	public boolean sendEvent(InetSocketAddress addr, Event event) throws IOException
	{
		return this.sendEvent(addr.getHostName(), addr.getPort(), event);
	}

	public boolean sendEvent(String host, int port, Event event) throws IOException
	{
	    String url = String.format("http://%s:%d%s", host, port, EventService.API_PATH);

	    BoundRequestBuilder builder = client.preparePost(url);

	    builder.addHeader(EventService.HEADER_EVENT_TYPE, event.getEventType());
		if (event.getSourceUUID() != null) {
		    builder.addHeader(EventService.HEADER_EVENT_KEY, event.getSourceUUID().toString());
		}
		builder.addHeader(EventService.HEADER_SENDER_TYPE, senderType);
		builder.addHeader(EventSerializer.HEADER_CONTENT_TYPE, serializer.getContentType());

		try {
    		Response response = builder.setBody(EventSerializerUDPUtil.toByteArray(serializer, event)).execute().get();
    
    		return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
		}
		catch (IOException ex) {
		    throw ex;
		}
		catch (Exception ex) {
		    throw new IOException("Could not send event", ex);
		}
	}

}
