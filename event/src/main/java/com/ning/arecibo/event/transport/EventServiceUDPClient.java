package com.ning.arecibo.event.transport;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.ning.arecibo.event.publisher.EventSenderType;
import com.ning.arecibo.eventlogger.Event;

public class EventServiceUDPClient
{
	private final EventSerializer serializer;
	private final String senderType;
	// TODO : sweep the cache for stale sockets
	private final ConcurrentHashMap<SocketAddress, DatagramSocket> cache = new ConcurrentHashMap<SocketAddress, DatagramSocket>();

	public EventServiceUDPClient(EventSerializer serializer, @EventSenderType String senderType) throws IOException
	{
		this.serializer = serializer;
		this.senderType = senderType;
	}

	public void sendEvent(SocketAddress udpAddress, Event event) throws IOException
	{
		DatagramSocket socket = cache.get(udpAddress);
		if (socket == null) {
			socket = new DatagramSocket();
			socket.connect(udpAddress);
			DatagramSocket old = cache.put(udpAddress, socket);
			if (old != null) {
				old.close();
			}
		}
		Map<String, String> headers = new HashMap<String, String>() ;
		headers.put(EventService.HEADER_EVENT_TYPE, event.getEventType());
        if (event.getSourceUUID() != null) {
            headers.put(EventService.HEADER_EVENT_KEY, event.getSourceUUID().toString());
        }
        headers.put(EventService.HEADER_SENDER_TYPE, senderType);
		headers.put(EventSerializer.HEADER_CONTENT_TYPE, serializer.getContentType());

		byte b[] = EventSerializerUDPUtil.toUDPPacket(headers, event, serializer);
		socket.send(new DatagramPacket(b, b.length));
	}
}
