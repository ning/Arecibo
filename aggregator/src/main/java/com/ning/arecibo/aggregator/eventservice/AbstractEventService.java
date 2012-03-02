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

package com.ning.arecibo.aggregator.eventservice;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.event.transport.EventServiceRESTClient;
import com.ning.arecibo.event.transport.EventServiceUDPClient;
import com.ning.arecibo.eventlogger.Event;

import com.ning.arecibo.util.Logger;

public abstract class AbstractEventService implements EventService
{
	private static final Logger log = Logger.getLogger(AbstractEventService.class);

	private final EventServiceRESTClient restClient;
	private final EventServiceUDPClient udpClient;
	private final EventServiceManager manager;

	protected AbstractEventService(EventServiceRESTClient restClient, EventServiceUDPClient udpClient, EventServiceManager manager)
	{
		this.restClient = restClient;
		this.udpClient = udpClient;
		this.manager = manager;
	}

	abstract protected InetSocketAddress getRESTSocketAddress() throws IOException;
	abstract protected InetSocketAddress getUDPSocketAddress() throws IOException;

	public void sendUDP(final Event event) throws IOException
	{
		manager.getAsyncSender().execute(new Runnable(){
			public void run()
			{
				try {
					udpClient.sendEvent(getUDPSocketAddress(), event);
				}
				catch (IOException e) {
					log.error(e);
				}
			}
		});
	}

	public void sendREST(Event event) throws IOException
	{
		restClient.sendEvent(getRESTSocketAddress(), event);
	}

}
