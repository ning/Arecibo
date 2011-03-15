package com.ning.arecibo.aggregator.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.antlr.stringtemplate.StringTemplate;
import com.ning.arecibo.aggregator.dictionary.EventDictionary;
import com.ning.arecibo.aggregator.stringtemplates.StringTemplates;

@Provider
@Produces(MediaType.TEXT_HTML)
public class EventDictionaryRenderer implements MessageBodyWriter<EventDictionary>
{
	@Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return EventDictionary.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(EventDictionary t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(EventDictionary dict,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException
    {
		PrintWriter pw = new PrintWriter(entityStream);

		StringTemplate st = StringTemplates.getTemplate("htmlOpen");
		st.setAttribute("header", "Event Dictionary Endpoint");
		st.setAttribute("msg", "click on a link to view event metadata");
		pw.println(st.toString());

		for (String name : dict.getEventNames()) {
			pw.printf("<li> <a href=\"/xn/rest/1.0/event/dictionary/%s\"> %s </a>\n", name, name);
		}

		pw.println(StringTemplates.getTemplate("htmlClose").toString());
	}
}
