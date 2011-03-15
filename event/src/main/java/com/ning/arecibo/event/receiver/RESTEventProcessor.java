package com.ning.arecibo.event.receiver;

import javax.ws.rs.core.Response;
import com.ning.arecibo.eventlogger.Event;

public interface RESTEventProcessor extends BaseEventProcessor
{
	public Response processEvent(Event event);
}
