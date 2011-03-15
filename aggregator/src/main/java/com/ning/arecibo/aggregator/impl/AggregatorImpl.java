package com.ning.arecibo.aggregator.impl;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.EventBean;
import com.ning.arecibo.aggregator.dictionary.EventDefinition;
import com.ning.arecibo.aggregator.dictionary.EventDictionary;
import com.ning.arecibo.aggregator.plugin.DynamicAggregatorPlugin;
import com.ning.arecibo.aggregator.stringtemplates.HtmlTemplateGroupLoader;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.lang.AggregationOutputProcessor;
import com.ning.arecibo.lang.Aggregator;
import com.ning.arecibo.lang.InternalDispatcher;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.commons.lang.StringUtils;

import com.ning.arecibo.util.Logger;

public class AggregatorImpl implements UpdateListener
{
	private static final StringTemplateGroup templateGroup = HtmlTemplateGroupLoader.load(AggregatorImpl.class, "/agg.st");
	private static final Logger log = Logger.getLogger(AggregatorImpl.class);

	private final AggregatorImpl parent;
	private final Aggregator agg;
    private final DynamicAggregatorPlugin plugin;
    private final EventDictionary dictionary;
	private final AggregatorRegistry registry;
	private final List<AggregatorImpl> children = new ArrayList<AggregatorImpl>();
	private final List<AggregationOutputProcessorImpl> processors = new ArrayList<AggregationOutputProcessorImpl>();
    private final AtomicReference<EventDefinition> outputEventDefinition = new AtomicReference<EventDefinition>(null);

    public AggregatorImpl(AggregatorImpl parent,
                          Aggregator agg,
                          DynamicAggregatorPlugin plugin,
                          AggregatorRegistry registry, 
                          EventDictionary dictionary)
	{
		this.parent = parent;
		this.agg = agg;
        this.plugin = plugin;
		this.registry = registry;
        this.dictionary = dictionary;

		for ( AggregationOutputProcessor p : agg.getProcessors() ) {
			if ( p instanceof InternalDispatcher) {
				InternalDispatcher d = (InternalDispatcher) p ;
				for ( Aggregator a : d.getAggregators() ) {
					children.add(new AggregatorImpl(this, a, plugin, registry, dictionary));
				}
			}
		}
	}

	public void register()
	{
		log.info("registering aggregator %s", getPath());
		registry.addAggregatorPendingEventRegistration(agg.getInputEvent(), this);
		for ( AggregatorImpl impl : children ) {
			impl.register();
		}
	}

	public String getPath()
	{
		if (parent == null) {
			return agg.getFullName();
		}
		else {
			return parent.getPath() + "/" + agg.getName();
		}
	}

	public void renderDebugText(PrintWriter pw, int ident)
	{
		pw.printf("%s AggregatorImpl : %s\n", StringUtils.repeat(" ", ident), getPath());
		for ( AggregatorImpl impl : children ) {
			impl.renderDebugText(pw, ident+2);
		}
		pw.flush();
	}

	public void renderHtml(PrintWriter pw, int ident)
	{
		StringTemplate st = templateGroup.getInstanceOf("agg");
		st.setAttribute("agg", this);
		st.setAttribute("ident", StringUtils.repeat("==", ident) + (ident > 0 ? ">" : ""));
		pw.println(st);
		for ( AggregatorImpl impl : children ) {
			impl.renderHtml(pw, ident+2);
		}
		pw.flush();
	}

	public void notifyEventRegistered(EventDefinition def)
	{
        log.info("registering EPL statement for aggregator %s", getPath());
		EPStatement stmt = registry.registerEPL(getPath(), agg);
		if ( stmt != null ) {
			for ( AggregationOutputProcessor aggregationOutputProcessor : agg.getProcessors() ) {
				processors.add(registry.createProcessor(this, aggregationOutputProcessor));
			}
			stmt.addListener(this);
		}
	}

    @Override
	public void update(final EventBean[] newEvents, final EventBean[] oldEvents)
	{
        this.registry.getEsperStatsManager().incrementNumEventUpdatesEmittedByEsper();

		if ( agg.getOutputEvent() != null ) {

            if ( newEvents != null && newEvents.length > 0 ) {

                final List<MapEvent> newEventList = getValidatedEventList(newEvents);

                if(newEventList.size() == 0) {
                    return;
                }

                this.registry.getEsperStatsManager().increaseNumEventsEmittedByEsper(newEventList.size());

                if(outputEventDefinition.get() == null) {
                    // ok if race condition here
                    EventDefinition outDef = dictionary.getOutputEventDefinition(agg.getOutputEvent());
                    if(plugin != null && outDef != null) {
                        EventDefinition preProcessedDef = plugin.preProcessEventDefinition(outDef);
                        outputEventDefinition.set(preProcessedDef);
                    }
                }
                final EventDefinition outputDef = outputEventDefinition.get();

                if(log.isDebugEnabled()) {
                    log.debug("dispatching %d events like this %s", newEventList.size(), agg.getOutputEvent());
                    for(MapEvent newEvent:newEventList) {
                        log.debug("\tdispatching %s", newEvent);
                    }
                }

                for ( final AggregationOutputProcessorImpl processor : processors ) {
                    this.registry.getWorker().executeLater(new Runnable()
                    {
                        public void run()
                        {
                            processor.update(outputDef,newEventList);
                        }
                    });
                }
            }
		}
	}

    private List<MapEvent> getValidatedEventList(EventBean[] newEvents) {

        // convert to MapEvents
        // make sure we don't have any nulls, etc
        ArrayList<MapEvent> newEventList = new ArrayList<MapEvent>();

        for( EventBean newEvent :newEvents)
        {

            MapEvent mapEvent = MapEvent.fromEventBean(newEvent, agg.getOutputEvent());
            Map<String, Object> map = mapEvent.getMap();

            // remove null fields in the map
            Iterator<String> iter = map.keySet().iterator();
            while (iter.hasNext()) {
                if (map.get(iter.next()) == null) {
                    iter.remove();
                }
            }

            // allow plugins to do any post-processing
            if(plugin != null)
                plugin.postProcessEvent(map);

            // make sure there's something left
            if (map.size() == 0) {
                log.info("Ignoring newEvent with no property type info, for eventType %s", agg.getOutputEvent());
                continue;
            }

            newEventList.add(mapEvent);
        }

        return newEventList;
    }

	public boolean hasChildren()
	{
		return children.size() > 0 ;
	}

	public Aggregator getAggregator()
	{
		return agg;
	}

    public void start()
    {
        log.info("starting aggregator %s", getPath());
        EPStatement stmt = registry.getEPL(this);
        if (stmt != null) {
            stmt.start();
        }
        for ( AggregatorImpl impl : children ) {
            impl.start();
        }
    }

    public void stop()
    {
        log.info("stopping aggregator %s", getPath());
        EPStatement stmt = registry.getEPL(this);
        if (stmt != null) {
            stmt.stop();
        }
        for ( AggregatorImpl impl : children ) {
            impl.stop();
        }
    }

    public void destroy()
    {
        log.info("destroying aggregator %s", getPath());
        registry.removeAggregatorPendingEventRegistration(agg.getInputEvent());
        registry.unregisterEPL(this);
        for ( AggregatorImpl impl : children ) {
            impl.destroy();
        }
    }
}
