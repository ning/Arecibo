package com.ning.arecibo.event.transport;

import java.io.IOException;
import com.ning.arecibo.eventlogger.Event;

public interface EventService
{
	public String HOST = "host" ;
	public String JETTY_PORT = "jetty.port" ;
	public String UDP_PORT = "udp.port" ;
    public String RMI_PORT = "rmi.port" ;
	public String API_PATH = "/xn/rest/1.0/event";

	public String HEADER_EVENT_TYPE = "x-event-type";
	public String HEADER_EVENT_KEY = "x-event-key";
	public String HEADER_SENDER_TYPE = "x-sender-type";

	public void sendUDP(Event event) throws IOException;
	public void sendREST(Event event) throws IOException;
}
