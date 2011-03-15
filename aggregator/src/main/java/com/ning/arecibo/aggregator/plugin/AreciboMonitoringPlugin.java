package com.ning.arecibo.aggregator.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.StringTokenizer;

import com.google.inject.Inject;
import com.ning.arecibo.aggregator.dictionary.EventDefinition;
import com.ning.arecibo.aggregator.dictionary.EventDictionary;
import com.ning.arecibo.aggregator.impl.AggregatorRegistry;
import com.ning.arecibo.aggregator.impl.EventProcessorImpl;
import com.ning.arecibo.aggregator.listeners.EventPreProcessorListener;
import com.ning.arecibo.aggregator.listeners.EventRegistrationListener;
import com.ning.arecibo.aggregator.plugin.guice.BaseLevelBatchIntervalSeconds;
import com.ning.arecibo.aggregator.plugin.guice.BaseLevelTimeWindowSeconds;
import com.ning.arecibo.aggregator.plugin.guice.ReceiverServiceName;
import com.ning.arecibo.aggregator.plugin.guice.ReductionFactors;
import com.ning.arecibo.event.MonitoringEvent;
import com.ning.arecibo.lang.Aggregator;
import com.ning.arecibo.lang.AggregatorCallback;
import com.ning.arecibo.lang.ConstantDispatchRouter;
import com.ning.arecibo.lang.DispatcherCallback;
import com.ning.arecibo.lang.ExternalPublisher;
import com.ning.arecibo.lang.InternalDispatcher;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.TemplateGroupLoader;

public class AreciboMonitoringPlugin implements DynamicAggregatorPlugin, EventRegistrationListener, EventPreProcessorListener
{
	private static final Logger log = Logger.getLogger(AreciboMonitoringPlugin.class);

    // suffixes
	private final static String HOST_SUFFIX = "_host";
	private final static String PATH_SUFFIX = "_path";
	private final static String TYPE_SUFFIX = "_type";

    // fields to remove from base event defs
	private final static String MIN_PREFIX = "min_";
	private final static String MAX_PREFIX = "max_";
	private final static String DATAPOINTS = "datapoints";
    private final static String NUM_HOSTS = "numHosts";
    private final static String AGG_TYPE = "aggType";
    private final static String REDUCTION = "reduction";

    // required fields for base event defs
	private final static String[] BASE_REQUIRED_FIELDS = {"hostName","deployedConfigSubPath","deployedType","deployedEnv","deployedVersion"};
	private final static Class<String> BASE_REQUIRED_FIELD_CLASS = String.class;

	private final StringTemplateGroup templates = TemplateGroupLoader.load(AreciboMonitoringPlugin.class, "/sql.st");
	public static final String NS = "monitoring" ;
	private final String serviceName;
    private final int baseLevelBatchIntervalSeconds;
    private final int baseLevelTimeWindowSeconds;
    private final AggregatorRegistry registry;
    private final EventDictionary dictionary;
    private final EventProcessorImpl eventProcessor;
    private final int[] reductionFactors;

    private int numReductionLevels;
    private final String[] reductionLevelIntervalSeconds;
    private final String[] reductionLevelTimeWindowSeconds;
    private final String[] reductionLevelTags;
    private final List<ExternalPublisher> externalPublishers;

