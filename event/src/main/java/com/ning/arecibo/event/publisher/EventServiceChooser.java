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

package com.ning.arecibo.event.publisher;

import java.io.IOException;
import java.util.UUID;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.util.service.ServiceListener;

public interface EventServiceChooser extends ServiceListener
{
	public void start();
	public void stop();
	public EventService choose(UUID uuid) throws IOException;
	public void invalidate(UUID uuid);	
}
