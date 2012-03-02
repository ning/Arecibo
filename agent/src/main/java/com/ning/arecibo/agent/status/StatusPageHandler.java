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

package com.ning.arecibo.agent.status;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import com.google.inject.Inject;
import com.ning.arecibo.agent.AgentDataCollectorManager;

public class StatusPageHandler
{
	private static final DateTimeFormatter formatter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

	private final AgentDataCollectorManager dataCollector;

	@Inject
	public StatusPageHandler(AgentDataCollectorManager dataCollector)
	{
		this.dataCollector = dataCollector;
	}

	@Path("/")
	@GET
	@Produces("text/html")
	public StreamingOutput getSummary()
	{
        return new StreamingOutput()
        {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException
            {
                PrintWriter pw = new PrintWriter(output, true);
                
                List<Status> statusList = dataCollector.getStatus();   // config objects retained for info and status
                if (statusList != null && statusList.size() > 0) {
                    pw.println("<h4>[Click on column headers to sort]</h4>");
                    pw.println("<table class=\"sortable\">");
                    pw.println("<tr>");
                    pw.println("<th>Host</th>");
                    pw.println("<th>JMX/SNMP</th>");
                    pw.println("<th>Attribute</th>");
                    pw.println("<th>Status</th>");
                    pw.println("<th>Last update</th>");
                    pw.println("<th>Message</th>");
                    pw.println("<th>Last Collected Value</th>");
                    pw.println("</tr>");
                    for (Status status : statusList) {
                        long lastUpdateTime = status.getLastUpdateTime();
                        String lastUpdateString = (lastUpdateTime != 0) ? formatter.print(lastUpdateTime) : "--";

                        pw.println("<tr>");
                        pw.printf("<td>%s</td>\n", status.getHost());
                        pw.printf("<td>%s</td>\n", status.getEventName());
                        pw.printf("<td>%s</td>\n", status.getValueName());
                        pw.printf("<td>%s</td>\n", status.getLastStatus());
                        pw.printf("<td>%s</td>\n", lastUpdateString);
                        pw.printf("<td>%s</td>\n", status.getLastStatusMessage());
                        pw.printf("<td>%s</td>\n", status.getLastValue());
                        pw.println("</tr>");
                    }
                    pw.println("</table>");
                }
                else {
                    pw.println("[no status available.]");
                }
            }
        };
	}

    @Path("/overview")
    @GET
    @Produces("text/html")
    public StreamingOutput getOverview()
    {
        return new StreamingOutput()
        {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException
            {
                PrintWriter pw = new PrintWriter(output, true);

                writeHeader(pw, "Core Monitoring Agent", "Agent Summary");

                List<StatusSummary> statusSummary = dataCollector.getStatusSummary();
                if (statusSummary != null && statusSummary.size() > 0) {
                    writeAgentSummary(pw, statusSummary);
                    writeZoneSummaries(pw, statusSummary);
                }
                else {
                    pw.println("[no status available.]");
                }
                pw.flush();
                pw.close();
            }
        };
    }

	private void writeAgentSummary(PrintWriter pw, List<StatusSummary> statusSummary)
	{
		pw.println("<table>");
		pw.println("<tr>");
		pw.println("<th>Zone</th>");
		pw.println("<th>JMX/SNMP Queries</th>");
		pw.println("<th>Total Attributes</th>");
		pw.println("<th>Success Rate</th>");
		pw.println("<th>Last update</th>");
		pw.println("</tr>");
		int numQueries = 0;
		int numSuccesses = 0;
		int numAttributes = 0;
		for (StatusSummary summary : statusSummary) {
			numQueries += summary.getNumAccessors();
			numSuccesses += summary.getNumSuccesses();
			numAttributes += summary.getNumQueries();
		}
		long lastUpdateTime = statusSummary.get(0).getTimestamp();	// all same
		String lastUpdateString = (lastUpdateTime != 0) ? formatter.print(lastUpdateTime) : "--";
		pw.println("<tr>");
		pw.println("<td>--</td>");
		pw.printf("<td>%d</td>\n", numQueries);
		pw.printf("<td>%d</td>\n", numSuccesses);
		pw.printf("<td>%.1f%%</td>\n", 100.0 * numSuccesses / (double) numAttributes);
		pw.printf("<td>%s</td>\n", lastUpdateString);
		pw.println("</tr>");
		pw.println("</table>");
	}

