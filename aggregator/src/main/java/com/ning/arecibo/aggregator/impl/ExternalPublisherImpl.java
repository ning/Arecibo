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
