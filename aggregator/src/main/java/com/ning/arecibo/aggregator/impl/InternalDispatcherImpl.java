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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.ning.arecibo.aggregator.dictionary.EventDefinition;
import com.ning.arecibo.event.BatchedEvent;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.publisher.AreciboEventServiceChooser;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.eventlogger.EventPublisher;
import com.ning.arecibo.lang.ConstantDispatchRouter;
import com.ning.arecibo.lang.InternalDispatcher;
import com.ning.arecibo.util.LRUCache;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.UUIDUtil;

public class InternalDispatcherImpl extends AggregationOutputProcessorImpl
{
	private static final Logger log = Logger.getLogger(InternalDispatcherImpl.class);

    //TODO: inject this?
    private static int UUID_CACHE_SIZE = 1000;

	private final AggregatorImpl impl;
	private final InternalDispatcher internalDispatcher;
	private final EventPublisher publisher;
	private final AreciboEventServiceChooser chooser;
	private final EventProcessorImpl processor;
	private final LRUCache<String, UUID> cache = new LRUCache<String, UUID>(UUID_CACHE_SIZE);
	private final UUID self ;

    public InternalDispatcherImpl(AggregatorImpl impl,
                          InternalDispatcher internalDispatcher,
                          EventPublisher publisher,
                          AreciboEventServiceChooser chooser,
                          EventProcessorImpl processor,
                          UUID self)
	{
		super();
		this.impl = impl;
		this.internalDispatcher = internalDispatcher;
		this.publisher = publisher;
		this.chooser = chooser;
		this.processor = processor;
		this.self = self ;
	}

    public void update(EventDefinition def, List<MapEvent> newEvents)
	{
		if ( newEvents != null ) {
			if ( internalDispatcher.getRouter() instanceof ConstantDispatchRouter) {
				BatchedEvent batch = null;
				for ( MapEvent evt : newEvents ) {

                    if(def != null)
                        evt = copyAndAddFields(def,evt);

					if ( batch == null ) {
						batch = new BatchedEvent(evt);
					}
					else {
						batch.getEvents().add(evt);
					}
				}
				dispatchEvent(batch);
			}
			else {
				Map<UUID, BatchedEvent> batches = new HashMap<UUID, BatchedEvent>();
				for ( MapEvent evt : newEvents ) {

                    if(def != null)
					    evt = copyAndAddFields(def,evt);

					if (!batches.containsKey(evt.getSourceUUID())){
						batches.put(evt.getSourceUUID(), new BatchedEvent(evt));
					}
					else {
						batches.get(evt.getSourceUUID()).getEvents().add(evt);
					}
				}
				for ( BatchedEvent be : batches.values() ) {
					dispatchEvent(be);
				}
			}
		}
	}

	private MapEvent copyAndAddFields(EventDefinition def,MapEvent evt)
	{
        // need to make a copy, before modifying, since these mods are specific to this processor
        MapEvent copyEvt = new MapEvent(evt.toMap());
        copyEvt.getMap().put(EVENT_DEFINITION_KEY,def);
	    copyEvt.setUuid(mapKeyToUUID(internalDispatcher.getRouter().route(copyEvt)));

        return copyEvt;
	}

	private void dispatchEvent(Event event)
    {
        UUID serviceUUID = chooser.getServiceUUID(event.getSourceUUID());
        if ( serviceUUID == null || self.equals(serviceUUID)) {
            log.debug("InternalDispatcher : dispatching to local event processor");
            processor.processEvent(event);
        }
        else {
            try {
                log.debug("InternalDispatcher : sending off to network");
                publisher.publish(event, internalDispatcher.isReliable() ? EventPublisher.PublishMode.SYNCHRONOUS_CLUSTER : EventPublisher.PublishMode.ASYNCHRONOUS);
            }
            catch (IOException e) {
                log.warn(e);
            }
        }
    }

    private UUID mapKeyToUUID(String key)
	{
		UUID uuid = cache.get(key);
		if ( uuid == null ) {
			uuid = UUIDUtil.md5UUID(key);
			cache.put(key, uuid);
		}
		return uuid ;
	}
}