	private void writeZoneSummaries(PrintWriter pw, List<StatusSummary> statusSummary)
	{
		pw.println("<h4>[Click on column headers to sort]</h4>");
		pw.println("<table class=\"sortable\">");
		pw.println(String.format("<tr>"));
		pw.println(String.format("<th>Zone</th>"));
		pw.println(String.format("<th>JMX/SNMP Queries</th>"));
		pw.println(String.format("<th>Total Attributes</th>"));
		pw.println(String.format("<th>Success Rate</th>"));
		pw.println(String.format("<th>Last update</th>"));
		pw.println(String.format("</tr>"));
		for (StatusSummary summary : statusSummary) {
			long lastUpdateTime = summary.getTimestamp();
			String lastUpdateString = (lastUpdateTime != 0) ? formatter.print(lastUpdateTime) : "--";

			pw.println(String.format("<tr>"));
			pw.println(String.format("<td>%s</td>", summary.getZone()));
			pw.println(String.format("<td>%d</td>", summary.getNumAccessors()));
			pw.println(String.format("<td>%d</td>", summary.getNumQueries()));
			pw.println(String.format("<td>%.1f%%</td>", summary.getPercentSuccessful()));
			pw.println(String.format("<td>%s</td>", lastUpdateString));
			pw.println(String.format("</tr>"));
		}
		pw.println("</table>");
	}


	private void writeHeader(PrintWriter out, String title, String subtitle)
	{
		out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
		out.println("<head>");
		out.println("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />");
		out.printf("<title>%s</title>\n", title);
		out.println("<meta name=\"keywords\" content=\"\" />");
		out.println("<meta name=\"description\" content=\"\" />");
		out.println("<script src=\"/static/sorttable.js\"></script>");
		out.println("<style rel=\"stylesheet\" type=\"text/css\" media=\"screen\" />");
		out.println("body { margin: 0; padding: 0; background: #496C10; font-family: Arial, Helvetica, sans-serif; font-size: 12px; color: #AFCE7C; }");
		out.println("h1, h2, h3 {font-family: Georgia, \"Times New Roman\", Times, serif;}");
		out.println("h1 {font-size: 3em;}");
		out.println("h2 {font-size: 2em;}");
		out.println("h3 {font-size: 1em;}");
		out.println("p, ul, ol {margin-bottom: 1.8em;line-height: 160%;}");
		out.println("a {color: #FFFFFF;}");
		out.println("#logo { width: 940px; height: 90px; margin: 4; background: #D2EBAB url(images/img02.jpg) no-repeat; color: #3E5C0E;}");
		out.println("#logo h1, #logo h2 { float: left; margin: 0;}");
		out.println("#logo h1 { padding: 25px 0 0 20px; letter-spacing: 3px; font-size: 3em;}");
		out.println("#logo h2 { padding: 36px 10 0 10px; letter-spacing: 1px; font-weight: normal; float: right}");
		out.println("#logo a { text-decoration: none; color: #3E5C0E;}");
		out.println("#subtitle h2 { height: 29px; margin: 0; padding: 9px 0 0 20px; background: #B6E074 url(images/img05.jpg) no-repeat; font-size: 1.4em; font-weight: normal; color: #000000;}");
		out.println("table {width: 100%; padding: 0px; border: 2px solid #FFFFFF;}");
		out.println("table td {vertical-align: middle; padding: 7px 25px; }");
		out.println("table td.num {vertical-align: middle; text-align: right; padding: 7px 25px; }");
		out.println("table th {height: 29px; padding: 9px 0 9 20px; background: #B6E074 url(images/img05.jpg) no-repeat; font-size: 1.4em; font-weight: normal; color: #000000;; }");
		out.println("</style>");
		out.println("</head>");
		out.println("<body>");
		out.println("    <div id=\"logo\">");
		out.printf("        <h1><a href=\"#\">%s</a></h1>\n", title);
		out.println("    </div>");
		out.printf("<H2> %s </H2>\n", subtitle);
	}

}
