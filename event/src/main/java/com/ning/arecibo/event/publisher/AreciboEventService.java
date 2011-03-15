package com.ning.arecibo.event.publisher;

import com.ning.arecibo.util.service.ServiceDescriptor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.event.transport.EventServiceRESTClient;
import com.ning.arecibo.event.transport.EventServiceUDPClient;
import com.ning.arecibo.eventlogger.Event;

public class AreciboEventService implements EventService
{
	private final EventServiceChooser parent;
	private final ServiceDescriptor sd;
	private final EventServiceRESTClient restClient ;
	private final String host;
	private final int port;
	private final int udpPort ;
	private final EventServiceUDPClient udpClient;
	private final SocketAddress udpAddress ;

	public AreciboEventService(EventServiceChooser parent, ServiceDescriptor sd, EventServiceRESTClient restClient, EventServiceUDPClient udpClient)
	{
		this.parent = parent;
		this.sd = sd;
		this.restClient = restClient;
		this.host = sd.getProperties().get(HOST);
		this.port = Integer.parseInt(sd.getProperties().get(JETTY_PORT));
		this.udpPort = Integer.parseInt(sd.getProperties().get(UDP_PORT));
		this.udpClient = udpClient ;
		this.udpAddress = new InetSocketAddress(host, udpPort);
	}

    @Override
	public void sendUDP(Event event) throws IOException
	{
		try {
			udpClient.sendEvent(udpAddress, event);
		}
		catch (IOException e) {
			parent.invalidate(event.getSourceUUID());
			throw e ;
		}
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
