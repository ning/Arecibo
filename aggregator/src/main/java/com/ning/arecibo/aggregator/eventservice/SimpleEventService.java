package com.ning.arecibo.aggregator.eventservice;

import java.net.InetSocketAddress;
import com.ning.arecibo.event.transport.EventServiceRESTClient;
import com.ning.arecibo.event.transport.EventServiceUDPClient;

public class SimpleEventService extends AbstractEventService
{
	private final InetSocketAddress restAddr ;
	private final InetSocketAddress udpAddr ;

	public SimpleEventService(String host, int restPort, int udpPort, EventServiceRESTClient restClient, EventServiceUDPClient udpClient, EventServiceManager manager)
	{
		super(restClient, udpClient, manager);
		this.udpAddr = new InetSocketAddress(host, udpPort);
		this.restAddr = new InetSocketAddress(host, restPort);
	}

	protected InetSocketAddress getRESTSocketAddress()
	{
		return restAddr;
	}

	protected InetSocketAddress getUDPSocketAddress()
	{
		return udpAddr;
	}
}
