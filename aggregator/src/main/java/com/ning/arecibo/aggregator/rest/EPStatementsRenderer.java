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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.antlr.stringtemplate.StringTemplate;
import com.ning.arecibo.aggregator.stringtemplates.StringTemplates;

@Provider
@Produces("text/plain+epstmts")
public class EPStatementsRenderer implements MessageBodyWriter<Set<String>>
{
	@Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return Set.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(Set<String> o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(Set<String> o,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException
    {
		Set<String> list = (Set<String>) o ;
		PrintWriter pw = new PrintWriter(entityStream);

		StringTemplate st = StringTemplates.getTemplate("htmlOpen");
		st.setAttribute("header", "Event Streaming Endpoint");
		st.setAttribute("msg", "click on a link to receive streaming events");
		pw.println(st.toString());

		for ( String name : list ) {
			pw.printf("<li> <a href=\"/xn/rest/1.0/event/stream/%s\"> %s </a>\n", name, name);
		}

		pw.println(StringTemplates.getTemplate("htmlClose").toString());
	}
}
