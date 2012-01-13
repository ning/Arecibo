package com.ning.arecibo.event.receiver;

import java.util.concurrent.atomic.AtomicLong;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import com.ning.arecibo.util.Logger;
import com.google.inject.Inject;
import com.ning.arecibo.eventlogger.Event;

@Path("/")
public class RESTEventEndPoint
{
	private static final Logger log = Logger.getLogger(RESTEventEndPoint.class) ;
	private final AtomicLong count = new AtomicLong(0);
	private final BaseEventProcessor processor;

	@Inject
	public RESTEventEndPoint(BaseEventProcessor processor)
	{
		this.processor = processor;
	}

	@POST
	@Path("/1.0/event")
	public Response post(Event e)
	{
		// TODO : handle forwarding by reading just the header and proxy the payload over without parsing
		if ( log.isDebugEnabled() ) {
            count.incrementAndGet();
			log.debug("Event received : %s %s %s %s count = %d", e.getSourceUUID(), e.getEventType(), e.getTimestamp(), e, count.get());
		}

        if(processor instanceof EventProcessor) {
            ((EventProcessor)processor).processEvent(e);
            return Response.ok().build();
        }
        else if(processor instanceof RESTEventProcessor) {
		    return ((RESTEventProcessor)processor).processEvent(e);
        }
        else {
            throw new IllegalStateException("Unsupported EventProcessor class: " + processor.getClass().getName());
        }
	}
}
