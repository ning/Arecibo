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

package com.ning.arecibo.event.transport;

import java.io.IOException;
import com.ning.arecibo.eventlogger.Event;

public interface EventService
{
	public String HOST = "host" ;
	public String JETTY_PORT = "jetty.port" ;
	public String UDP_PORT = "udp.port" ;
    public String RMI_PORT = "rmi.port" ;
	public String API_PATH = "/xn/rest/1.0/event";

	public String HEADER_EVENT_TYPE = "x-event-type";
	public String HEADER_EVENT_KEY = "x-event-key";
	public String HEADER_SENDER_TYPE = "x-sender-type";

	public void sendUDP(Event event) throws IOException;
	public void sendREST(Event event) throws IOException;
}
