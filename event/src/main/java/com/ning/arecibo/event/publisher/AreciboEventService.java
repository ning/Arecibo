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
