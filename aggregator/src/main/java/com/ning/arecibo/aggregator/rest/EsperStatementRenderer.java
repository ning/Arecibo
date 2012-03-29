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

package com.ning.arecibo.aggregator.rest;

import com.ning.arecibo.aggregator.rest.EventStreamEndPoint.EPStatementQuery;
import com.ning.arecibo.util.Logger;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Provider
@Produces(MediaType.TEXT_PLAIN)
public class EsperStatementRenderer implements MessageBodyWriter<EventStreamEndPoint.EPStatementQuery>
{
    private static final Logger log = Logger.getLogger(EsperStatementRenderer.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final long MAX_QUIET_TIME = 60000L;
    private static final long MAX_POLL_TIME = 5000L;

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType)
    {
        return EventStreamEndPoint.EPStatementQuery.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(final EPStatementQuery query, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(final EPStatementQuery query,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException, WebApplicationException
    {
        UpdateListener listener = null;
        try {
            final PrintWriter pw = new PrintWriter(new OutputStreamWriter(entityStream), true);
            final LinkedBlockingQueue<EventBean> queue = new LinkedBlockingQueue<EventBean>();
            log.info("registering update listener with %s", query.getStmt().getName());
            listener = new UpdateListener()
            {
                public void update(final EventBean[] newEvents, final EventBean[] oldEvents)
                {
                    if (newEvents == null) {
                        // add logging here to understand what's happening
                        log.warn("got newEvents == null");
                    }
                    else if (query == null) {
                        // add logging here to understand what's happening
                        log.warn("got query1 == null");
                    }
                    else {
                        log.info("receieved %d events from %s", newEvents.length, query.getStmt().getName());
                        Collections.addAll(queue, newEvents);
                    }
                }
            };
            query.getStmt().addListener(listener);
            log.info("polling event beans for %s", query.getStmt().getName());

            long lastOutputTime = System.currentTimeMillis();
            while (!pw.checkError()) {
                final EventBean bean = queue.poll(MAX_POLL_TIME, TimeUnit.MILLISECONDS);
                pw.flush();

                final long now = System.currentTimeMillis();
                if (bean != null) {
                    log.info("got event beans for %s", query.getStmt().getName());
                    if (query.getWhere() != null) {
                        int match = 0;
                        for (final Map.Entry<String, List<String>> entry : query.getWhere().entrySet()) {
                            for (final String v : entry.getValue()) {
                                if (v.equals(bean.get(entry.getKey()))) {
                                    match++;
                                    break;
                                }
                            }
                        }
                        if (match >= query.getWhere().size()) {
                            printBean(pw, bean);
                            entityStream.flush();
                            lastOutputTime = now;
                        }
                        else if (now - lastOutputTime > MAX_QUIET_TIME) {
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
                    if (now - lastOutputTime > MAX_QUIET_TIME) {
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
            if (query != null && listener != null) {
                query.getStmt().removeListener(listener);
            }
        }
    }

    private void printBean(final PrintWriter pw, final EventBean bean)
    {
        try {
            pw.println(mapper.writeValueAsString(bean));
            pw.flush();
        }
        catch (IOException ignored) {
        }
    }

    private void printEllipsis(final PrintWriter pw)
    {
        pw.println("...");
        pw.flush();
    }
}