    @Inject
	public AreciboMonitoringPlugin(AggregatorRegistry registry,
								 EventDictionary dictionary,
								 EventProcessorImpl eventProcessor,
                                 @ReceiverServiceName String service,
                                 @BaseLevelBatchIntervalSeconds int baseLevelBatchIntervalSeconds,
                                 @BaseLevelTimeWindowSeconds int baseLevelTimeWindowSeconds,
                                 @ReductionFactors int[] reductionFactors)
	{
		this.serviceName = service;
        this.baseLevelBatchIntervalSeconds = baseLevelBatchIntervalSeconds;
        this.baseLevelTimeWindowSeconds = baseLevelTimeWindowSeconds;
        this.registry = registry;
        this.dictionary = dictionary;
        this.eventProcessor = eventProcessor;

        // note, ultimately, the collector should tell the aggregators which reduction factors to use, etc...
        this.reductionFactors = reductionFactors;
        this.numReductionLevels = reductionFactors.length;
        this.reductionLevelIntervalSeconds = new String[numReductionLevels];
        this.reductionLevelTimeWindowSeconds = new String[numReductionLevels];
        this.reductionLevelTags = new String[numReductionLevels];

        int level=0;
        for(int reductionFactor:this.reductionFactors) {
        	this.reductionLevelIntervalSeconds[level] = reductionFactor * this.baseLevelBatchIntervalSeconds + " sec";
        	if(level == 0) {
        		this.reductionLevelTimeWindowSeconds[level] = this.baseLevelTimeWindowSeconds + " sec";

        		// for backwards compatibility
        		this.reductionLevelTags[level] = "";
        	}
        	else {
        		this.reductionLevelTimeWindowSeconds[level] = ((int)(((double)reductionFactor + 0.5) * (double)this.baseLevelBatchIntervalSeconds)) + " sec";
        		this.reductionLevelTags[level] = "_" + reductionFactor + "X";
        	}
        	level++;
        }

        this.dictionary.addEventRegistrationListener(this);
        this.eventProcessor.addEventPreProcessorListener(this.getClass().getSimpleName(),this);
        this.registry.registerPlugin(this);

        this.externalPublishers = new ArrayList<ExternalPublisher>();
        StringTokenizer st = new StringTokenizer(service,",");
        while(st.hasMoreTokens()) {
            String serviceName = st.nextToken();
            externalPublishers.add(new ExternalPublisher(serviceName));
        }
	}

    @Override
	public Aggregator getDynamicAggregator(EventDefinition def)
	{
		if(isInterestedIn(def)) {

			final List<String> numerics = new ArrayList<String>();

			for (Map.Entry<String, Class> entry : def.getProperties().entrySet()) {
				Class clazz = entry.getValue();
				String name = entry.getKey();
				if (Number.class.isAssignableFrom(clazz) && !name.equals("timestamp")) {
					numerics.add(name);
				}
			}

            final String inputEvent = def.getEventType() ;

            Aggregator agg = new Aggregator(NS, inputEvent, inputEvent);
            agg.setStatement(generateEPL("host_query", inputEvent, numerics, reductionLevelTags[0], reductionLevelTimeWindowSeconds[0], reductionLevelIntervalSeconds[0]));
            agg.setOutputEvent(inputEvent + HOST_SUFFIX);
            agg.addDispatcher(new ConstantDispatchRouter(inputEvent), new DispatcherCallback()
                    {
                        public void configure(InternalDispatcher internalDispatcher)
                        {
                            AggregatorCallback hostStack = getNestedAggregatorStack(1,"host","host_query",HOST_SUFFIX,HOST_SUFFIX,inputEvent,numerics,externalPublishers);
                            if(hostStack != null) {
                                internalDispatcher.addAggregator("host", hostStack);
                            }

                            AggregatorCallback pathStack = getNestedAggregatorStack(0,"path","path_query",HOST_SUFFIX,PATH_SUFFIX,inputEvent,numerics,externalPublishers);
                            if(pathStack != null) {
                                internalDispatcher.addAggregator("host_path", pathStack);
                            }

                            AggregatorCallback typeStack = getNestedAggregatorStack(0,"type","type_query",HOST_SUFFIX,TYPE_SUFFIX,inputEvent,numerics,externalPublishers);
                            if(typeStack != null) {
                                internalDispatcher.addAggregator("host_type", typeStack);
                            }
                        }
                    },true);

            for(ExternalPublisher externalPublisher:externalPublishers) {
                agg.addOutputProcessor(externalPublisher);
            }

            return agg;
		}
		return null;
	}

