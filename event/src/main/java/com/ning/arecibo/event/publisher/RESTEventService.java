package com.ning.arecibo.event.publisher;

import com.ning.arecibo.util.service.ServiceDescriptor;

import java.io.IOException;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.event.transport.EventServiceRESTClient;
import com.ning.arecibo.eventlogger.Event;

public class RESTEventService implements EventService
{
	private final EventServiceChooser parent;
	private final ServiceDescriptor sd;
	private final EventServiceRESTClient restClient ;
	private final String host;
	private final int port;

	public RESTEventService(EventServiceChooser parent, ServiceDescriptor sd, EventServiceRESTClient restClient)
	{
		this.parent = parent;
		this.sd = sd;
		this.restClient = restClient;
		this.host = sd.getProperties().get(HOST);
		this.port = Integer.parseInt(sd.getProperties().get(JETTY_PORT));
	}

    @Override
	public void sendUDP(Event event) throws IOException
	{
		throw new UnsupportedOperationException();
	}

    @Override
	public void sendREST(Event event) throws IOException
	{
		try {
			if (!restClient.sendEvent(host, port, event) ) {
				parent.invalidate(event.getSourceUUID());
			}
		}
		catch (IOException e) {
			parent.invalidate(event.getSourceUUID());
			throw e ;
		}
	}

	public ServiceDescriptor getServiceDescriptor()
	{
		return sd;
	}
}
