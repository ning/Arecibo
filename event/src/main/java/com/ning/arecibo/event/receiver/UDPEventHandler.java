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

package com.ning.arecibo.event.receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.inject.Inject;
import com.ning.arecibo.event.transport.EventSerializer;
import com.ning.arecibo.event.transport.EventSerializerUDPUtil;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.esper.MiniEsperEngine;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;

public class UDPEventHandler
{
	private static final Logger log = Logger.getLogger(UDPEventHandler.class) ;
	private final EventProcessor processor;
	private final MiniEsperEngine<Integer> esper ;
    private final Map<String, EventSerializer> serializers;

    @Inject
	public UDPEventHandler(@EventSerializers List<EventSerializer> list, BaseEventProcessor processor)
	{
		this.processor = (EventProcessor)processor;
		this.esper = new MiniEsperEngine<Integer>(this.getClass().getSimpleName(), Integer.class);

        this.serializers = new HashMap<String, EventSerializer>();
		for (EventSerializer serializer : list) {
			serializers.put(serializer.getContentType(), serializer);
		}
	}

	public void receive(DatagramPacket p)
	{
		try {
			byte b[] = p.getData() ;
			try {
				esper.send(b.length);
			}
			catch(Exception e) {
			}
			if (b.length > 2) {
                Event e = EventSerializerUDPUtil.fromUDPPacket(b, serializers);
				processor.processEvent(e);
			}
			else {
				log.warn("corrupted UDP packet received, length < 2");
			}
		}
		catch (IOException e) {
			log.error(e);
		}
		catch (RuntimeException e) {
			log.error(e);
		}
	}

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
	public long getDatagramsReceived()
	{
		return esper.getCount() ;
	}

	@MonitorableManaged(monitored = true)
	public long getAverageDatagramSize()
	{
		return (long) esper.getAverage() ;
	}

	@MonitorableManaged(monitored = true)
	public long getMaxDatagramSize()
	{
		return esper.getMax().longValue();
	}

	@MonitorableManaged(monitored = true)
	public long getMinDatagramSize()
	{
		return esper.getMin().longValue();
	}

}
