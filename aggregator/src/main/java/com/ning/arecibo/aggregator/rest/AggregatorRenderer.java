package com.ning.arecibo.aggregator.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.antlr.stringtemplate.StringTemplate;
import com.ning.arecibo.aggregator.impl.AggregatorImpl;
import com.ning.arecibo.aggregator.stringtemplates.StringTemplates;

@Provider
@Produces("text/html+agg")
public class AggregatorRenderer implements MessageBodyWriter<AggregatorImpl>
{
	@Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return AggregatorImpl.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(AggregatorImpl impl, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(AggregatorImpl impl,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException
    {
		PrintWriter pw = new PrintWriter(entityStream);

		StringTemplate st = StringTemplates.getTemplate("htmlOpen");
		st.setAttribute("header", "Aggregator Details");
		st.setAttribute("msg", "");
		pw.println(st.toString());
						
		pw.println(StringTemplates.getTemplate("tableOpen"));

		StringTemplate th = StringTemplates.getTemplate("tableHeader");
		th.setAttribute("headers", Arrays.asList("Aggregator", "InputEvent", "OutputEvent", "Esper Statement"));
		pw.println(th);

		impl.renderHtml(pw, 0);
		pw.println(StringTemplates.getTemplate("tableClose"));

		pw.println(StringTemplates.getTemplate("htmlClose"));
	}
}
