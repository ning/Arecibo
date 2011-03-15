package com.ning.arecibo.aggregator.plugin;

import java.util.Map;
import com.ning.arecibo.aggregator.dictionary.EventDefinition;
import com.ning.arecibo.lang.Aggregator;


public interface DynamicAggregatorPlugin
{
	Aggregator getDynamicAggregator(EventDefinition definition);
    EventDefinition preProcessEventDefinition(EventDefinition definition);
    void postProcessEvent(Map<String,Object> map);
    boolean isInterestedIn(EventDefinition definition);
}
