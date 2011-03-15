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