	private AggregatorCallback getNestedAggregatorStack(final int depth,final String name,final String queryName,final String inSuffix,final String outSuffix,
														final String eventName,final List<String> numerics,final List<ExternalPublisher> externalPublishers) {

		if(depth >= numReductionLevels)
			// shouldn't happen
			return null;

		final String timeWindowSecs = reductionLevelTimeWindowSeconds[depth];
		final String batchIntervalSecs = reductionLevelIntervalSeconds[depth];

		final String inputEventName;
		final String queryExt;

		if(depth == 0) {
			inputEventName = eventName + inSuffix;
			queryExt = "";
		}
		else {
			inputEventName = eventName + inSuffix + reductionLevelTags[depth-1];
			queryExt = "_X";
		}

		final String outputEventName = eventName + outSuffix + reductionLevelTags[depth];

		if(depth == numReductionLevels-1) {
			return new AggregatorCallback() {
				public void configure(Aggregator agg)
				{
					agg.setStatement(generateEPL(queryName + queryExt, inputEventName, numerics, reductionLevelTags[depth],timeWindowSecs, batchIntervalSecs));
					agg.setOutputEvent(outputEventName);

                    for(ExternalPublisher externalPublisher:externalPublishers) {
                        agg.addOutputProcessor(externalPublisher);
                    }
				}
			};
		}
		else {
			return new AggregatorCallback() {
				public void configure(Aggregator agg)
				{
					agg.setStatement(generateEPL(queryName + queryExt, inputEventName, numerics, reductionLevelTags[depth],timeWindowSecs, batchIntervalSecs));
					agg.setOutputEvent(outputEventName);
					agg.addDispatcher(new ConstantDispatchRouter(eventName), new DispatcherCallback()
					{
						public void configure(InternalDispatcher internalDispatcher)
						{
							internalDispatcher.addAggregator(name + reductionFactors[depth],getNestedAggregatorStack(depth+1,name,queryName,outSuffix,outSuffix,eventName,numerics,externalPublishers));
						}
					},true);

                    for(ExternalPublisher externalPublisher:externalPublishers) {
                        agg.addOutputProcessor(externalPublisher);
                    }
				}
			};
		}
	}

    @Override
	public boolean isInterestedIn(EventDefinition def)
	{
		return (def.isGeneratedClass() && MonitoringEvent.class.isAssignableFrom(def.getSourceEventClass()));
	}

	// Callback called on each event registration
    @Override
	public void eventRegistered(EventDefinition def) {
		if(isEventTypeHigherLevelType(def.getEventType())) {
			EventDefinition baseDef = getBaseEventDefinition(def);
			this.dictionary.registerDefinition(baseDef);
		}
	}

    @Override
    public void eventUnRegistered(EventDefinition def) {

        if (isEventTypeHigherLevelType(def.getEventType())) {
            def = getBaseEventDefinition(def);
        }

        try {
            registry.unregister(NS + "/" + def.getEventType());
        }
        catch(Exception ex) {
            log.warn(ex,"Failed to unregister eventType " + def.getEventType());
        }
    }

    // Callback called on each event received, via EventPreProcessorListener api
	// Convert types to Double, as they will be aggregated to such anyway,
	// this prevents class cast problems later when higher level events are registered first, etc.
    // Also cleanup usage of the "Count" attribute, which no longer works staring in Esper 3.
    // TODO: This should be invoked on a per-namespace basis, so this plugin pre-processing doesn't clash with another plugin
    @Override
	public void preProcessEvent(Map<String,Object> map) {

		for ( String name : map.keySet() ) {

			Object obj = map.get(name);

			// prevent type confusion later on...use only doubles for numbers
			// (except for meta-data like timestamp)
			if(!name.equalsIgnoreCase("timestamp") && (obj instanceof Number)) {
				if(!(obj instanceof Double)) {
					obj = ((Number)obj).doubleValue();
					map.put(name,obj);
				}
			}
        }

        // add name of base event type, for use by the keyed executor (full stack should use the same executor key)
        String eventType = (String)map.get("eventType");
        if(isEventTypeHigherLevelType(eventType)) {
            String baseEventType = eventType.substring(0,eventType.lastIndexOf("_"));
            map.put("baseEventType",baseEventType);
        }
        else {
            map.put("baseEventType",eventType);
        }
    }

