package com.ning.arecibo.event.receiver;

import com.google.inject.Inject;
import com.ning.arecibo.event.transport.EventSerializer;
import com.ning.arecibo.event.transport.EventSerializerUDPUtil;
import com.ning.arecibo.event.transport.JavaEventSerializer;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

@Path("rest")
public class RESTEventEndPoint
{
    private static final Logger log = Logger.getLogger(RESTEventEndPoint.class);
    private static final EventSerializer serializer = new JavaEventSerializer();

    private final AtomicLong count = new AtomicLong(0);
    private final BaseEventProcessor processor;

    @Inject
    public RESTEventEndPoint(BaseEventProcessor processor)
    {
        this.processor = processor;
    }

    @POST
    @Path("/1.0/event")
    public Response post(byte[] eventBytes)
    {
        try {
            Event e = EventSerializerUDPUtil.fromByteArray(serializer, eventBytes);
            return processEvent(e);
        }
        catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    private Response processEvent(Event e)
    {
        // TODO : handle forwarding by reading just the header and proxy the payload over without parsing
        if (log.isDebugEnabled()) {
            count.incrementAndGet();
            log.debug("Event received : %s %s %s %s count = %d", e.getSourceUUID(), e.getEventType(), e.getTimestamp(), e, count.get());
        }

        if (processor instanceof EventProcessor) {
            ((EventProcessor) processor).processEvent(e);
            return Response.ok().build();
        }
        else if (processor instanceof RESTEventProcessor) {
            return ((RESTEventProcessor) processor).processEvent(e);
        }
        else {
            throw new IllegalStateException("Unsupported EventProcessor class: " + processor.getClass().getName());
        }
    }
}
