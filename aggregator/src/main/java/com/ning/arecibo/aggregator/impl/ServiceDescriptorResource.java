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

package com.ning.arecibo.aggregator.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.UUID;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import org.antlr.stringtemplate.StringTemplate;
import com.google.inject.Inject;
import com.ning.arecibo.aggregator.guice.SelfUUID;
import com.ning.arecibo.aggregator.stringtemplates.StringTemplates;
import com.ning.arecibo.event.publisher.AreciboEventServiceChooser;
import com.ning.arecibo.util.service.ServiceDescriptor;

@Path("/")
public class ServiceDescriptorResource
{
    private final AreciboEventServiceChooser chooser;
    private final UUID selfUUID;

    @Inject
    public ServiceDescriptorResource(AreciboEventServiceChooser chooser, @SelfUUID UUID selfUUID)
    {
        this.chooser = chooser;
        this.selfUUID = selfUUID;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public StreamingOutput getServiceDescriptors()
    {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException
            {
                final PrintWriter pw = new PrintWriter(output, true);

                StringTemplate st = StringTemplates.getTemplate("htmlOpen");
                st.setAttribute("header", "Event Aggregator v2" );
                st.setAttribute("msg", "" );
                pw.println(st);

                pw.println("<h3> Aggregator Nodes </h3>");

                for (ServiceDescriptor sd : chooser.getAllServiceDescriptors()) {
                    String host = sd.getProperties().get("host");
                    String port = sd.getProperties().get("jetty.port");
                    pw.printf("<li> [<a href=\"http://%s:%s/\"> %s:%s </a>]  ", host, port, host, port);
                    pw.printf(" [<a href=\"http://%s:%s/xn/rest/1.0/event/aggregator\"> Aggregators </a>]", host, port);
                    pw.printf(" [<a href=\"http://%s:%s/xn/rest/1.0/event/dictionary\"> Event Dictionary </a>]", host, port);
                    pw.printf(" [<a href=\"http://%s:%s/xn/rest/1.0/event/stream\"> Streaming End Point </a>]", host, port);
                    pw.printf("%s \n", selfUUID.equals(sd.getUuid()) ? " &lt;== hey that's this Node" : "");
                }

                pw.println(StringTemplates.getTemplate("htmlClose"));

                pw.flush();
                pw.close();
            }
        };
    }
}