    // Callback called when an AggregatorImpl emits a new higher-level event for the first time, we want to register the
    // appropriate input event type, that incorporates all known variants (and doesn't cause an esper error due to missing
    // fields in the actual event received)...
    @Override
    public EventDefinition preProcessEventDefinition(EventDefinition def) {
        if(def == null) {
            return null;
        }

        String eventType = def.getEventType();
        EventDefinition newDef = new EventDefinition(def.getSourceEventClass(),eventType,def.getEvtClass(),def.getProperties());

        for(String name:newDef.getProperties().keySet()) {

            Class clazz = newDef.getProperties().get(name);

            // prevent type confusion later on...use only doubles for numbers
            // (except for meta-data like timestamp)
            if(!name.equalsIgnoreCase("timestamp") && (Number.class.isAssignableFrom(clazz))) {
                if(!(clazz.equals(Double.class))) {
                    clazz = Double.class;
                    newDef.getProperties().put(name,clazz);
                }
            }
        }

        // make sure the 'eventType' is in the definition (since it gets auto-generated in the map for incoming events)
        newDef.getProperties().put("eventType",String.class);

        // make sure the 'sourceUUID' is in the definition (since it gets auto-generated in the map for incoming events)
        newDef.getProperties().put("sourceUUID", UUID.class);

        // add the 'numHosts' for all above the first level host aggregation
        if(eventType.endsWith(PATH_SUFFIX) || eventType.endsWith(TYPE_SUFFIX)){

            newDef.getProperties().put("numHosts",Double.class);
        }

        return newDef;
    }


    @Override
    public void postProcessEvent(Map<String,Object> map) {

        // see if we have any residual precision errors, due to Esper's buggy avg() function
        // (see http://jira.codehaus.org/browse/ESPER-356)
        //
        // find keys that start with max prefix, and then make sure avg value is between max/min
        for(String maxKey:map.keySet()) {
            if(maxKey.startsWith(MAX_PREFIX)) {
                try {

                    String key = maxKey.substring(MAX_PREFIX.length());
                    Double val = (Double)map.get(key);
                    Double maxVal = (Double)map.get(maxKey);
                    if(val > maxVal) {
                        map.put(key,maxVal);
                    }
                    else {
                        String minKey = MIN_PREFIX + key;
                        Double minVal = (Double)map.get(minKey);

                        if(val < minVal)
                            map.put(key,minVal);
                    }
                }
                catch(RuntimeException ruEx) {
                    log.warn(ruEx,"RuntimeException:");
                }
            }
        }
    }

	private boolean isEventTypeHigherLevelType(String eventType) {

		if(eventType.endsWith(HOST_SUFFIX) ||
				eventType.endsWith(PATH_SUFFIX) ||
				eventType.endsWith(TYPE_SUFFIX)) {
			return true;
		}
		else {
			return false;
		}
	}

	private EventDefinition getBaseEventDefinition(EventDefinition def) {

		// create new event alias with suffix stripped off
		String baseEventType = def.getEventType();
		if(baseEventType.endsWith(HOST_SUFFIX)) {
			baseEventType = baseEventType.substring(0,baseEventType.lastIndexOf(HOST_SUFFIX));
		}
		else if(baseEventType.endsWith(PATH_SUFFIX)) {
			baseEventType = baseEventType.substring(0,baseEventType.lastIndexOf(PATH_SUFFIX));
		}
		else if(baseEventType.endsWith(TYPE_SUFFIX)) {
			baseEventType = baseEventType.substring(0,baseEventType.lastIndexOf(TYPE_SUFFIX));
		}

		// create new map without higherlevel attributes, such as min/max events, numHosts, etc.
		SortedMap<String,Class> currMap = def.getProperties();
		SortedMap<String,Class> baseMap = new TreeMap<String,Class>();
		for(String key:currMap.keySet()) {
			if(key.startsWith(MIN_PREFIX) ||
                    key.startsWith(MAX_PREFIX) ||
                    key.equals(DATAPOINTS) ||
                    key.equals(NUM_HOSTS) ||
                    key.equals(AGG_TYPE) ||
                    key.equals(REDUCTION)) {

				continue;
			}
			baseMap.put(key,currMap.get(key));
		}

		// add in base keys that might not be present in higher level
		for(String baseKey: BASE_REQUIRED_FIELDS) {
			if(baseMap.get(baseKey) == null) {
				baseMap.put(baseKey, BASE_REQUIRED_FIELD_CLASS);
			}
		}

		EventDefinition baseDef = new EventDefinition(MonitoringEvent.class,
														baseEventType,
														def.getEvtClass(),
														baseMap);

		return baseDef;
	}

	private String generateEPL(String name, String eventAlias, List<String> numerics, String reductionTag, String win, String every)
	{
		StringTemplate st = templates.getInstanceOf(name);
		st.setAttribute("event", eventAlias);
		st.setAttribute("fields", numerics);
		st.setAttribute("reduction",reductionTag);
        st.setAttribute("win", win);
        st.setAttribute("every", every);
		return st.toString();
	}
}
