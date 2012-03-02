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
import java.io.InputStream;
import java.io.OutputStream;
import com.ning.arecibo.eventlogger.Event;

public interface EventSerializer
{
    String HEADER_CONTENT_TYPE = "Content-type" ;

	void serialize(Event event, OutputStream stream) throws IOException;
	Event deserialize(InputStream in) throws IOException;
	String getContentType();
}
