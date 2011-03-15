package com.ning.arecibo.aggregator.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.weakref.jmx.Managed;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPRuntime;
import com.google.inject.Inject;
import com.ning.arecibo.aggregator.concurrent.KeyedExecutor;
import com.ning.arecibo.aggregator.dictionary.EventDefinition;
import com.ning.arecibo.aggregator.dictionary.EventDictionary;
import com.ning.arecibo.aggregator.esper.EsperProvider;
import com.ning.arecibo.aggregator.esper.EsperStatsManager;
import com.ning.arecibo.aggregator.guice.AggregatorNamespaces;
import com.ning.arecibo.aggregator.listeners.EventPreProcessorListener;
import com.ning.arecibo.aggregator.listeners.EventProcessorListener;
import com.ning.arecibo.event.BatchedEvent;
import com.ning.arecibo.event.TransformableEvent;
import com.ning.arecibo.event.receiver.EventProcessor;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.esper.MiniEsperEngine;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;

public class EventProcessorImpl implements EventProcessor
{
	private static final Logger log = Logger.getLogger(EventProcessorImpl.class);

	private final EventDictionary dictionary;
	private final String[] namespaces;

	private final ConcurrentHashMap<String, EventPreProcessorListener> preListeners = new ConcurrentHashMap<String, EventPreProcessorListener>();
	private final ConcurrentHashMap<String, List<EventProcessorListener>> listeners = new ConcurrentHashMap<String, List<EventProcessorListener>>();
    private final EsperStatsManager esperStatsManager;
	private final KeyedExecutor executor;
	private final MiniEsperEngine<Long> stats;
	private final AtomicBoolean isSuspending = new AtomicBoolean(false);

	@Inject
	public EventProcessorImpl(EventDictionary dictionary,
                                KeyedExecutor exe,
                                EsperStatsManager esperStatsManager,
                                @AggregatorNamespaces String[] namespaces)
	{
		this.dictionary = dictionary;
		this.namespaces = namespaces;
		this.executor = exe;
        this.esperStatsManager = esperStatsManager;
		this.stats = new MiniEsperEngine<Long>(EventProcessorImpl.class.getName(), Long.class);

        // add the main event dictionary as a pre-processor
        addEventPreProcessorListener("EventDictionary",dictionary);
	}

    @Override
    public void processEvent(final Event evt)
    {
	    if ( !isSuspending.get() ) {
			if (evt instanceof BatchedEvent) {
				for ( Event e : ((BatchedEvent)evt).getEvents() ) {
					_processEvent(e);
				}
			}
			else {
				_processEvent(evt);
			}
	    }
	    else {
		    log.info("Aggregator suspended, discarding event...");
	    }
    }

    public void _processEvent(final Event evt)
	{
		final long startT = System.currentTimeMillis() ;

		if (evt instanceof TransformableEvent) {
			TransformableEvent e = (TransformableEvent) evt;

            try {
                final Map<String, Object> map = e.toMap();

                // pull out any stored eventDef
                final EventDefinition eventDef = (EventDefinition)map.remove(AggregationOutputProcessorImpl.EVENT_DEFINITION_KEY);

                // debug dump the full event
                if (log.isDebugEnabled()) {
                    Set<String> keys = e.getKeys();
                    log.debug("Event type: %s, Event map size: %s, sourceUUID: %s", e.getEventType(),keys.size(),evt.getSourceUUID());
                    /*
                    for (String key : keys) {
                        Object obj = e.getObject(key);
                        if(obj != null)
                            log.debug("  %s: %s (%s)", key, obj, obj.getClass().getSimpleName());
                        else
                            log.debug("  %s: %s", key, obj);
                    }
                    */
                }

                // do any pre-filtering on the event map
				for(EventPreProcessorListener listener:preListeners.values()) {
					listener.preProcessEvent(map);
				}

                // this may get set by a preProcessor, pull it out and save it
                // TODO: Clean this up, make a constant, dictionary reserved property, etc.
                String baseEventType = (String)map.remove("baseEventType");
                if(baseEventType == null)
                    baseEventType = evt.getEventType();
                

                // use baseEventType for keyed Executor, for thread-safety within the full aggregator stack
				executor.execute(baseEventType, new Runnable()
				{
					public void run()
					{
						try {

                            String registeredEventType;
                            if(eventDef != null)
                                registeredEventType = dictionary.registerDefinition(eventDef);
                            else
							    registeredEventType = dictionary.registerEvent(evt, map);
                            
							try {
								List<EventProcessorListener> list = listeners.get(registeredEventType);
								if (list != null) {
									for (EventProcessorListener p : list) {
										p.processEvent(map);
									}
								}
							}
							catch (Exception e1) {
								log.error(e1);
							}

							for (String ns : namespaces) {
								EPRuntime runtime = EsperProvider.getProvider(ns).getEPRuntime();
								try {
									runtime.sendEvent(map, registeredEventType);
                                    esperStatsManager.incrementNumEventsSentToEsper();
								}
								catch (Exception ex) {
									log.error(ex);
								}
							}
						}
						catch (Exception e1) {
							log.error(e1);
						}
						finally {
							stats.send(System.currentTimeMillis() - startT) ;
						}

					}
				});
			}
			catch (Exception ex) {
				log.warn(ex);
			}
		}
		else {
			executor.execute(evt.getEventType(), new Runnable()
			{
				public void run()
				{
					try {
                        dictionary.registerEventClass(evt);
						for (String ns : namespaces) {
							EPRuntime runtime = EsperProvider.getProvider(ns).getEPRuntime();
							runtime.sendEvent(evt);
                            esperStatsManager.incrementNumEventsSentToEsper();
						}
					}
					catch (EPException e) {
						log.error(e);
					}
					finally {
						stats.send(System.currentTimeMillis() - startT) ;
					}
				}
			});
		}
	}

	public void addEventPreProcessorListener(String preProcessorName,EventPreProcessorListener p)
	{
		preListeners.put( preProcessorName,p);
	}

	public void addEventProcessorListener(String inputEvent, EventProcessorListener p)
	{
		if (!listeners.containsKey(inputEvent)) {
			listeners.putIfAbsent(inputEvent, new ArrayList<EventProcessorListener>());
		}
		listeners.get(inputEvent).add(p);
	}

	public void removeEventProcessorListener(String inputEvent, EventProcessorListener p)
	{
		List<EventProcessorListener> list = listeners.get(inputEvent);
		if (list != null) {
			list.remove(p);
		}
	}

    @Managed
	public void suspend()
	{
		isSuspending.set(true);
	}

    @Managed
	public void resume()
	{
		isSuspending.set(false);
	}

    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE })
    public long getEventsProcessed()
    {
        return stats.getCount();
    }

    @MonitorableManaged(monitored = true)
    public long getMaxEventProcessTime()
    {
        return stats.getMax().longValue() ;
    }

    @MonitorableManaged(monitored = true)
    public long getMinEventProcessTime()
    {
        return stats.getMin().longValue() ;
    }

    @MonitorableManaged(monitored = true)
    public long getAverageEventProcessTime()
    {
        return (long) stats.getAverage();
    }
}
