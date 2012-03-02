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
import java.util.List;
import com.ning.arecibo.aggregator.dictionary.EventDefinition;
import com.ning.arecibo.aggregator.eventservice.EventServiceManager;
import com.ning.arecibo.event.BatchedEvent;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.lang.ExternalPublisher;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.UUIDUtil;

public class ExternalPublisherImpl extends AggregationOutputProcessorImpl
{
	private static final Logger log = Logger.getLogger(ExternalPublisherImpl.class);

	private final EventService service;
	private final AggregatorImpl impl;
	private final EventServiceManager manager;

	public ExternalPublisherImpl(AggregatorImpl impl, ExternalPublisher externalPublisher, EventServiceManager manager)
	{
		super();
		this.impl = impl;
		this.manager = manager;
		this.service = manager.createEventService(externalPublisher.getServiceName());
	}

    public void update(EventDefinition def, List<MapEvent> newEvents)
	{
		if ( newEvents != null ) {
			BatchedEvent batch = null ;
			for (MapEvent event : newEvents) {

                // make local copy, so can modify if needed
                MapEvent copyEvt = new MapEvent(event.toMap());

				if (copyEvt.getSourceUUID() == null) {
					copyEvt.setUuid(UUIDUtil.md5UUID(copyEvt.getEventType()));
				}

				if ( batch == null ) {
					batch = new BatchedEvent(copyEvt);
				}
				else {
					batch.getEvents().add(copyEvt);
				}
			}

			if ( batch != null ) {
				try {
					service.sendREST(batch);
                    manager.updateExternalEventsDelivered(newEvents.size());
				}
				catch (IOException e) {
					log.error(e);
                    manager.updateExternalEventsFailed(newEvents.size());
				}
			}
		}
    }
}
