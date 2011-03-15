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
import com.google.inject.Inject;
import com.ning.arecibo.aggregator.impl.AggregatorRegistry;
import com.ning.arecibo.aggregator.stringtemplates.StringTemplates;
import com.ning.arecibo.event.publisher.AreciboEventServiceChooser;
import com.ning.arecibo.util.service.ServiceDescriptor;

@Provider
@Produces(MediaType.TEXT_HTML)
public class AggregatorsRenderer implements MessageBodyWriter<AggregatorRegistry>
{
	private final AreciboEventServiceChooser chooser;

	@Inject
	public AggregatorsRenderer(AreciboEventServiceChooser chooser)
	{
		this.chooser = chooser;
	}

	@Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return AggregatorRegistry.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(AggregatorRegistry registry, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(AggregatorRegistry registry,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException
    {
		PrintWriter pw = new PrintWriter(entityStream);

		StringTemplate st = StringTemplates.getTemplate("htmlOpen");
		st.setAttribute("header", "Event Aggregator Endpoint");
		st.setAttribute("msg", "click on a link to view Aggregator details");
		pw.println(st.toString());


		pw.println("Other Aggregators :");

		for (ServiceDescriptor sd : chooser.getAllServiceDescriptors()) {
			String host = sd.getProperties().get("host");
			String port = sd.getProperties().get("jetty.port");
			pw.printf("[<a href=\"http://%s:%s/xn/rest/1.0/event/aggregator\"> %s:%s </a>]  ", host, port, host, port);
		}
		
		pw.println("<br><br>");

		pw.println(StringTemplates.getTemplate("aggJS"));
		pw.println(StringTemplates.getTemplate("formOpen"));

		pw.println(StringTemplates.getTemplate("tableOpen"));

		for (String name : registry.getAggregatorNames()) {
			pw.printf("<tr> <td> <a href=\"/xn/rest/1.0/event/aggregator/%s\"> %s </a> </td>\n", name, name);
			pw.printf("<td> %s </td> <tr>\n", StringTemplates.getTemplate("deleteButton",
					"label", "destroy",
					"name", name 							
			));
		}

		pw.println(StringTemplates.getTemplate("tableClose"));

		pw.println(StringTemplates.getTemplate("formClose"));

		pw.println(StringTemplates.getTemplate("htmlClose").toString());
	}
}
