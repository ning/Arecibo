package com.ning.arecibo.event.receiver;

import com.google.inject.Inject;
import com.ning.arecibo.event.BatchedEvent;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.MonitoringEvent;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.concurrent.atomic.AtomicLong;

@Path("/")
public class RESTEventEndPoint
{
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = Logger.getLogger(RESTEventEndPoint.class);
    private final AtomicLong count = new AtomicLong(0);
    private final BaseEventProcessor processor;

    @Inject
    public RESTEventEndPoint(BaseEventProcessor processor)
    {
        this.processor = processor;
    }

    // TODO: support xml and binary endpoints

    @POST
    @Path("/1.0/event")
    public Response post(JsonNode node)
    {
        final Event e;
        if (node.isArray()) {
            e = mapper.convertValue(node, BatchedEvent.class);
        }
        else if (node.has(MonitoringEvent.KEY_CONFIG_PATH)) {
            e = mapper.convertValue(node, MonitoringEvent.class);
        }
        else {
            e = mapper.convertValue(node, MapEvent.class);
        }

        // TODO : handle forwarding by reading just the header and proxy the payload over without parsing
        if (log.isDebugEnabled()) {
            count.incrementAndGet();
            log.debug("Event received : %s %s %s %s count = %d", e.getSourceUUID(), e.getEventType(), e.getTimestamp(), e, count.get());
        }

        if (processor instanceof EventProcessor) {
            ((EventProcessor)processor).processEvent(e);
            return Response.ok().build();
        }
        else if (processor instanceof RESTEventProcessor) {
            return ((RESTEventProcessor)processor).processEvent(e);
        }
        else {
            throw new IllegalStateException("Unsupported EventProcessor class: " + processor.getClass().getName());
        }
    }
}
