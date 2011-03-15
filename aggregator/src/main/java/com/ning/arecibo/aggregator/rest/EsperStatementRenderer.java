package com.ning.arecibo.aggregator.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.json.JSONObject;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.ning.arecibo.aggregator.rest.EventStreamEndPoint.EPStatementQuery;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.util.Logger;

@Provider
@Produces(MediaType.TEXT_PLAIN)
public class EsperStatementRenderer implements MessageBodyWriter<EventStreamEndPoint.EPStatementQuery>
{
	private static final Logger log = Logger.getLogger(EsperStatementRenderer.class);
    private static final long MAX_QUIET_TIME = 60000L;
    private static final long MAX_POLL_TIME = 5000L;

	@Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return EventStreamEndPoint.EPStatementQuery.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(EPStatementQuery query, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(EPStatementQuery query,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException
    {
		UpdateListener listener = null;
		try {
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(entityStream), true);
			final LinkedBlockingQueue<EventBean> queue = new LinkedBlockingQueue<EventBean>();
			log.info("registering update listener with %s", query.getStmt().getName());
			final EventStreamEndPoint.EPStatementQuery query1 = query;
			listener = new UpdateListener(){
				public void update(EventBean[] newEvents, EventBean[] oldEvents)
				{
                    if(newEvents == null) {
                        // add logging here to understand what's happening
                        log.warn("got newEvents == null");
                    }
                    else if(query1 == null) {
                        // add logging here to understand what's happening
                        log.warn("got query1 == null");
                    }
                    else {
					    log.info("receieved %d events from %s", newEvents.length, query1.getStmt().getName());
					    for ( EventBean bean : newEvents ) {
						    queue.add(bean);
                        }
					}
				}
			};
			query.getStmt().addListener(listener);
            log.info("polling event beans for %s", query.getStmt().getName());

            long lastOutputTime = System.currentTimeMillis();
			while (!pw.checkError()) {
				EventBean bean = queue.poll(MAX_POLL_TIME, TimeUnit.MILLISECONDS) ;
				pw.flush();

                long now = System.currentTimeMillis();
				if (bean != null ) {
					log.info("got event beans for %s", query.getStmt().getName());
                    if ( query.getWhere() != null )
                    {
                        int match = 0 ;
                        for ( Map.Entry<String, List<String>> entry : query.getWhere().entrySet() ) {
                            for ( String v : entry.getValue() ) {
                                if ( v.equals(bean.get(entry.getKey()))) {
                                    match ++ ;
                                    break;
                                }
                            }
                        }
                        if ( match >= query.getWhere().size() ) {
                            printBean(pw, bean);
                            entityStream.flush();
                            lastOutputTime = now;
                        }
                        else if(now - lastOutputTime > MAX_QUIET_TIME) {
                            // this should force a failure when browser disconnects
                            printEllipsis(pw);
                            entityStream.flush();
                            lastOutputTime = now;
                        }
                    }
                    else {
                        printBean(pw, bean);
                        entityStream.flush();
                        lastOutputTime = now;
                    }
				}
                else {
                    if(now - lastOutputTime > MAX_QUIET_TIME) {
                        // this should force a failure when browser disconnects
                        printEllipsis(pw);
                        entityStream.flush();
                        lastOutputTime = now;
                    }
                }
			}
		}
		catch (Exception e) {
			log.warn(e);
		}
		finally {
			log.info("Commiting response and cleaning up update listener for %s", query.getStmt().getName());
            if ( query != null && listener != null ) {
				query.getStmt().removeListener(listener);
			}
		}
	}

    private void printBean(PrintWriter pw, EventBean bean)
    {
	    pw.println(new JSONObject(MapEvent.fromEventBean(bean, bean.getEventType().getName()).toMap()));
	    pw.flush();
    }

    private void printEllipsis(PrintWriter pw) {
        pw.println("...");
        pw.flush();
    }
}
